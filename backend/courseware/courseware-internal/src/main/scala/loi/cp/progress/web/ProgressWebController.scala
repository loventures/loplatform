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

package loi.cp.progress
package web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentDescriptor, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.ResponseBody
import loi.cp.content.{CourseContents, CourseWebUtils}
import loi.cp.course.{CourseAccessService, CourseEnrollmentService, CourseSection}
import loi.cp.lwgrade.GradeService
import loi.cp.path.Path
import loi.cp.progress.report.{Progress, ProgressReport}
import loi.cp.reference.EdgePath
import loi.cp.user.web.UserWebUtils
import scalaz.\/
import scalaz.std.string.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.boolean.*
import scaloi.syntax.boxes.*
import scaloi.syntax.option.*

import scala.jdk.CollectionConverters.*

/** A web controller for asking about progress.
  */
@Component
@Controller(value = "progreß", root = true)
class ProgressWebController(
  val componentInstance: ComponentInstance,
  courseAccessService: CourseAccessService,
  courseEnrollmentService: CourseEnrollmentService,
  courseWebUtils: CourseWebUtils,
  currentUser: UserDTO,
  gradeService: GradeService,
  progressService: LightweightProgressService,
  userWebUtils: UserWebUtils,
)(implicit
  cd: ComponentDescriptor,
) extends ApiRootComponent
    with ComponentImplementation:

  import ProgressWebController.*

  @Secured
  @RequestMapping(path = "progress/{sectionId}/report/{userId}", method = Method.GET)
  def getProgressReport(
    @PathVariable("sectionId") sectionId: Long,
    @PathVariable("userId") userId: Long,
    @MatrixParam paths: java.util.List[Path]
  ): ErrorResponse \/ ProgressReport =
    for
      section <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _       <- checkAccess(section, Seq(userId)).leftMap(_.to404)
      user    <- userWebUtils.loadUser(userId).leftMap(_.to404)
    yield
      val gradebook = gradeService.getGradebook(section, user)
      val progress  = progressService.loadProgress(section, user, gradebook)
      progress.toWebProgress(userId)

  @Secured
  @RequestMapping(path = "progress/{sectionId}/report/{userId}", method = Method.PUT)
  def setProgress(
    @PathVariable("sectionId") sectionId: Long,
    @PathVariable("userId") userId: Long,
    @RequestBody submissions: List[ProgressSubmission],
  ): ErrorResponse \/ ProgressReport =
    lazy val changes = submissions.map { case ProgressSubmission(path, value, tpe) =>
      import IncrementType.*
      val pcType: EdgePath => ProgressChange = tpe match
        case Some(TESTEDOUT)             => ProgressChange.testOut
        case Some(VISITED) if value > 0d => ProgressChange.visited
        case Some(SKIPPED)               => ProgressChange.skipped
        case _                           => ProgressChange.unvisit
      pcType(path.asEdgePath)
    }

    for
      section         <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _               <- checkAccess(section, Seq(userId)).leftMap(_.to404)
      user            <- userWebUtils.loadUser(userId).leftMap(_.to404)
      gradebook        = gradeService.getGradebook(section, user)
      updatedProgress <- progressService.updateProgress(section, user, gradebook, changes).leftMap(_.to422)
    yield updatedProgress.toWebProgress(userId)
  end setProgress

  @Secured
  @RequestMapping(path = "progress/{sectionId}/report", method = Method.GET)
  def getProgressReportForUsers(
    @PathVariable("sectionId") sectionId: Long,
    @MatrixParam("users") userIds: Seq[java.lang.Long],
    @MatrixParam paths: java.util.List[Path] // ignored for lwc
  ): ErrorResponse \/ Map[Long, ProgressReport] =
    for
      section <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _       <- checkAccess(section, userIds.unboxInside()).leftMap(_.to404)
      _       <- checkUserLimit(userIds.length)
      users   <- userWebUtils.loadUsers(userIds.unboxInside().toList).leftMap(_.to404)
    yield
      val userIds = users.map(_.userId)
      progressService
        .loadVisitationBasedProgress(section, userIds)
        .map({ case (userId, report) =>
          userId.value -> report.toWebProgress(userId.value)
        })

  @Secured
  @RequestMapping(path = "progress/{sectionId}/report/export", method = Method.GET)
  @ResponseBody(Array("text/csv"))
  def exportProgressReportForUsers(
    @PathVariable("sectionId") sectionId: Long,
  ): ErrorResponse \/ WebResponse =
    for
      section <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _       <- checkInstructorAccess(section).leftMap(_.to404)
      students = courseEnrollmentService.getEnrolledStudentDTOs(sectionId)
      userIds  = students.map(_.userId)
      _       <- checkUserLimit(userIds.length)
    yield
      val progressMap = progressService.loadVisitationBasedProgress(section, userIds)
      val progresses  = students.map(student => student -> progressMap.getOrElse(student, ProgressMap.empty))
      TextResponse.csvDownload("progressReport.csv") {
        csvHeader(section.contents) :: progresses
          .sortBy(t => csvOrder(t._1))
          .map((csvRow(section.contents)).tupled)
      }

  private def csvOrder(user: UserDTO) = user.familyName.toLowerCase -> user.givenName.toLowerCase // shame

  private def csvHeader(contents: CourseContents) = StudentMsg.i18n +: contents.nonRootElements.map(_.title)

  private def csvRow(contents: CourseContents)(user: UserDTO, progressMap: ProgressMap) =
    s"${user.givenName} ${user.familyName}" +: contents.nonRootElements.map { content =>
      progressMap.get(content.edgePath).map(_.weightedPercentage).foldZ(csvCell)
    }

  private def csvCell(pct: Double) = (pct.isNaN || pct.isInfinite).fold(NAMsg.i18n, f"$pct%.2f")

  @Secured
  @RequestMapping(path = "progress/{sectionId}/overall/{userId}", method = Method.GET)
  def getOverallProgress(
    @PathVariable("sectionId") sectionId: Long,
    @PathVariable("userId") userId: Long,
  ): ErrorResponse \/ Progress =
    for
      section <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _       <- checkAccess(section, Seq(userId)).leftMap(_.to404)
      user    <- userWebUtils.loadUser(userId).leftMap(_.to404)
    yield
      val gradebook = gradeService.getGradebook(section, user)
      val progress  = progressService.loadProgress(section, user, gradebook)
      // progress will always contain EdgePath.Root
      progress.map.getOrDefault(EdgePath.Root, Progress.missing)

  @Secured
  @RequestMapping(path = "progress/{sectionId}/overall", method = Method.GET)
  def getOverallProgressForUsers(
    @PathVariable("sectionId") sectionId: Long,
    @MatrixParam("users") userIds: Seq[java.lang.Long],
  ): ErrorResponse \/ Map[Long, Progress] =
    for
      section <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _       <- checkAccess(section, userIds.unboxInside()).leftMap(_.to404)
      _       <- checkUserLimit(userIds.length)
      users   <- userWebUtils.loadUsers(userIds.unboxInside().toList).leftMap(_.to404)
    yield
      val userIds = users.map(_.userId)
      progressService
        .loadVisitationBasedProgress(section, userIds)
        .map({ case (userId, report) =>
          userId.value -> report.toWebProgress(userId.value).getProgress(EdgePath.Root)
        })

  @Secured
  @RequestMapping(path = "progress/{sectionId}/overallReport", method = Method.GET)
  def getOverallReportForUsers(
    @PathVariable("sectionId") sectionId: Long,
    @QueryParam(value = "user", decodeAs = classOf[Long]) userIds: List[Long],
  ): ErrorResponse \/ Map[Long, ProgressReport] =
    for
      section <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _       <- checkInstructorAccess(section).leftMap(_.to404)
      _       <- checkUserLimit(userIds.length)
      users   <- userIds.nonEmpty.fold(
                   userWebUtils.loadUsers(userIds).leftMap(_.to404),
                   courseEnrollmentService.getEnrolledStudentDTOs(sectionId).take(100).right[ErrorResponse]
                 )
    yield
      val userIds = users.map(_.userId)
      progressService
        .loadVisitationBasedProgress(section, userIds)
        .map({ case (userId, report) =>
          userId.value -> withRootProgressOnly(report.toWebProgress(userId.value))
        })

  private def checkUserLimit(userCount: Int): ErrorResponse \/ Unit =
    (userCount < MaxProgressUsers) \/> ErrorResponse.badRequest("too many users")

  private def checkAccess(section: CourseSection, userIds: Seq[Long]): String \/ Unit =
    val hasAccess: Boolean =
      (userIds.forall(_ == currentUser.getId) // Only requesting yourself
        || courseAccessService.hasAdvisorAccess(section))

    hasAccess.elseLeft(s"cannot view progress for $userIds in ${section.id} as ${currentUser.id}")

  private def checkInstructorAccess(section: CourseSection): String \/ Unit =
    courseAccessService
      .hasAdvisorAccess(section.lwc)
      .elseLeft(s"cannot view progress for all users in ${section.id} as ${currentUser.id}")

  private def withRootProgressOnly(report: ProgressReport): ProgressReport =
    ProgressReport(
      report.userId,
      report.lastModified,
      report.progress.asScala.view.filterKeys(_.asEdgePath == EdgePath.Root).toMap.asJava
    )
end ProgressWebController

object ProgressWebController:

  private final val StudentMsg = I18nMessage.key("progress_column_student")
  private final val NAMsg      = I18nMessage.key("progress_cell_na")

  private final val MaxProgressUsers = 1640 // in memory of the Catalan revolt of 1640
