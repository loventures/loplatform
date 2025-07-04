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

package loi.cp.lti

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.enrollment.{EnrollmentFacade, EnrollmentWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import jakarta.servlet.http.HttpServletRequest
import loi.cp.analytics.ApiAnalyticsService
import loi.cp.course.CourseComponent
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.enrollment.{EnrollmentComponent, EnrollmentService}
import loi.cp.gatedate.GateDateSchedulingService
import loi.cp.integration.BasicLtiSystemComponent
import loi.cp.learnertransfer.LearnerTransferService
import loi.cp.lti.role.{LtiContextRole, LtiLoRole, LtiRole}
import loi.cp.role.{RoleService, RoleType}
import loi.cp.user.UserComponent
import scalaz.\/
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.GetOrCreate
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.foldable.*
import scaloi.syntax.localDateTime.*
import scaloi.syntax.option.*

import java.time.Instant
import java.util.Date
import scala.jdk.CollectionConverters.*

/** Handle LTI enrolments.
  */
@Service
final class LtiEnrolmentService(
  apiAnalyticsService: ApiAnalyticsService,
  enrollmentService: EnrollmentService,
  ltiCourseService: LtiCourseService,
  learnerTransferService: LearnerTransferService,
)(implicit
  enrollmentWebService: EnrollmentWebService,
  componentService: ComponentService,
  facadeService: FacadeService,
  gateDateService: GateDateSchedulingService,
  roleService: RoleService,
  domain: () => DomainDTO,
  now: TimeSource
):
  import LtiEnrolmentService.*
  import jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN as NOPE

  /** Process the roles for a non-course launch. */
  def processRoles(
    user: UserComponent
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Unit =
    for
      rolesOpt      <- ltiParam(RolesParameter)
      roleOpt       <- rolesOpt.flatMap(mappedRole).right
      domainRoleOpt <- roleOpt.flatMap(_.domainRole) traverse getRole
    yield domainRoleOpt foreach processDomainRole(user)

  /** Process the roles for a course launch. Return whether instructor-like. */
  def processRoles(user: UserComponent, course: CourseComponent)(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent,
  ): LtiError \/ Boolean =
    for
      roles         <- ltiParam_!(RolesParameter)
      role          <- mappedRole(roles) \/> GenericLtiError("lti_no_supported_roles", roles).widen
      instructorLike = role.courseRole exists instructorLikeRoles.contains
      courseRoleOpt <- role.courseRole traverse getRole
      domainRoleOpt <- role.domainRole traverse getRole
      startDateOpt  <- ltiParamT[Instant](CustomEnrollmentStartDateParameter)
      _             <- startDateOpt traverse enrolmentStarted
      endDateOpt    <- ltiParamT[Instant](CustomEnrollmentEndDateParameter)
      _             <- endDateOpt traverse enrolmentNotEnded
      isEnrolled     = enrollmentWebService.isCurrentOrFormerMember(user.id, course.id)
      _             <-
        (isEnrolled || instructorLike) either Option.empty[EnrollmentComponent] `orElse` learnerTransfer(user, course)
      enrolmentOpt   =
        courseRoleOpt.map(enrolUser(course, _, user, startDateOpt.map(Date.from), endDateOpt.map(Date.from)))
      _             <- enrolmentOpt.exists(_.result.getDisabled) \/>! FriendlyLtiError("lti_enrollment_suspended", NOPE).widen
    yield
      domainRoleOpt foreach processDomainRole(user)
      instructorLike

  /** Find the student's prior section, if any, and transfer them. */
  private def learnerTransfer(user: UserComponent, course: CourseComponent)(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent,
  ): LtiError \/ Option[EnrollmentComponent] =
    for
      transfers  <- ltiParam(CustomTransferHistoryParameter)
      transferIds = transfers.foldZ(_.split(",").toList)
      priorOpt    = transferIds.findMap(ltiCourseService.findTransferSection(_, user))
      xfer       <- priorOpt.cata(performTransfer(user, _, course), None.right)
    yield xfer

  private def performTransfer(user: UserComponent, src: LightweightCourse, dst: CourseComponent)(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent,
  ): LtiError \/ Option[EnrollmentComponent] =
    logger.info(s"LTI transfer ${user.id} from ${src.id} to ${dst.id}")
    learnerTransferService
      .transferLearner(user.id, src.id, dst.id)
      .bimap(
        errors =>
          val errStr = errors.toList.mkString(",")
          logger.warn(s"Learner transfer error: $errStr")
          GenericLtiError("lti_transfer_failed", errStr)
        ,
        transfer => transfer.destinationEnrollment.some
      )
  end performTransfer

  /** Check that the enrolment has started. */
  private def enrolmentStarted(startDate: Instant): LtiError \/ Unit =
    (now > startDate) \/> FriendlyLtiError("lti_enrollment_not_started", startDate, NOPE)

  /** Check that the enrolment has not ended. */
  private def enrolmentNotEnded(endDate: Instant): LtiError \/ Unit =
    (now <= endDate) \/> FriendlyLtiError("lti_enrollment_ended", endDate, NOPE)

  /** Process a domain enrollment. If the user does not already have the domain role, add it. */
  private def processDomainRole(user: UserComponent)(role: RoleType): Unit =
    if !enrollmentWebService
        .getUserEnrollments(user.getId, domain.id)
        .asScala
        .exists(_.getRoleId == role.id)
    then enrollmentWebService.createEnrollment(domain.id, role.id, user.getId, LtiDataSource)

  /** Find the first mapped role for the LTI roles parameter. */
  private def mappedRole(roles: String)(implicit system: BasicLtiSystemComponent): Option[RoleMapping] =
    roles.split(',').flatMap(roleMapping(_)).headOption

  /** Find the mapped role for a single LTI role. */
  private def roleMapping(roleId: String)(implicit system: BasicLtiSystemComponent): Option[RoleMapping] =
    system.getRoleMappings.toList
      .findMap(rm => (roleId == rm.ltiRole).option(RoleMapping(rm.roleId, rm.domainRoleId)))
      .orElse(LtiRole.withNameOption(roleId).flatMap(ltiRoleMapping.get))

  /** Get a role from the database by id. */
  private def getRole(roleId: String): LtiError \/ RoleType =
    Option(roleService.getRoleByRoleId(roleId)).map(RoleType.apply) \/> GenericLtiError(
      "lti_invalid_role_mapping",
      roleId
    )

  /** Get or create the sole enrolment for a user in a course. Support our wacky LTI logic by which we allow suspended
    * enrolments in a course to override and prevent the LTI launch...
    */
  private def enrolUser(
    course: CourseComponent,
    role: RoleType,
    user: UserComponent,
    startDateOpt: Option[Date],
    endDateOpt: Option[Date],
  )(implicit system: BasicLtiSystemComponent): GetOrCreate[EnrollmentFacade] =

    // Do not use datasource to find existing enrollments. They may have come from an import/manually created
    val existingEnrolments =
      enrollmentWebService.getUserEnrollments(user.getId, course.getId, EnrollmentType.ALL).asScala
    val disabledEnrolment  = existingEnrolments.find(_.getDisabled)
    val dataSource         = Some(LtiDataSource)

    val defaultEndDate = system.getBasicLtiConfiguration.daysToKeepActive.map(d => now.localDateTime.plusDays(d))
    val endDate        = endDateOpt.orElse(defaultEndDate.map(_.asDate))

    disabledEnrolment match
      case Some(de)                                                                             =>
        GetOrCreate.gotten(de)
      case None if existingEnrolments.size == 1 && existingEnrolments.head.getRoleId == role.id =>
        // Update existing enrolment.
        val enrolmentFacade = existingEnrolments.head
        val enrolment       = enrolmentFacade.component[EnrollmentComponent]

        val startDateChange = enrolment.getStartTime != startDateOpt.orNull

        val change = enrolment.getDataSource != dataSource ||
          startDateChange ||
          enrolment.getStopTime != endDate.orNull

        enrolment.setDataSource(dataSource)
        startDateOpt.foreach(enrolment.setStartTime)
        endDate.foreach(enrolment.setStopTime)

        if change then apiAnalyticsService.emitEnrollmentUpdateEvent(enrolment)

        if startDateChange then
          LightweightCourse.unapply(course).foreach(gateDateService.scheduleGateDateEvents(_, user.userId))

        GetOrCreate.gotten(enrolmentFacade)
      case None                                                                                 =>
        val newEnrolmentId =
          enrollmentService.setEnrollment(user.userId, course, role, dataSource, startDateOpt, endDate)
        LightweightCourse.unapply(course).foreach(gateDateService.scheduleGateDateEvents(_, user.userId))
        val enrolment      = newEnrolmentId.facade[EnrollmentFacade]
        GetOrCreate.created(enrolment)
    end match
  end enrolUser
end LtiEnrolmentService

object LtiEnrolmentService:
  private final val RolesParameter                      = "roles"
  private final val CustomEnrollmentStartDateParameter  = "custom_enrollment_start_date"
  private final val CustomEnrollmentEndDateParameter    = "custom_enrollment_end_date"
  private[lti] final val CustomTransferHistoryParameter = "custom_transfer_history"

  /** Data source used to record the enrollments. */
  private final val LtiDataSource = "LTI"

  /** Mapping from LTI roles to LO role identifiers. */
  private final val ltiRoleMapping = Map[LtiRole, RoleMapping](
    LtiLoRole.Student             -> RoleMapping("student", "student"),
    LtiLoRole.TrialLearner        -> RoleMapping("trialLearner", "student"),
    LtiContextRole.Learner        -> RoleMapping("student", "student"),
    LtiContextRole.Instructor     -> RoleMapping("instructor", "faculty"),
    LtiContextRole.Mentor_Advisor -> RoleMapping("advisor", "advisor"),
  )

  /** Instructor like role identifiers. */
  private final val instructorLikeRoles = Set("instructor", "advisor")

  /** Role mapping within a course and optionally domain. */
  final case class RoleMapping(courseRole: Option[String], domainRole: Option[String])

  private final val logger = org.log4s.getLogger

  object RoleMapping:
    def apply(courseRole: String, domainRole: String): RoleMapping = RoleMapping(Option(courseRole), Option(domainRole))
end LtiEnrolmentService
