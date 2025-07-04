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

package loi.cp.notification

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueries.ApiQueryOps
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.notification.AlertFinder
import com.learningobjects.cpxp.service.notification.NotificationConstants.*
import com.learningobjects.cpxp.service.query.{BaseDataProjection, Comparison, Function, QueryBuilder}
import scaloi.syntax.DateOps.DateOrder
import scaloi.syntax.OptionOps.*

import java.util.Date

@Component
class AlertRootApiImpl(
  val componentInstance: ComponentInstance,
)(implicit cs: ComponentService, fs: FacadeService)
    extends AlertRootApi
    with ComponentImplementation:
  import AlertRootApi.*

  // Get a summary of filtered alerts
  override def summary(q: ApiQuery): AlertSummary =
    val uf      = notificationParent
    // Add alert component property mappings to the api query
    val builder =
      new ApiQuery.Builder(q).addPropertyMappings(classOf[Alert])

    uf.getViewTime foreach (builder.addFilter(Alert.TimeProperty, PredicateOperator.GREATER_THAN, _))

    val q2 = builder.build.withDataModel[Alert]
    // Query for MAX(date), COUNT(*) of the matching notifications
    val a  = ApiQuerySupport
      .getQueryBuilder(queryAlerts, q2)
      .addCondition(DATA_TYPE_ALERT_VIEWED, Comparison.eq, false)
      .setDataProjection(
        Array(
          BaseDataProjection.ofAggregateData(DATA_TYPE_ALERT_TIME, Function.MAX),
          BaseDataProjection.ofAggregateData(DATA_TYPE_ALERT_COUNT, Function.COUNT)
        )
      )
      .getResult[Array[Object]] // meh
    // Cast the results into the corresponding types
    val date  = Option(a(0).asInstanceOf[Date])
    val count = a(1).asInstanceOf[Number].longValue
    // Return the summary
    AlertSummary(count, date, uf.getViewTime)
  end summary

  // Query all filtered alerts
  override def get(q: ApiQuery): ApiQueryResults[Alert] =
    ApiQueries.query[Alert](queryAlerts, q)

  // Get a specific alert by id
  override def get(id: Long): Option[Alert] =
    notificationParent.getAlert(id).map(nf => nf.component[Alert]).filter(_.getNotification != null)

  override def view(alertId: Long): Unit =
    notificationParent.getAlert(alertId).map(nf => nf.component[Alert].view())

  // Update the timestamp of the most-recently-viewed alert
  override def viewed(viewed: AlertViewed): Unit =
    for
      d       <- Option(viewed.date)
      uf       = notificationParent
      viewTime = uf.getViewTime.max(d)
    do uf.setViewTime(viewTime)

  private def queryAlerts: QueryBuilder =
    val qb = notificationParent.queryAlerts
    // an inner join meant to exclude AlertFinders whose NotificationFinder has been deleted (del not null)
    qb.getOrCreateJoinQuery(AlertFinder.DATA_TYPE_ALERT_NOTIFICATION, ITEM_TYPE_NOTIFICATION)
    qb

  // Get the notification parent for the current user
  private def notificationParent: NotificationParentFacade =
    Current.getUserDTO.facade[NotificationParentFacade]
end AlertRootApiImpl
