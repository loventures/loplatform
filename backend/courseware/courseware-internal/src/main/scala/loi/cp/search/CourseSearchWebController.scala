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

package loi.cp.search

import argonaut.CodecJson
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueryResults
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import kantan.csv.HeaderEncoder
import loi.asset.lesson.model.Lesson
import loi.asset.module.model.Module
import loi.asset.unit.model.Unit1
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.index.{EsQuery, EsResults, EsSearch}
import loi.cp.content.{ContentAccessService, CourseContentService}
import loi.cp.course.CourseAccessService
import loi.cp.customisation.Customisation
import loi.cp.reference.EdgePath
import scalaz.std.anyVal.*
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.annotation.unused
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.Try

@Component
@Controller(root = true)
@unused
private[search] class CourseSearchWebController(
  val componentInstance: ComponentInstance,
  cas: ContentAccessService,
  coas: CourseAccessService,
  ccs: CourseContentService,
  cus: CurrentUrlService,
  esSearch: EsSearch,
  user: UserDTO,
) extends ApiRootComponent
    with ComponentImplementation:

  import CourseSearchWebController.*

  @RequestMapping(path = "lwc/{context}/search", method = Method.GET)
  def search(
    @PathVariable("context") context: Long,
    @QueryParam(value = "query") jsonQuery: ArgoBody[CourseSearchWebQuery]
  ): Try[ApiQueryResults[CourseSearchWebHit]] =
    for
      (course, contents) <- cas.readContents(context, user, _ => true)
      allContents        <- ccs.getCourseContentsInternal(course, Customisation.empty)
      branchId           <- course.getBranchId.toScala <@~* new InvalidRequestException("Course has no branch")
      searchQuery        <- jsonQuery.decode_!
    yield
      val includeNames = contents.map(_.asset.info.name).toSet
      val allNames     = allContents.tree.flatten.map(_.asset.info.name)
      val excludeNames = allNames.filterNot(includeNames.contains)

      val query     = esQuery(searchQuery, branchId, course.getOfferingId, excludeNames, coas.hasInstructorAccess(course))
      val edgePaths = contents.groupMapUniq(_.asset.info.name)(_.edgePath)

      val (results, hits) = doSearch(query, edgePaths)

      new ApiQueryResults(hits.asJava, results.total, results.total)

  @RequestMapping(path = "lwc/{context}/search/results", method = Method.GET)
  def results(
    @PathVariable("context") context: Long,
    @QueryParam(value = "query") jsonQuery: ArgoBody[CourseSearchWebQuery],
    request: WebRequest,
  ): Try[WebResponse] =
    for
      course        <- cas.getCourseAsInstructor(context, user)
      (_, contents) <- cas.readContents(context, user, _ => true)
      allContents   <- ccs.getCourseContentsInternal(course, Customisation.empty)
      branchId      <- course.getBranchId.toScala <@~* new InvalidRequestException("Course has no branch")
      searchQuery   <- jsonQuery.decode_!
    yield
      val includeNames = contents.map(_.asset.info.name).toSet
      val allNames     = allContents.tree.flatten.map(_.asset.info.name)
      val excludeNames = allNames.filterNot(includeNames.contains)

      val query     = esQuery(searchQuery, branchId, course.getOfferingId, excludeNames, coas.hasInstructorAccess(course))
      val esResults = esSearch.search(query)
      val edgePaths = contents.groupMapUniq(_.asset.info.name)(_.edgePath)

      val out        =
        ExportFile.create(
          s"${course.getName} (${course.getGroupId}) - Search Results.csv",
          MediaType.CSV_UTF_8,
          request
        )
      val maxResults = CsvMaxRows
      val stopwatch  = new Stopwatch

      out.file.writeCsvWithBom[CourseSearchRow] { csv =>
        def loop(from: Int): Unit =
          val (results, hits) = doSearch(query.copy(from = from.some, size = CsvPageSize.some), edgePaths)

          for // worst case quadratic performance but shrug
            hit  <- hits
            path <- allContents.tree.findPath(content => content.edgePath == hit.edgePath)
          do
            val unit    = path.find(_.rootLabel.asset.is[Unit1])
            val module  = path.find(_.rootLabel.asset.is[Module])
            val lesson  = path.find(_.rootLabel.asset.is[Lesson])
            val content = path.head
            csv.write(
              CourseSearchRow(
                course = course.getName,
                section = course.getGroupId,
                unit = unit.map(_.rootLabel.title),
                module = module.map(_.rootLabel.title),
                lesson = lesson.map(_.rootLabel.title),
                title = content.rootLabel.title.some,
                url = cus.getUrl(s"${course.getUrl}/#/instructor/content/${hit.edgePath}").some,
                context = hitContext(hit).some,
              )
            )
          end for

          val count = from + results.hits.size
          if (count < maxResults) && (count < results.total) then
            if stopwatch.elapsed < TimeLimit then loop(count)
            else csv.write(TruncatedCourseSearchRow)
        end loop
        loop(0)
      }

      FileResponse(out.toFileInfo)

  private def doSearch(query: EsQuery, edgePaths: Map[UUID, EdgePath]): (EsResults, List[CourseSearchWebHit]) =
    val results = esSearch.search(query)
    // it is not expected that a result would be returned that is not
    // in the edgepath map but it is possible. Because we are doing a
    // blacklist ACL rather than a whitelist, this is important, because
    // it effectively applies a whitelist to the results so unexpected
    // things will not accidentally show up.
    val hits    = for
      hit      <- results.hits
      edgePath <- edgePaths.get(hit.name)
    yield CourseSearchWebHit(
      edgePath = edgePath,
      highlights = hit.highlights.view.mapValues(_.toList).toMap,
    )
    results -> hits
  end doSearch

  // Originally tried an include list of only those elements you can see but that
  // list proved too long for elastic search so instead reformulating as an exclude
  // query, presuming there is less you can't see than you can, which forces me to
  // add a type ids list.
  private def esQuery(
    query: CourseSearchWebQuery,
    branchId: Long,
    offering: Option[Long],
    names: List[UUID],
    instructor: Boolean
  ): EsQuery =
    EsQuery(
      search = query.query,
      fields = (if instructor then InstructorSearchFields else SearchFields).map(f => s"data.$f"),
      branch = branchId.some,
      offering = offering,
      sortBy = List("_score" -> false, "name" -> true),
      typeIds = StandardTypeIds.toList,
      excludeNames = names,
      archived = false.some,
      used = true.some,
      from = query.offset,
      size = query.limit `min` MaxHits.some,
    )
end CourseSearchWebController

private object CourseSearchWebController:
  private final val StandardTypeIds =
    AssetTypeId.LessonElementTypes + AssetTypeId.Lesson + AssetTypeId.Module + AssetTypeId.Unit

  private final val SearchFields =
    List("title", "subtitle", "description", "instructions", "content")

  // In some world we would include non-question embedded content in the student
  // view in order that rubrics could show
  private final val InstructorSearchFields =
    "embedded" :: SearchFields // embedded includes question content

  private final val MaxHits     = 25
  private final val CsvMaxRows  = 5000
  private final val CsvPageSize = 100
  private final val TimeLimit   = 45.seconds // 60 second web request timeout

  private final val TruncatedCourseSearchRow = CourseSearchRow(
    "** Search Timed Out, Results Truncated **",
    "",
    None,
    None,
    None,
    None,
    None,
    None
  )

  def hitContext(hit: CourseSearchWebHit): String =
    hit.highlights.view
      .filterKeys(field => !IgnoreHighlights.contains(field))
      .values
      .flatten
      .take(MaxHighlights)
      .map(HitRE.replaceAllIn(_, "$1"))
      .mkString(" … ")

  // Fields to ignore in the highlight context.
  private final val IgnoreHighlights = Set("data.title")
  private final val MaxHighlights    = 5
  private final val HitRE            = s"""\\{\\{\\{(.*?)}}}""".r
end CourseSearchWebController

private final case class CourseSearchWebQuery(
  query: Option[String],
  offset: Option[Int] = None,
  limit: Option[Int] = None,
)

private object CourseSearchWebQuery:
  implicit val codec: CodecJson[CourseSearchWebQuery] =
    CodecJson.casecodec3(CourseSearchWebQuery.apply, ArgoExtras.unapply)(
      "query",
      "offset",
      "limit"
    )

private final case class CourseSearchWebHit(
  edgePath: EdgePath,
  highlights: Map[String, List[String]],
)

private object CourseSearchWebHit:
  implicit val codec: CodecJson[CourseSearchWebHit] =
    CodecJson.casecodec2(CourseSearchWebHit.apply, ArgoExtras.unapply)(
      "path",
      "highlights",
    )

private final case class CourseSearchRow(
  course: String,
  section: String,
  unit: Option[String],
  module: Option[String],
  lesson: Option[String],
  title: Option[String],
  url: Option[String],
  context: Option[String],
)

private object CourseSearchRow:

  implicit val linkStatusRowHeaderEncoder: HeaderEncoder[CourseSearchRow] = HeaderEncoder.caseEncoder(
    "Course",
    "Section",
    "Unit",
    "Module",
    "Lesson",
    "Title",
    "Course URL",
    "Context",
  )(ArgoExtras.unapply)
end CourseSearchRow
