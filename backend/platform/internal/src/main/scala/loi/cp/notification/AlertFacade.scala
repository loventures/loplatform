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

@FacadeItem(ITEM_TYPE_ALERT)
trait AlertFacade extends Facade:
  @FacadeData(DATA_TYPE_ALERT_CONTEXT)
  def getContext: Option[Long]
  def setContext(context: Option[Long]): Unit

  @FacadeData(DATA_TYPE_ALERT_AGGREGATION_KEY)
  def getAggregationKey: String
  def setAggregationKey(aggregationKey: String): Unit

  @FacadeData(DATA_TYPE_ALERT_COUNT)
  def getCount: Long
  def setCount(count: Long): Unit

  @FacadeData(DATA_TYPE_ALERT_TIME)
  def getTime: Date
  def setTime(date: Date): Unit

  @FacadeData(DATA_TYPE_ALERT_NOTIFICATION)
  def getNotification: Notification
  def setNotification(notification: Notification): Unit

  @FacadeData(DATA_TYPE_ALERT_VIEWED)
  def getViewed: Boolean
  def setViewed(viewed: Boolean): Unit

  def refresh(pessimistic: Boolean): Unit
end AlertFacade
