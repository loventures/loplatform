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

package loi.authoring.index.web

import argonaut.Argonaut.*
import argonaut.CodecJson
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueryResults
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.util.EntityContext
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import kantan.codecs.Encoder
import kantan.csv.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.EdgeService
import loi.authoring.index.{EsHit, EsQuery, EsService, SearchPathElement}
import loi.authoring.node.AssetNodeService
import loi.authoring.project.AccessRestriction
import loi.authoring.security.right.{AccessAuthoringAppRight, ViewAllProjectsRight}
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.WorkspaceService
import scalaz.std.anyVal.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras
import scaloi.misc.Monoids.*
import scaloi.syntax.boolean.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

@Component
@Controller(root = true)
private[web] class SearchWebController(
  val componentInstance: ComponentInstance,
  esService: EsService,
  workspaceService: WorkspaceService,
  webUtils: AuthoringWebUtils,
)(implicit
  assetNodeService: AssetNodeService,
  edgeService: EdgeService,
  urlService: CurrentUrlService,
) extends ApiRootComponent
    with ComponentImplementation:
  import DcmPathUtils.*
  import SearchWebController.*

  @Secured(Array(classOf[ViewAllProjectsRight]))
  @RequestMapping(path = "authoring/search", method = Method.GET)
  def search(
    @QueryParam(value = "query") jsonQuery: ArgoBody[SearchWebQuery]
  ): ApiQueryResults[SearchWebHit] =
    doSearch(esQuery(jsonQuery.decode_!.get), withAsset = false)

  @Secured(Array(classOf[ViewAllProjectsRight]))
  @RequestMapping(path = "authoring/search/aggregates", method = Method.GET)
  def aggregates(
    @QueryParam(value = "query") jsonQuery: ArgoBody[SearchWebQuery]
  ): Map[Any, Long] =
    val query     = esQuery(jsonQuery.decode_!.get)
    val limited   = query.copy(offering = Some(0L), size = Some(0))
    val esResults = esService.search(limited)
    esResults.aggregates

  @Secured(Array(classOf[ViewAllProjectsRight]))
  @RequestMapping(path = "authoring/search/results", method = Method.GET)
  def results(
    @QueryParam(value = "query") jsonQuery: ArgoBody[SearchWebQuery],
    request: WebRequest
  ): FileResponse[?] =
    downloadResults(esQuery(jsonQuery.decode_!.get), request);

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/search/branch/{branch}", method = Method.GET)
  def branchSearch(
    @PathVariable(value = "branch") branch: Long,
    @QueryParam(value = "query") jsonQuery: ArgoBody[SearchWebQuery],
    @QueryParam(value = "asset", decodeAs = classOf[Boolean]) asset: Option[Boolean]
  ): ApiQueryResults[SearchWebHit] =
    doSearch(branchQuery(jsonQuery.decode_!.get, branch), asset.isTrue)

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/search/branch/{branch}/results", method = Method.GET)
  def branchResults(
    @PathVariable(value = "branch") branch: Long,
    @QueryParam(value = "query") jsonQuery: ArgoBody[SearchWebQuery],
    request: WebRequest
  ): FileResponse[?] =
    downloadResults(branchQuery(jsonQuery.decode_!.get, branch), request);

  private def doSearch(
    query: EsQuery,
    withAsset: Boolean
  ): ApiQueryResults[SearchWebHit] =
    val limited   = query.copy(offering = Some(0L), size = query.size `min` MaxPageSize.some)
    val esResults = esService.search(limited)
    new ApiQueryResults(loadHits(esResults.hits, withAsset).asJava, esResults.total, esResults.total)

  private def downloadResults(
    query: EsQuery,
    request: WebRequest
  ): FileResponse[?] =
    val out        = ExportFile.create("results.csv", MediaType.CSV_UTF_8, request)
    val maxResults = query.size `min` CsvMaxRows
    val stopwatch  = new Stopwatch

    // This could stream the results straight to the browser if it proves slow in production.
    out.file.writeCsvWithBom[SearchRow] { csv =>
      def loop(from: Int): Unit =
        val results = esService.search(query.copy(offering = Some(0L), from = from.some, size = MaxPageSize.some))
        loadHits(results.hits) foreach { hit =>
          csv.write(
            SearchRow(hit)
          )
        }
        EntityContext.flushAndClearCaches()
        val count   = from + results.hits.size
        if (count < maxResults) && (count < results.total) && stopwatch.elapsed < TimeLimit then loop(count)
      end loop
      loop(0)
    }

    FileResponse(out.toFileInfo)
  end downloadResults

  private def branchQuery(query: SearchWebQuery, branch: Long): EsQuery =
    webUtils.branchOrFakeBranchOrThrow404(branch)
    esQuery(query).copy(branch = branch.some, sortBy = List("modified" -> false, "name" -> true))

  private def esQuery(query: SearchWebQuery): EsQuery =
    EsQuery(
      search = query.query,
      fields = (query.fields | Nil).map(f => s"data.$f"),
      projectRetired = query.retiredProjects.isTrue.noption(false),
      branchArchived = query.archivedBranches.isTrue.noption(false),
      archived = query.archivedAssets.isTrue.noption(false),
      used = query.unusedAssets.isTrue.noption(true),
      typeIds = query.typeIds | StandardTypeIds.toList,
      sortBy = List("project" -> true, "branch" -> true, "_score" -> false, "name" -> true),
      from = query.offset,
      size = query.limit,
      aggregate = query.aggregate,
      project = query.project,
    )

  private def loadHits(hits: List[EsHit], asset: Boolean = false): List[SearchWebHit] =
    // Group results for efficient processing
    // If we really cared about being efficient, we would ask for more source fields to be returned in
    // the document and we could then use the search results alone to render most of our search results.
    val hitMap = hits
      .groupBy(_.project)
      .toList
      .flatMap { case (projectId, projectHits) =>
        projectHits.groupBy(_.branch).toList flatMap { case (bronchId, branchHits) =>
          val workspace = workspaceService.requireReadWorkspace(bronchId, AccessRestriction.none)

          val assets = asset ?? assetNodeService
            .load(workspace)
            .byId(branchHits.flatMap(hit => workspace.getNodeId(hit.name)))
            .groupUniqBy(_.info.name)

          branchHits map { hit =>
            bronchId -> hit.name -> SearchWebHit(
              project = workspace.projectInfo.id,
              projectName = workspace.projectInfo.name,
              projectCode = workspace.projectInfo.code,
              projectType = workspace.projectInfo.productType,
              projectStatus = workspace.projectInfo.liveVersion,
              branch = bronchId,
              branchName = workspace.projectInfo.name,
              course = workspace.homeName,
              path = searchPath(workspace, hit.name).path,
              highlights = hit.highlights.view.mapValues(_.toList).toMap,
              archived = hit.archived,
              asset = assets.get(hit.name)
            )
          }
        }
      }
      .toMap
    // Return results in original order
    hits.map(hit => hitMap(hit.branch -> hit.name))
  end loadHits
end SearchWebController

object SearchWebController:
  private final val MaxPageSize = 100
  private final val CsvMaxRows  = 5000
  private final val TimeLimit   = 45.seconds // 60 second web request timeout

  private final val StandardTypeIds =
    AssetTypeId.LessonElementTypes + AssetTypeId.Lesson + AssetTypeId.Module + AssetTypeId.Course + AssetTypeId.Survey1

final case class SearchWebQuery(
  query: Option[String],
  fields: Option[List[String]] = None,
  retiredProjects: Option[Boolean] = None,
  archivedBranches: Option[Boolean] = None,
  archivedAssets: Option[Boolean] = None,
  unusedAssets: Option[Boolean] = None,
  typeIds: Option[List[AssetTypeId]] = None,
  offset: Option[Int] = None,
  limit: Option[Int] = None,
  aggregate: Option[String] = None,
  project: Option[Long] = None,
)

object SearchWebQuery:
  implicit val codec: CodecJson[SearchWebQuery] = CodecJson.casecodec11(SearchWebQuery.apply, ArgoExtras.unapply)(
    "query",
    "fields",
    "retiredProjects",
    "archivedBranches",
    "archivedAssets",
    "unusedAssets",
    "typeIds",
    "offset",
    "limit",
    "aggregate",
    "project",
  )
end SearchWebQuery

final case class SearchWebHit(
  project: Long,
  projectName: String,
  projectCode: Option[String],
  projectType: Option[String],
  projectStatus: Option[String],
  branch: Long,
  branchName: String,
  course: UUID,
  path: List[SearchPathElement],
  highlights: Map[String, List[String]],
  archived: Boolean,
  asset: Option[Asset[?]]
)

object SearchWebHit:

  import loi.authoring.asset.JacksonAssetCodec.assetCodecJson

  implicit val codec: CodecJson[SearchWebHit] = CodecJson.casecodec12(SearchWebHit.apply, ArgoExtras.unapply)(
    "project",
    "projectName",
    "projectCode",
    "projectType",
    "projectStatus",
    "branch",
    "branchName",
    "course",
    "path",
    "highlights",
    "archived",
    "asset"
  )
end SearchWebHit

final case class SearchRow(
  project: String,
  code: Option[String],
  `type`: Option[String],
  status: Option[String],
  // branch: String,
  module: Option[String],
  lesson: Option[String],
  title: Option[String],
  url: String,
  // typeId: AssetTypeId,
  // uuid: UUID,
  context: String,
)

object SearchRow:
  def apply(hit: SearchWebHit)(implicit urlService: CurrentUrlService): SearchRow =
    val asset = hit.path.head
    SearchRow(
      project = hit.projectName,
      code = hit.projectCode,
      `type` = hit.projectType,
      status = hit.projectStatus,
      // branch = hit.branchName,
      module = hit.path.find(_.typeId == AssetTypeId.Module).flatMap(_.title),
      lesson = hit.path.find(_.typeId == AssetTypeId.Lesson).flatMap(_.title),
      title = asset.title,
      url = urlService.getUrl(asset.href),
      // typeId = asset.typeId,
      // uuid = asset.name,
      context = hitContext(hit),
    )
  end apply

  def hitContext(hit: SearchWebHit): String =
    hit.highlights.view
      .filterKeys(field => !IgnoreHighlights.contains(field))
      .values
      .flatten
      .take(MaxHighlights)
      .map(HitRE.replaceAllIn(_, "$1"))
      .mkString(" … ")

  // Fields to ignore in the highlight context. The front end also ignores subtitle and keywords
  // but that is because it displays them on the UI; in the CSV export only title is present.
  private final val IgnoreHighlights = Set("data.title") // "data.subtitle", "data.keywords"
  private final val MaxHighlights    = 5
  private final val HitRE            = s"""\\{\\{\\{(.*?)}}}""".r

  implicit val assetTypeIdCellEncoder: CellEncoder[AssetTypeId] = Encoder.from(_.entryName)

  implicit val searchRowHeaderEncoder: HeaderEncoder[SearchRow] = HeaderEncoder.caseEncoder(
    "Project",
    "Code",   // metadata
    "Type",   // metadata
    "Status", // metadata
    // "Branch",
    "Module",
    "Lesson",
    "Title",
    "URL",
    // "Type",
    // "UUID",
    "Context",
  )(ArgoExtras.unapply)
end SearchRow
