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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.scala.util.Timer
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.authoring.blob.BlobService
import loi.authoring.edge.EdgeService
import loi.authoring.index.SearchPath
import loi.authoring.index.web.DcmPathUtils.*
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.AttachedReadWorkspace
import net.htmlparser.jericho.{Element, Source, StartTag, TextExtractor}

import scala.jdk.CollectionConverters.*

@Service
private[html] class LinkSearchService(implicit
  blobService: BlobService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  mimeWebService: MimeWebService,
):
  import LinkSearchService.*
  import loi.authoring.index.IndexServiceImpl.inOrderSubtree

  /** Extract all the hyperlinks from HTML content in the specified `branch` of the specified `project`, applying a
    * supplied function to each. Stops searching when the specified `timer` expires.
    *
    * This does not consider image or video sources, nor does it drill into S3 stored content.
    */
  def hyperlinkSearch[A](workspace: AttachedReadWorkspace, timer: Timer)(accept: Link => A): Iterable[A] =

    // There is a plausible issue that if Content Item A has some form of
    // remediation or other link to Content Item B, and A comes first in
    // the traversal, then B will be considered a descendant of A, but
    // let's just ignore for now.
    val homeNode = workspace.requireNodeId(workspace.homeName).get
    val assetIds = inOrderSubtree(workspace, homeNode)

    for
      asset <- nodeService.load(workspace).byId(assetIds)
      if !asset.info.archived && !timer.expired
      path   = searchPath(workspace, asset.info.name)
      html  <- asset.htmls
      link  <- getLinks(html, path)
    yield accept(link)
  end hyperlinkSearch
end LinkSearchService

private[html] object LinkSearchService:
  def getLinks(html: String, path: SearchPath): List[Link] =
    val source     = new Source(html)
    val elements   = source.getAllElements().asScala
    val hyperLinks = for
      element <- elements
      if element.getName == "a"
      href    <- Option(element.getAttributeValue("href"))
      if href.startsWith("http") || LoLinkRe.matches(href)
    yield Link(href, element.getTextExtractor.toString, path)
    // some content is full of links stored as text that are converted to <a> tags by jshittery
    val textLinks  = for
      element <- elements
      if element.getName != "a"
      href    <- extractTextHyperlink(element)
    yield Link(href, href, path)
    hyperLinks.toList ::: textLinks.toList
  end getLinks

  // This only matches if the entire content looks like one link, which seems
  // to be the usual case. I can't capture the correct link text because
  // that is typically stored in some sibling element. For example:
  // <ul><li>#magic keyword</li><li>Link Text</li><li>http://example.org/</li></ul>
  def extractTextHyperlink(element: Element): Option[String] =
    val text = new DirectTextExtractor(element).toString
    HyperlinkRE.unapplySeq(text).map(_.head)

  private val HyperlinkRE = """(?i)^\s*(https?://\S+)\s*$""".r

  val LoLinkRe = """(?i)^javascript:lonav\(['"]([^'"]*)['"].*\)$""".r
end LinkSearchService

/** Only extracts direct text content of this element. */
private[html] class DirectTextExtractor(element: Element) extends TextExtractor(element.getContent):
  override def excludeElement(startTag: StartTag): Boolean = true

private[html] final case class Link(href: String, text: String, path: SearchPath)
