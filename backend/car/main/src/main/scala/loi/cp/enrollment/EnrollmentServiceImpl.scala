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

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.data.{DataSupport, DataTypes}
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.enrollment.{EnrollmentConstants, EnrollmentFacade}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{QueryBuilder, QueryService}
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.cpxp.util.DateUtils
import com.learningobjects.de.group.GroupComponent
import loi.cp.analytics.ApiAnalyticsService
import loi.cp.role.RoleType
import scaloi.misc.TimeSource

import java.util.Date

@Service
class EnrollmentServiceImpl(
  apiAnalyticsService: ApiAnalyticsService,
  queryService: QueryService,
  timeSource: => TimeSource,
)(implicit componentService: ComponentService, facadeService: FacadeService)
    extends EnrollmentService:

  override def loadEnrollments(
    userId: Long,
    groupId: Long,
    enrollmentType: EnrollmentType
  ): List[EnrollmentFacade] =
    queryFacades(userId, groupId, enrollmentType).getFacades[EnrollmentFacade].toList

  override def setEnrollment(
    user: UserId,
    group: GroupComponent,
    role: RoleType,
    dataSource: Option[String],
    startTime: Option[Date] = None,
    stopTime: Option[Date] = None,
    disabled: Boolean = false,
  ): EnrollmentComponent =

    val existing = queryFacades(user.id, group.getId, EnrollmentType.ALL).getFacades[EnrollmentFacade].toList

    existing match
      case Nil =>
        val newFacade = facadeService.addFacade(user.id, classOf[EnrollmentFacade])
        newFacade.setGroupId(group.getId)
        newFacade.setRoleId(role.id)
        newFacade.setDataSource(dataSource.orNull)
        newFacade.setStartTime(min(startTime))
        newFacade.setStopTime(max(stopTime))
        newFacade.setCreatedOn(timeSource.instant)
        newFacade.setDisabled(disabled)

        invalidateQueries(newFacade)

        val newEnrollment = newFacade.component[EnrollmentComponent]
        apiAnalyticsService.emitEnrollmentCreateEvent(newEnrollment)

        newEnrollment

      case head :: tail =>
        if tail.nonEmpty then
          tail.foreach(facade =>
            facade.delete()
            apiAnalyticsService.emitEnrollmentDeleteEvent(facade.getId)
          )

        val change            = setAttributes(head, role.id, dataSource, startTime, stopTime, disabled)
        val updatedEnrollment = head.component[EnrollmentComponent]

        if tail.nonEmpty || change then invalidateQueries(head)

        if change then apiAnalyticsService.emitEnrollmentUpdateEvent(updatedEnrollment)

        updatedEnrollment
    end match
  end setEnrollment

  // the editable administrative attributes of enrollmentfinder
  private def setAttributes(
    facade: EnrollmentFacade,
    roleId: Long,
    dataSource0: Option[String],
    startTime0: Option[Date],
    stopTime0: Option[Date],
    disabled: Boolean
  ): Boolean =

    val dataSource = dataSource0.orNull
    val startTime  = min(startTime0)
    val stopTime   = max(stopTime0)

    val change = facade.getRoleId != roleId ||
      facade.getDataSource != dataSource ||
      facade.getStartTime != startTime ||
      facade.getStopTime != stopTime ||
      facade.getDisabled != disabled

    facade.setRoleId(roleId)
    facade.setDataSource(dataSource)
    facade.setStartTime(startTime)
    facade.setStopTime(stopTime)
    facade.setDisabled(disabled)

    change
  end setAttributes

  override def updateEnrollment(
    enrollment: EnrollmentComponent,
    role: RoleType,
    dataSource: Option[String],
    startTime: Option[Date],
    stopTime: Option[Date],
    disabled: Boolean,
  ): Unit =
    val facade = enrollment.facade[EnrollmentFacade]
    val change = setAttributes(facade, role.id, dataSource, startTime, stopTime, disabled)
    if change then
      invalidateQueries(facade)
      apiAnalyticsService.emitEnrollmentUpdateEvent(enrollment)
  end updateEnrollment

  override def deleteEnrollment(id: Long): Unit =
    id.facade_?[EnrollmentFacade]
      .foreach(enrollment =>
        enrollment.delete()
        invalidateQueries(enrollment)
        apiAnalyticsService.emitEnrollmentDeleteEvent(id)
      )

  override def transferEnrollment(enrollment: EnrollmentComponent, destinationId: Long): EnrollmentComponent =
    val oldFacade = enrollment.facade[EnrollmentFacade]
    oldFacade.delete()
    invalidateQueries(oldFacade)
    apiAnalyticsService.emitEnrollmentDeleteEvent(enrollment.getId)

    val newFacade = facadeService.addFacade(oldFacade.getUser.getId, classOf[EnrollmentFacade])
    newFacade.setGroupId(destinationId)
    setAttributes(newFacade, oldFacade.getRoleId, Some("Transfer"), None, None, disabled = false)
    invalidateQueries(newFacade)

    val newEnrollment = newFacade.component[EnrollmentComponent]
    apiAnalyticsService.emitEnrollmentCreateEvent(newEnrollment)
    newEnrollment
  end transferEnrollment

  private def invalidateQueries(enrollment: EnrollmentFacade): Unit =
    queryService.invalidateQuery(EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + enrollment.getGroupId)
    queryService.invalidateQuery(EnrollmentConstants.INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP + enrollment.getParentId)

  private def queryFacades(userId: Long, groupId: Long, enrollmentType: EnrollmentType): QueryBuilder =
    val qb = queryService
      .queryParent(userId, EnrollmentConstants.ITEM_TYPE_ENROLLMENT)
      .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, "eq", groupId)
      .addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP + userId)
      .addInvalidationKey(EnrollmentConstants.INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP + groupId)

    if enrollmentType == EnrollmentType.ACTIVE_ONLY then includeActiveEnrollmentsOnly(qb)

    qb
  end queryFacades

  private def includeActiveEnrollmentsOnly(qb: QueryBuilder): Unit =
    qb.addCondition(DataTypes.DATA_TYPE_START_TIME, "le", DateUtils.getApproximateTimeCeiling(timeSource.date))
    qb.addCondition(DataTypes.DATA_TYPE_STOP_TIME, "gt", DateUtils.getApproximateTime(timeSource.date))
    qb.addCondition(DataTypes.DATA_TYPE_DISABLED, "ne", true)

  private def min(date: Option[Date]): Date = date.map(DataSupport.defaultToMinimal).getOrElse(DataSupport.MIN_TIME)
  private def max(date: Option[Date]): Date = date.map(DataSupport.defaultToMaximal).getOrElse(DataSupport.MAX_TIME)
end EnrollmentServiceImpl
