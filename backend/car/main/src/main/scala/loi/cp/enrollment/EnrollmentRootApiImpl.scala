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

package loi.cp.enrollment

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse}
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentInterface,
  ComponentService
}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.data.{DataSupport, DataTypes}
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.enrollment.{EnrollmentFacade, EnrollmentWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.{UserId, UserWebService}
import loi.cp.analytics.ApiAnalyticsService
import loi.cp.course.CourseComponent
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.enrollment.EnrollmentRootApi.{
  EnrollmentBatchDTO,
  EnrollmentDTO,
  EnrollmentTransitionDTO,
  UserEnrollmentDTO
}
import loi.cp.gatedate.GateDateSchedulingService
import loi.cp.role.{RoleComponent, RoleType}
import loi.cp.user.{UserComponent, componentToUserId}
import scalaz.\/
import scalaz.std.list.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.boolean.*
import scaloi.syntax.boxes.*

import java.util.Date
import scala.jdk.CollectionConverters.*

@Component
class EnrollmentRootApiImpl(
  apiAnalyticsService: ApiAnalyticsService,
  val componentInstance: ComponentInstance,
  gateDateService: GateDateSchedulingService,
  enrollmentService: EnrollmentService,
  ews: EnrollmentWebService,
  userWebService: UserWebService,
)(implicit
  cs: ComponentService,
  fs: FacadeService,
) extends EnrollmentRootApi
    with ComponentImplementation:

  override def deleteUserEnrollments(courseId: Long, userId: Long): ErrorResponse \/ NoContentResponse =
    for
      course <- courseExists(courseId)
      user   <- Option(userWebService.getUser(userId)) \/> ErrorResponse.notFound
    yield
      val ids = ews.removeGroupEnrollmentsFromUser(course.getId, user.getId).asScala.unboxInside()
      ids.foreach(apiAnalyticsService.emitEnrollmentDeleteEvent)
      rescheduleGateDateEvents(course, user)
      NoContentResponse

  override def deleteEnrollment(courseId: Long, enrollmentId: Long): ErrorResponse \/ NoContentResponse =
    for
      course     <- courseExists(courseId)
      enrollment <- enrollmentExists(enrollmentId)
      _          <- (enrollment.getGroupId == course.getId) \/> ErrorResponse.notFound
    yield
      ews.removeEnrollment(enrollment.getId)
      apiAnalyticsService.emitEnrollmentDeleteEvent(enrollment.getId)
      rescheduleGateDateEvents(course, enrollment.getUser)
      NoContentResponse

  override def createEnrollments(
    courseId: Long,
    enrollmentDTO: EnrollmentBatchDTO
  ): ErrorResponse \/ List[EnrollmentComponent] =
    for
      course <- courseExists(courseId)
      users  <- enrollmentDTO.ids.traverseU(userId => userId.component_?[UserComponent] \/> ErrorResponse.notFound)
      role   <- roleExists(enrollmentDTO.roleId)
    yield
      val enrolments = users.map(user => enrollmentService.setEnrollment(user, course, role))
      users.foreach(rescheduleGateDateEvents(course, _))
      enrolments

  override def updateUserEnrollment(
    courseId: Long,
    userId: Long,
    enrollmentDTO: EnrollmentDTO
  ): ErrorResponse \/ EnrollmentComponent =
    for
      course <- courseExists(courseId)
      user   <- userExists(userId)
      role   <- roleExists(enrollmentDTO.roleId)
    yield
      val enrollmentId = enrollmentService.setEnrollment(user, course, role)
      rescheduleGateDateEvents(course, user)
      enrollmentId.component[EnrollmentComponent]

  override def updateEnrollment(
    courseId: Long,
    enrollmentId: Long,
    enrollmentDTO: UserEnrollmentDTO
  ): ErrorResponse \/ EnrollmentComponent =
    for
      course <- courseExists(courseId)
      facade <- enrollmentExists(enrollmentId)
      _      <- (facade.getGroupId == course.getId) \/> ErrorResponse.notFound
      role   <- roleExists(enrollmentDTO.roleId)
    yield
      val enrollment = facade.component[EnrollmentComponent]

      val change = enrollment.getRoleId != role.id ||
        seconds(enrollment.getStartTime) != seconds(enrollmentDTO.startTime) ||
        seconds(enrollment.getStopTime) != seconds(enrollmentDTO.stopTime) ||
        enrollment.isDisabled != enrollmentDTO.disabled

      enrollment.setRoleId(role.id)
      enrollment.setStartTime(enrollmentDTO.startTime)
      enrollment.setStopTime(enrollmentDTO.stopTime)
      facade.setDisabled(enrollmentDTO.disabled)

      if change then apiAnalyticsService.emitEnrollmentUpdateEvent(enrollment)
      rescheduleGateDateEvents(course, facade.getUser)
      ews.invalidateEnrollment(facade)
      enrollment

  // java.sql.Timestamp (in an entity) is *never* equal to a Date
  private def seconds(time: Date): Long =
    Option(time).cata(_.getTime / 1000L, 0L)

  override def getEnrollmentsForCourse(
    courseId: Long,
    query: ApiQuery
  ): ErrorResponse \/ ApiQueryResults[EnrollmentComponent] =
    for course <- courseExists(courseId)
    yield
      val qb = ews.getGroupEnrollmentsQuery(course.getId, EnrollmentWebService.EnrollmentType.ALL)
      ApiQuerySupport.query(qb, query, classOf[EnrollmentComponent])

  override def getEnrollmentForCourseUser(
    courseId: Long,
    userId: Long,
    query: ApiQuery
  ): ErrorResponse \/ ApiQueryResults[EnrollmentComponent] =
    for _ <- courseExists(courseId)
    yield
      val qb = ews
        .getGroupEnrollmentsQuery(courseId, EnrollmentWebService.EnrollmentType.ALL)
        .addCondition(DataTypes.META_DATA_TYPE_PARENT_ID, "eq", userId)
      ApiQuerySupport.query(qb, query, classOf[EnrollmentComponent])

  override def addUserEnrollment(
    courseId: Long,
    userId: Long,
    userEnrollmentDTO: UserEnrollmentDTO
  ): ErrorResponse \/ EnrollmentComponent =
    for
      course <- courseExists(courseId)
      user   <- userExists(userId)
      role   <- roleExists(userEnrollmentDTO.roleId)
      _      <- !ews
                  .getUserEnrollments(user.getId, course.getId, EnrollmentType.ALL)
                  .asScala
                  .exists(e => e.getRoleId == role.id) \/> enrollmentWithSpecifiedRoleExists(role.name)
    yield
      val facade     = ews.addEnrollment(user.getId, course.getId)
      facade.setRoleId(role.id)
      facade.setStartTime(DataSupport.defaultToMinimal(userEnrollmentDTO.startTime))
      facade.setStopTime(DataSupport.defaultToMaximal(userEnrollmentDTO.stopTime))
      facade.setDisabled(userEnrollmentDTO.disabled)
      ews.invalidateEnrollment(facade)
      rescheduleGateDateEvents(course, user)
      val enrollment = facade.component[EnrollmentComponent]
      apiAnalyticsService.emitEnrollmentCreateEvent(enrollment)
      enrollment

  override def transition(
    courseId: Long,
    userId: Long,
    dto: EnrollmentTransitionDTO
  ): ErrorResponse \/ List[EnrollmentComponent] =
    for
      course <- courseExists(courseId)
      user   <- userExists(userId)
    yield
      val enrolments = ews
        .getUserEnrollments(user.getId, course.getId, EnrollmentType.ALL)
        .asScala
        .map(enrollmentFacade =>
          enrollmentFacade.setDisabled(dto.disabled)
          ews.invalidateEnrollment(enrollmentFacade)
          val component = enrollmentFacade.component[EnrollmentComponent]
          apiAnalyticsService.emitEnrollmentUpdateEvent(component)
          component
        )
        .toList
      rescheduleGateDateEvents(course, user)
      enrolments

  private def rescheduleGateDateEvents(crs: Id, usr: Id): Unit =
    for
      course <- crs.component_?[ComponentInterface]
      lwc    <- LightweightCourse.unapply(course)
      user   <- usr.component_?[UserComponent]
    do gateDateService.scheduleGateDateEvents(lwc, UserId(user.id()))

  private def enrollmentWithSpecifiedRoleExists(roleName: String): ErrorResponse =
    ErrorResponse.validationError("role", roleName)("An enrollment with the specified role already exists.")

  private def courseExists(courseId: Long) = courseId.component_?[CourseComponent] \/> ErrorResponse.notFound

  private def userExists(userId: Long) = userId.component_?[UserComponent] \/> ErrorResponse.notFound

  private def enrollmentExists(enrollmentId: Long) = enrollmentId.facade_?[EnrollmentFacade] \/> ErrorResponse.notFound

  private def roleExists(roleId: Long) =
    roleId.component_?[RoleComponent].map(RoleType.apply) \/> ErrorResponse.notFound
end EnrollmentRootApiImpl
