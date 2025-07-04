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

package loi.cp.reply

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.{Method, WebResponse}
import com.learningobjects.de.web.{Queryable, QueryableId}

/** Representation of an email reply receipt. Exists for the purpose of debugging email issues. This is the read sibling
  * of Reply.
  */
@Schema("replyReceipt")
trait ReplyReceipt extends ComponentInterface with QueryableId:
  import ReplyReceipt.*

  @JsonProperty(SenderProperty)
  @Queryable
  def getSender: Option[String]

  @JsonProperty(DateProperty)
  @Queryable
  def getDate: Option[Date]

  @JsonProperty(MessageIdProperty)
  @Queryable
  def getMessageId: Option[String]

  @JsonProperty(EntityProperty)
  @Queryable
  def getEntity: Option[Long]

  @JsonProperty
  def getSubject: String

  @JsonProperty
  def getBody: String

  @RequestMapping(path = "download", method = Method.GET, csrf = false)
  def download: WebResponse
end ReplyReceipt

object ReplyReceipt:
  final val SenderProperty    = "sender"
  final val DateProperty      = "date"
  final val MessageIdProperty = "messageId"
  final val EntityProperty    = "entity"
