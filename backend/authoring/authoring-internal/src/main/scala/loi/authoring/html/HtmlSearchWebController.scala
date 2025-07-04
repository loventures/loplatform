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

import argonaut.CodecJson
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueryResults
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.Timer
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import loi.asset.html.model.Html
import loi.authoring.blob.BlobService
import loi.authoring.edge.EdgeService
import loi.authoring.index.web.DcmPathUtils.*
import loi.authoring.index.web.{SearchRow, SearchWebHit}
import loi.authoring.index.{BlobExtractor, TextExtractor}
import loi.authoring.node.AssetNodeService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scaloi.json.ArgoExtras
import scaloi.syntax.option.*

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[html] class HtmlSearchWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
)(implicit
  blobService: BlobService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  urlService: CurrentUrlService,
) extends ApiRootComponent
    with ComponentImplementation:

  import HtmlSearchWebController.*
  import loi.authoring.index.IndexServiceImpl.inOrderSubtree

  @RequestMapping(path = "authoring/search/{branch}/html", method = Method.GET)
  def search(
    @PathVariable("branch") branchId: Long,
    @QueryParam(value = "query") jsonQuery: ArgoBody[HtmlSearchWebQuery]
  ): ApiQueryResults[SearchWebHit] =
    val timer     = new Timer(TimeLimit)
    val query     = jsonQuery.decode_!.get
    val workspace = webUtils.workspaceOrThrow404(branchId)

    val usedAssetIds = inOrderSubtree(workspace, workspace.requireNodeId(workspace.homeName).get)
    val assetIds     =
      if query.unused.isTrue then mergeAssetIds(usedAssetIds, workspace.nodeIds)
      else usedAssetIds

    val archived = query.archived.isTrue
    val htmls    = nodeService
      .load(workspace)
      .byId(assetIds)
      .filterNot(_.info.archived && !archived)
      .flatMap(_.filter[Html])

    // TODO: I could parallelize the blob retrieval and search, but...
    val lcQuery = query.query.toLowerCase
    val results =
      for
        html   <- htmls
        if lcQuery.nonEmpty && !timer.expired
        source <- html.data.source
        text    = BlobExtractor.extractText(TextExtractor, source)
        if text.toLowerCase.contains(lcQuery)
      yield SearchWebHit(
        project = workspace.projectInfo.id,
        projectName = workspace.projectInfo.name,
        projectCode = workspace.projectInfo.code,
        projectType = workspace.projectInfo.productType,
        projectStatus = workspace.projectInfo.liveVersion,
        branch = workspace.bronchId,
        branchName = workspace.branch.name,
        course = workspace.homeName,
        path = searchPath(workspace, html.info.name).path,
        highlights = Map(query.query -> highlights(text, query.query).take(5)),
        archived = html.info.archived,
        asset = None,
      )

    new ApiQueryResults(results.asJava, results.size.toLong, results.size.toLong, timer.didExpire)
  end search

  @RequestMapping(path = "authoring/search/{branch}/html/results", method = Method.GET)
  def results(
    @PathVariable("branch") branchId: Long,
    @QueryParam(value = "query") jsonQuery: ArgoBody[HtmlSearchWebQuery],
    request: WebRequest
  ): FileResponse[?] =
    val hits = search(branchId, jsonQuery)

    val out = ExportFile.create(s"results.csv", MediaType.CSV_UTF_8, request)

    out.file.writeCsvWithBom[SearchRow] { csv =>
      hits forEach { hit =>
        csv.write(SearchRow(hit))
      }
      if hits.isTruncated then csv.write(TruncatedSearchRow)
    }

    FileResponse(out.toFileInfo)
  end results

  private def mergeAssetIds(primary: List[Long], secondary: Iterable[Long]): List[Long] =
    val primarySet = primary.toSet
    primary ::: secondary.filterNot(primarySet.contains).toList.sorted
end HtmlSearchWebController

object HtmlSearchWebController:
  private final val TimeLimit = 45.seconds // 60 second web request timeout

  private val TruncatedSearchRow = SearchRow(
    project = "** Search Timed Out, Results Truncated **",
    code = None,
    `type` = None,
    status = None,
    module = None,
    lesson = None,
    title = None,
    url = "",
    context = ""
  )

  private def highlights(text: String, query: String): List[String] =
    s"(?i)(.{0,50})(${Regex.quote(query)})(.{0,50})".r
      .findAllMatchIn(text)
      .map(m => s"${m.group(1)}{{{${m.group(2)}}}}${m.group(3)}")
      .toList
end HtmlSearchWebController

final case class HtmlSearchWebQuery(
  query: String,
  archived: Option[Boolean] = None,
  unused: Option[Boolean] = None
)

object HtmlSearchWebQuery:
  implicit val codec: CodecJson[HtmlSearchWebQuery] =
    CodecJson.casecodec3(HtmlSearchWebQuery.apply, ArgoExtras.unapply)(
      "query",
      "archived",
      "unused"
    )
