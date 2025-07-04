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

import java.lang.Long as jLong
import java.util.Date

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}

@Component
class AlertImpl(
  val componentInstance: ComponentInstance,
  self: AlertFacade
) extends Alert
    with ComponentImplementation:
  override def getId: jLong = self.getId

  override def delete(): Unit = self.delete()

  override def getContextId: Option[Long] = self.getContext

  override def getCount: Long = self.getCount

  override def getTime: Date = self.getTime

  override def getAggregationKey: String = self.getAggregationKey

  override def getNotification: Notification = self.getNotification

  override def isViewed: Boolean = self.getViewed

  override def view(): Unit = self.setViewed(true)
end AlertImpl
