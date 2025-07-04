/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package loi.authoring.html

import com.learningobjects.cpxp.async.async.AsyncOperationActor
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.scala.util.Timer
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.authoring.asset.Asset
import loi.authoring.blob.BlobService
import loi.authoring.edge.{EdgeService, Group}
import loi.authoring.html.LinkSearchService.{LoLinkRe, getLinks}
import loi.authoring.index.IndexServiceImpl.inOrderSubtree
import loi.authoring.index.web.DcmPathUtils.searchPath
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{HttpGet, HttpHead, HttpUriRequest}
import org.apache.http.util.EntityUtils
import scalaz.syntax.std.option.*
import scaloi.syntax.any.*

import java.net.URI
import java.util.UUID
import scala.collection.mutable
import scala.util.Try
import scala.util.control.NonFatal

@Service
private[html] class LinkCheckServiceImpl(httpClient: HttpClient)(implicit
  blobService: BlobService,
  mimeWebService: MimeWebService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
) extends LinkCheckService:

  import LinkCheckServiceImpl.*

  /** Checks all the external hyperlinks from HTML content in the specified `commit` of the specified `project` for
    * validity by issuing a HEAD or GET request and recording the status and any redirect. Stops searching when the
    * specified `timer` expires.
    *
    * This does not consider image or video sources, nor does it drill into S3 stored content.
    */
  override def hyperlinkCheck[A](workspace: AttachedReadWorkspace, timer: Timer)(
    accept: (Link, LinkStatus) => A
  ): Iterable[A] =
    val statusCache = mutable.Map.empty[String, LinkStatus]
    val hostDelay   = mutable.Map.empty[String, Long]

    // There is a plausible issue that if Content Item A has some form of
    // remediation or other link to Content Item B, and A comes first in
    // the traversal, then B will be considered a descendant of A, but
    // let's just ignore for now. But what if Quiz 1 gates Module 2...
    // This will result in false search paths.
    val homeNode = workspace.requireNodeId(workspace.homeName).get
    val assetIds = inOrderSubtree(workspace, homeNode, _.traverse)
    val assets   = nodeService.load(workspace).byId(assetIds).filterNot(_.info.archived)

    AsyncOperationActor.withTodo(assets.length) { progress =>
      for
        asset      <- assets
        if !timer.expired
        _           = progress.increment()
        path        = searchPath(workspace, asset.info.name)
        html       <- asset.htmls
        link       <- getLinks(html, path)
        (url, hash) = hrefSplit(link.href)
        status      = statusCache.getOrElseUpdate(
                        url,
                        LoLinkRe
                          .findFirstMatchIn(url)
                          .cata(m => checkContentLink(m.group(1), asset, workspace), checkStatus(url, hostDelay))
                      )
      yield accept(link, status.copy(redirect = status.redirect.map(_ + hash)))
    }
  end hyperlinkCheck

  private def checkContentLink(edgeStr: String, asset: Asset[?], workspace: ReadWorkspace): LinkStatus =
    try
      val edgeId = UUID.fromString(edgeStr)
      val found  =
        workspace.outEdgeInfos(asset.info.name, Group.Hyperlinks).exists(_.edgeId == edgeId)
      if found then LinkStatus(200, "OK", None)
      else LinkStatus(404, "Content link not found", None)
    catch
      case _: IllegalArgumentException =>
        LinkStatus(500, "Invalid content link", None)

  private def checkStatus(url: String, hostDelay: mutable.Map[String, Long]): LinkStatus =
    logger.info(s"Checking link $url")
    try
      // browsers accept gross URLs with space, but not java
      val uri        = new URI(url.replace(" ", "%20"))
      val headStatus = fetchStatus(new HttpHead(uri), hostDelay)
      // Some URLs (in particular query string searches) fail on a HEAD but work on a GET, so fallback
      // to trying a GET. The failure is not necessarily a 4xx, often it is a 3xx to an error page.
      if headStatus.isOkay(url) then headStatus else fetchStatus(new HttpGet(uri), hostDelay)
    catch
      case NonFatal(error) =>
        logger.info(error)("Link status error")
        LinkStatus(-1, error.getMessage, None)
    end try
  end checkStatus

  private def fetchStatus(request: HttpUriRequest, hostDelay: mutable.Map[String, Long]): LinkStatus =
    val host = request.getURI.getHost
    // Sleep 100ms between requests to the same host
    hostDelay.get(host) foreach { ts => Thread.sleep((HostDelayMs + ts - System.currentTimeMillis) max 0) }
    val resp = httpClient.execute(request)
    logger.info(s"${request.getMethod} status: ${resp.getStatusLine}")
    EntityUtils.consumeQuietly(resp.getEntity)
    hostDelay.put(host, System.currentTimeMillis)
    LinkStatus(
      resp.getStatusLine.getStatusCode,
      resp.getStatusLine.getReasonPhrase,
      resp.getHeaders("Location").headOption.map(_.getValue).map(cleanRedirect(_, request.getURI))
    )
  end fetchStatus

  // redirect to SSL often sadly includes port and is sometimes relative.
  private def cleanRedirect(location: String, uri: URI): String =
    Try(uri.resolve(location.replace(":443", "")).toString).getOrElse(location)
end LinkCheckServiceImpl

private[html] object LinkCheckServiceImpl:
  private val logger = org.log4s.getLogger

  private val HostDelayMs = 100L

  // splits an href into the URL before any # and the # part, or ""
  private def hrefSplit(href: String): (String, String) =
    href.splitAt(hashIndex(href))

  private def hashIndex(href: String): Int = href.indexOf('#') ∂|> { case -1 => href.length }
end LinkCheckServiceImpl
