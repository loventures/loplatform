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

package loi.cp.content

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.{ComponentService, NoSuchComponentException}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.{EnrollmentFacade, EnrollmentWebService}
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.user.UserId
import loi.authoring.asset.Asset
import loi.cp.content.gate.*
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.course.lightweight.{LightweightCourse, Lwc}
import loi.cp.course.{CourseAccessService, CourseWorkspaceService}
import loi.cp.customisation.Customisation
import loi.cp.gatedate.*
import loi.cp.lwgrade.GradeService
import loi.cp.reference.EdgePath
import scalaz.\/
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.order.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.InstantInstances.*
import scaloi.misc.TimeSource
import scaloi.misc.TryInstances.*
import scaloi.syntax.AnyOps.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.DisjunctionOps.*
import scaloi.syntax.OptionOps.*
import scaloi.syntax.TryOps.*

import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Service
class ContentAccessServiceImpl(domain: => DomainDTO)(implicit
  componentService: ComponentService,
  courseAccessService: CourseAccessService,
  courseContentService: CourseContentService,
  courseWorkspaceService: CourseWorkspaceService,
  gateCustomisationService: ContentGateOverrideService,
  gradebookService: GradeService,
  now: TimeSource,
  performanceRuleService: PerformanceRuleStructureService,
  enrollmentWebService: EnrollmentWebService
) extends ContentAccessService:
  import ContentAccessServiceImpl.*

  override def getCourseAsLearner(course: Long, user: UserId): Try[LightweightCourse] =
    loadCourse(course, user, StudentAccess)

  override def getCourseAsInstructor(course: Long, user: UserId): Try[LightweightCourse] =
    loadCourse(course, user, InstructorAccess)

  override def getCourseAsAdministrator(course: Long, user: UserId): Try[LightweightCourse] =
    loadCourse(course, user, AdministratorAccess)

  override def getCourse(course: Long, user: UserId, instructor: Boolean): Try[LightweightCourse] =
    if instructor then getCourseAsInstructor(course, user) else getCourseAsLearner(course, user)

  override def getContentForInteraction(course: LightweightCourse, path: EdgePath, user: UserId): Try[CourseContent] =
    accessContent(course, path, user, GateStatus.Open)

  override def getContentReadOnly(course: LightweightCourse, path: EdgePath, user: UserId): Try[CourseContent] =
    accessContent(course, path, user, GateStatus.ReadOnly)

  override def readContent(courseId: Long, path: EdgePath, user: UserId): Try[(LightweightCourse, CourseContent)] =
    for
      course  <- getCourseAsLearner(courseId, user)
      content <- getContentReadOnly(course, path, user)
    yield (course, content)

  override def readContentT[A: ContentType](
    courseId: Long,
    path: EdgePath,
    user: UserId
  ): Try[(LightweightCourse, CourseContent, Asset[A])] =
    for
      (course, content) <- readContent(courseId, path, user)
      asset             <- ContentType[A].accept(content)
    yield (course, content, asset)

  override def readContents(
    courseId: Long,
    user: UserId,
    predicate: CourseContent => Boolean
  ): Try[(LightweightCourse, List[CourseContent])] =
    for
      course   <- getCourseAsLearner(courseId, user)
      contents <- findContents(course, user, GateStatus.ReadOnly, predicate)
    yield (course, contents)

  override def readContentsT[A: ContentType](
    courseId: Long,
    user: UserId
  ): Try[(LightweightCourse, List[(CourseContent, Asset[A])])] =
    for (course, contents) <- readContents(courseId, user, ContentType[A].predicate)
    yield (course, contents.flatMap(c => c -*> ContentType[A].option(c)))

  override def useContent(courseId: Long, path: EdgePath, user: UserId): Try[(LightweightCourse, CourseContent)] =
    for
      course  <- getCourseAsLearner(courseId, user)
      content <- getContentForInteraction(course, path, user)
    yield (course, content)

  override def useContentT[A: ContentType](
    courseId: Long,
    path: EdgePath,
    user: UserId
  ): Try[(LightweightCourse, CourseContent, Asset[A])] =
    for
      (course, content) <- useContent(courseId, path, user)
      asset             <- ContentType[A].accept(content)
    yield (course, content, asset)

  override def teachContent(courseId: Long, path: EdgePath, user: UserId): Try[(LightweightCourse, CourseContent)] =
    for
      course  <- getCourseAsLearner(courseId, user)
      content <- getContentForInteraction(course, path, user)
    yield (course, content)

  override def teachContentT[A: ContentType](
    courseId: Long,
    path: EdgePath,
    user: UserId
  ): Try[(LightweightCourse, CourseContent, Asset[A])] =
    for
      (course, content) <- teachContent(courseId, path, user)
      asset             <- ContentType[A].accept(content)
    yield (course, content, asset)

  /** Loads a course, checking rights and dates. */
  private def loadCourse(courseId: Long, user: UserId, access: Access): Try[LightweightCourse] =
    for
      course <- courseId.component_![LightweightCourse] `mapExceptions` invalidCourse
      rights <- courseAccessService.actualRights(course, user) `elseFailure` noRightsInCourse
      _      <- (course.getDisabled && !rights.isAdministrator) `thenFailure` courseSuspended
      _      <- checkStartDate(course, rights.likeInstructor).toTry(CourseNotYetStartedException.apply)
      _      <- checkShutdownDate(course, rights.likeInstructor).toTry(CourseAlreadyShutdownException.apply)
      _      <- checkAccess(access, rights) `elseFailure` insufficientRights
    yield course

  /** Loads all content matching a predicate accessible at a given level. */
  private def findContents(
    course: LightweightCourse,
    user: UserId,
    level: GateStatus,
    predicate: CourseContent => Boolean
  ): Try[List[CourseContent]] =
    for
      rights <- courseAccessService.actualRights(course, user) `elseFailure` noRightsInCourse
      _      <- checkEndDate(
                  course,
                  rights.likeInstructor,
                  level
                ).toTry(CourseAlreadyEndedException.apply) // this test is a bit out of place
      contents <- courseContentService.getCourseContents(course, rights.some)
      scrawny  <- contents.tree.filtr(predicate).success
      status   <- scrawny.traverse(gateStatus(course, user, contents, _, rights))
    yield
      val filteredTree = status.flatMap(_.filtl(_._1.head.status <= level))
      filteredTree.foldZ(_.unannotated.flatten).filter(predicate)

  /** Loads content for a specific access level. */
  private def accessContent(
    course: LightweightCourse,
    path: EdgePath,
    user: UserId,
    level: GateStatus
  ): Try[CourseContent] =
    for
      rights <- courseAccessService.actualRights(course, user) `elseFailure` noRightsInCourse
      _      <- checkEndDate(
                  course,
                  rights.likeInstructor,
                  level
                ).toTry(CourseAlreadyEndedException.apply) // this test is a bit out of place
      contentOpt <- courseContentService.getCourseContent(course, path, rights.some)
      content    <- contentOpt getOrElseF notFoundError(course, path)
      status     <- gateStatus(course, user, content.contents, content.branch, rights)
      _          <- status.flatten.find(_._1.head.status > level) `thenHollowFailure` gateLocked
    yield content.content

  /** Returns a not found error appropriate for whether content was hidden or never there. */
  private def notFoundError(
    course: LightweightCourse,
    path: EdgePath
  ): Try[ContentPath] =
    courseContentService.getCourseContentsInternal(course, Customisation.empty) flatMap { contents =>
      if contents.get(path).isDefined then ContentDeletedException(path).failure else contentNotFound.failure
    }

  /** Evaluate the gate status of the paths to the content item(s). */
  private def gateStatus(
    course: LightweightCourse,
    user: UserId,
    contents: CourseContents,
    contentTree: ContentTree,
    rights: CourseRights
  ): Try[AnnotatedTree[GateSummary *: EmptyTuple]] =
    for
      startDate <- startDate(course, user, rights.likeInstructor) `elseFailure` notEnrolledInCourse
      overrides <- gateCustomisationService.loadOverrides(course)
      ws         = courseWorkspaceService.loadReadWorkspace(course)
      perfRules <- performanceRuleService.computePerformanceRuleStructure(ws, course)
    yield
      val gradebook             = gradebookService.getGrades(user, course, contents)
      val policyRules           = PolicyRule.evaluatePolicyRules(contents, rights)
      val contentDates          = startDate.value.cata(ContentDateUtils.contentDates(contentTree, _), ContentDates.empty)
      val annotatedTree         = contentTree.annotate
      val temporallyRuled       = TemporalRule.addTemporalRules(contentDates.gateDates)(annotatedTree)
      val performantlyRuled     = PerformanceRule.addPerformanceRules(perfRules)(temporallyRuled)
      val temporallyEvaluated   = TemporalRule.evaluateTemporalRules(now.instant)(performantlyRuled)
      val performantlyEvaluated = PerformanceRule.evaluateRules(gradebook)(temporallyEvaluated)
      val policyRuled           = tree.mappedByEdgePath(policyRules)(performantlyEvaluated)
      GateSummary.collectGateSummary(rights.likeInstructor, overrides(user))(policyRuled)

  /** Gets the effective start date of a course for the purpose of gate computation. This handles standard courses and
    * courses with rolling student-centric start dates. Returns [[None]] if a student is not enrolled in a
    * rolling-enrolment course. What? How? Think guest access to a self-study course. This is an error.
    */
  private def startDate(course: Lwc, user: UserId, instructorLike: Boolean): Option[GateStartDate] =
    val enrollments: List[EnrollmentFacade] = enrollmentWebService.getUserEnrollments(user.id, course.id).asScala.toList
    // TODO: Consider user's domain for self study
    GateStartDate.forUser(course, enrollments, instructorLike, domain.timeZoneId)
end ContentAccessServiceImpl

object ContentAccessServiceImpl:

  /** Returns course start date on the left if it has not yet started and you are not an instructor. */
  private def checkStartDate(course: LightweightCourse, instructor: Boolean)(implicit
    now: TimeSource
  ): Instant \/ Unit =
    course.getStartDate.unless(instructor).filter(_ > now.instant).toLeftDisjunction(())

  /** Returns course end date on the left if it has stopped and you are not an instructor and want interact access. */
  private def checkEndDate(course: LightweightCourse, instructor: Boolean, level: GateStatus)(implicit
    now: TimeSource
  ): Instant \/ Unit =
    course.getEndDate.unless(instructor).when(level === GateStatus.Open).filter(_ <= now.instant).toLeftDisjunction(())

  /** Returns course shutdown date on the left if it has shut down and you are not an instructor. */
  private def checkShutdownDate(course: LightweightCourse, instructor: Boolean)(implicit
    now: TimeSource
  ): Instant \/ Unit =
    course.getShutdownDate.unless(instructor).filter(_ <= now.instant).toLeftDisjunction(())

  /** Checks whether rights grant a specific access level. */
  private def checkAccess(access: Access, rights: CourseAccessService.CourseRights): Boolean = access match
    case StudentAccess       => true
    case InstructorAccess    => rights.likeInstructor
    case AdministratorAccess => rights.isAdministrator

  // TODO: a nice algebra in a disjunction

  private def invalidCourse: PartialFunction[Throwable, Throwable] = {
    case _: NoSuchComponentException[?] => new ResourceNotFoundException("Course not found")
    case e                              => new InvalidRequestException("Invalid course", e)
  }

  private def courseSuspended = new AccessForbiddenException("Course is suspended")

  private def noRightsInCourse = new AccessForbiddenException("No rights in course")

  private def insufficientRights = new AccessForbiddenException("Insufficient rights")

  private def notEnrolledInCourse = new AccessForbiddenException("No enrolled in course")

  private def contentNotFound = new ResourceNotFoundException("Content not found")

  private def gateLocked(c: (GateSummary *: EmptyTuple, CourseContent)) = c match
    case (summary *: EmptyTuple, content) =>
      new AccessForbiddenException(s"Content ${content.edgePath} has gate status ${summary.status}")

  private sealed trait Access

  private case object StudentAccess       extends Access
  private case object InstructorAccess    extends Access
  private case object AdministratorAccess extends Access
end ContentAccessServiceImpl
