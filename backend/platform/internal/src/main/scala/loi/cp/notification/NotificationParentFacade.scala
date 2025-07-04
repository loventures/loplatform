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

import java.util.Date

import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.notification.NotificationConstants.*
import com.learningobjects.cpxp.service.notification.SubscribeFinder
import com.learningobjects.cpxp.service.query.QueryBuilder
import com.learningobjects.cpxp.service.user.UserConstants
import scaloi.GetOrCreate

@FacadeItem(UserConstants.ITEM_TYPE_USER)
trait NotificationParentFacade extends Facade:
  @FacadeData(DATA_TYPE_USER_ALERT_VIEW_TIME)
  def getViewTime: Option[Date]
  def setViewTime(date: Date): Unit

  @FacadeChild
  def getAlert(id: Long): Option[AlertFacade]
  def getOrCreateAlertByAggregationKeyAndViewed(
    @FacadeCondition(DATA_TYPE_ALERT_AGGREGATION_KEY) aggregationKey: String,
    @FacadeCondition(DATA_TYPE_ALERT_VIEWED) viewed: Boolean = false // shame?
  ): GetOrCreate[AlertFacade]
  def queryAlerts: QueryBuilder

  @FacadeChild
  def getSubscribe(id: Long): Option[SubscribeFacade]
  def findSubscribe(
    @FacadeCondition(SubscribeFinder.DATA_TYPE_SUBSCRIBE_CONTEXT) context: Long
  ): Option[SubscribeFacade]
  def getOrCreateSubscribe(
    @FacadeCondition(SubscribeFinder.DATA_TYPE_SUBSCRIBE_CONTEXT) context: Long
  ): SubscribeFacade
  def querySubscribes: QueryBuilder

  @FacadeChild
  def getNotify(id: Long): Option[NotifyFacade]
  def addNotify(init: NotifyFacade => Unit): NotifyFacade
  def queryNotifies: QueryBuilder
end NotificationParentFacade
