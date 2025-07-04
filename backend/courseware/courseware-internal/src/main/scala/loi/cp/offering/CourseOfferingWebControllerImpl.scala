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

package loi.cp.offering

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueries.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.web.ExportFile
import kantan.csv.*
import kantan.csv.ops.*
import loi.asset.lesson.model.Lesson
import loi.authoring.project.ProjectService
import loi.authoring.web.{AuthoringApiWebUtils, ExceptionResponses}
import loi.cp.content.{ContentTree, CourseContent, CourseContentService}
import loi.cp.course.lightweight.LightweightCourse
import scalaz.\/
import scalaz.std.list.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.data.ListTree.Node
import scaloi.syntax.ʈry.*

@Component
class CourseOfferingWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  contentService: CourseContentService,
  offeringService: ProjectOfferingService,
  projectService: ProjectService,
  urlService: CurrentUrlService,
  webUtils: AuthoringApiWebUtils,
) extends CourseOfferingWebController
    with ComponentImplementation:
  import CourseOfferingWebController.*

  override def getCourseOfferings(query: ApiQuery): ApiQueryResults[CourseOffering] =
    get(query).map(CourseOffering.apply)

  private def get(query: ApiQuery): ApiQueryResults[LightweightCourse] =
    ApiQuerySupport
      .query(offeringService.queryOfferings, query.sublet[LightweightCourse], classOf[LightweightCourse])

  override def getCourseOffering(id: Long): Option[CourseOffering] =
    get(id).map(CourseOffering.apply)

  private def get(id: Long): Option[LightweightCourse] =
    get(ApiQuery.byId(id, classOf[LightweightCourse])).asOption()

  override def deleteCourseOffering(id: Long): ErrorResponse \/ Unit =
    for section <- get(id) \/> ErrorResponse.notFound
    yield section.delete()

  def flattenLessons(content: CourseContent, subforest: List[ContentTree]): ContentTree =
    val flatterSubforest = subforest flatMap {
      case Node(childLabel, subsubforest) if childLabel.asset.is[Lesson] => subsubforest
      case o                                                             => o :: Nil
    }
    Node(content, flatterSubforest)

  override def getLtiLaunchInfo(id: Long): ErrorResponse \/ Seq[LtiLaunchInfo] =
    for
      lwc      <- get(id) \/> ErrorResponse.notFound
      contents <- contentService.getCourseContents(lwc) \/> ExceptionResponses.exceptionResponse
    yield contents.tree.endoRebuild(flattenLessons).zipWithDepth.flatCollect {
      case (content, depth) if depth > 0 =>
        LtiLaunchInfo(
          content.edgePath.toString,
          content.title,
          depth,
          content.gradingPolicy.map(_.isForCredit)
        )
    }

  override def getActivityLinks(id: Long, request: WebRequest): ErrorResponse \/ WebResponse =
    for
      lwc      <- get(id) \/> ErrorResponse.notFound
      contents <- contentService.getCourseContents(lwc) \/> ExceptionResponses.exceptionResponse
    yield
      val items = contents.tree.endoRebuild(flattenLessons).zipWithDepth.flatCollect {
        case (content, depth) if depth > 0 =>
          ActivityCsvRow(
            "\t" * (depth - 1) + content.title,
            urlService.getUrl(s"/lwlti/offering/${lwc.getGroupId}/${content.edgePath}"),
            content.gradingPolicy.exists(_.isForCredit)
          )
      }
      val out   = ExportFile.create(s"LTI - ${lwc.getName}.csv", MediaType.CSV_UTF_8, request)
      out.file.writeCsv(items, rfc.withHeader)
      FileResponse(out.toFileInfo)

  override def downloadLtiInfo(ids: List[Long], request: WebRequest): ErrorResponse \/ WebResponse =
    for offerings <- ids.traverseU(id => get(id) \/> ErrorResponse.notFound)
    yield
      val info = offerings map { offering =>
        val projectName = offering.loadBranch().project.map(_.name)
        val launchUrl   = urlService.getUrl(s"/lwlti/offering/${offering.getGroupId}")
        LtiLaunchCsvRow(
          offeringId = offering.getGroupId,
          projectName = projectName,
          courseName = offering.getName,
          launchUrl = launchUrl
        )
      }
      val out  = ExportFile.create("ltiLaunchInfo.csv", MediaType.CSV_UTF_8, request)
      out.file.writeCsv(info, rfc.withHeader)
      FileResponse(out.toFileInfo)

  override def updateStatus(id: Long, status: OfferingStatus): ErrorResponse \/ OfferingStatus =
    for offering <- get(id) \/> ErrorResponse.notFound
    yield
      offeringService.invalidateOfferings()
      offering.setDisabled(status.disabled)
      status

  override def countSections(branchId: Long): ErrorResponse \/ Int =
    val branch = webUtils.branchOrFakeBranchOrThrow404(branchId)
    for count <- offeringService.countCourseSections(branch) \/> ExceptionResponses.exceptionResponse
    yield count
end CourseOfferingWebControllerImpl
