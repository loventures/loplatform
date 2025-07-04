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
import com.learningobjects.cpxp.service.notification.NotifyFinder

@FacadeItem(NotifyFinder.ITEM_TYPE_NOTIFY)
trait NotifyFacade extends Facade:
  @FacadeData(NotifyFinder.DATA_TYPE_NOTIFY_TIME)
  def getTime: Date
  def setTime(time: Date): Unit

  @FacadeData(NotifyFinder.DATA_TYPE_NOTIFY_NOTIFICATION)
  def getNotification: Option[Notification]
  def setNotification(event: Notification): Unit
