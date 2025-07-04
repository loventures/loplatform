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

import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.notification.NotificationConstants.*

import scala.reflect.ClassTag

@FacadeItem(ITEM_TYPE_NOTIFICATION)
trait NotificationFacade extends Facade:
  @FacadeData(DATA_TYPE_NOTIFICATION_PROCESSED)
  def getProcessed: Option[Boolean]
  def setProcessed(processed: Boolean): Unit

  @FacadeData(DATA_TYPE_NOTIFICATION_TIME)
  def getTime: Date
  def setTime(time: Date): Unit

  @FacadeData(DATA_TYPE_NOTIFICATION_SENDER)
  def getSender: Option[Long]
  def setSender(sender: Option[Long]): Unit

  @FacadeData(DATA_TYPE_NOTIFICATION_CONTEXT)
  def getContext: Option[Long]
  def setContext(context: Option[Long]): Unit

  @FacadeData(DATA_TYPE_NOTIFICATION_TOPIC)
  def getTopic: Option[String]
  def setTopic(context: Option[String]): Unit

  /** deprecated */
  @FacadeData(DATA_TYPE_NOTIFICATION_TOPIC_ID)
  def getTopicId: Option[Long]

  @FacadeData(DATA_TYPE_NOTIFICATION_DATA)
  def getData[T](clas: Class[T]): T
  def getData[T: ClassTag]: T
  def setData[T](t: T): Unit

  def refresh(timeout: Long): Boolean
end NotificationFacade
