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

package loi.cp.message.notification

import java.lang.Long as JLong

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.HtmlUtils
import loi.cp.message.MessageStorageFacade
import loi.cp.notification.*
import scalaz.syntax.std.option.*

/** Notification associated with an internal message. */
@Schema("messageNotification")
trait MessageNotification extends Notification:
  type Init = MessageNotification.Init

  @JsonProperty
  def getSubject: String

  @JsonProperty("body")
  def getTextBody: String

object MessageNotification:

  /** Message notification initialization. */
  case class Init(
    storage: MessageStorageFacade,
    recipients: Seq[Long]
  )

/** Message notification implementation. */
@Component
class MessageNotificationImpl(
  val componentInstance: ComponentInstance,
  val self: NotificationFacade,
  implicit val fs: FacadeService
) extends MessageNotification
    with NotificationImplementation
    with ComponentImplementation:
  import MessageNotificationImpl.Data

  @PostCreate
  def init(init: Init): Unit =
    self.setSender(init.storage.getSender.some)
    self.setTime(init.storage.getTimestamp)
    self.setData(Data(init.storage.getId, init.recipients))

  override def getSubject: String = storage.getSubject

  override def audience: Seq[Long] = data.recipients

  override def getTextBody: String = HtmlUtils.toPlaintext(storage.getBody)

  override def aggregationKey: Option[String] = Some(s"$schemaName:${storage.getThread}")

  private lazy val data = self.getData(classOf[Data])

  private lazy val storage = data.storage.facade[MessageStorageFacade]
end MessageNotificationImpl

object MessageNotificationImpl:

  /** Message notification data storage. */
  case class Data(
    storage: Long,
    @JsonDeserialize(contentAs = classOf[JLong]) recipients: Seq[Long]
  )
