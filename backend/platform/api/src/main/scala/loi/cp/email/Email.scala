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

package loi.cp.email

import java.util.Date
import javax.mail.internet.MimeMessage

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.RequestMapping
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.scala.util.Misc.ErrorMessage
import com.learningobjects.de.web.{Queryable, QueryableId}
import loi.cp.user.UserComponent

import scalaz.\/

/** An email component represents an email to be sent out by the system. Emails are typically associated with some
  * entity that triggered the creation of the email; for example, an internal message, and support processing emailed
  * replies.
  */
trait Email extends ComponentInterface with QueryableId:
  import Email.*

  /** Get the entity associated with the email.
    * @return
    *   the entity associated with the email
    */
  @JsonProperty(EntityProperty)
  @Queryable
  def getEntity: Option[Long]

  /** Get the body of the email. This may include additional information that cannot be derived from the entity that
    * triggered the email.
    * @return
    *   the configuration specified when the email was created
    */
  @JsonProperty
  def getBody: Option[Any]

  /** Get whether this email was sent successfully.
    * @return
    *   whether this email was sent successfully, or none if not yet attempted
    */
  @JsonProperty(SuccessProperty)
  @Queryable
  def getSuccess: Option[Boolean]

  /** Get when the email send was attempted.
    * @return
    *   when this email was attempted, or none if not yet attempted
    */
  @JsonProperty(SentProperty)
  @Queryable
  def getSent: Option[Date]

  /** Get whether replies are disallowed.
    * @return
    *   whether replies are disallowed
    */
  @JsonProperty
  def getNoReply: Boolean

  /** Set whether replies are disallowed.
    * @param noReply
    *   whether replies are disallowed.
    */
  def setNoReply(noReply: Boolean): Unit

  /** Get the user pk associated with this email.
    * @return
    *   the user pk
    */
  @JsonProperty(UserIdProperty)
  @Queryable
  def getUserId: Long

  /** Get the user associated with this email.
    * @return
    *   the user
    */
  @RequestMapping(path = UserProperty, method = Method.GET)
  @Queryable(joinComponent = classOf[UserComponent])
  def getUser: UserComponent

  /** Block replies to this.
    */
  @RequestMapping(path = "blockReplies", method = Method.POST)
  def blockReplies(): Unit

  /** Build an email message for sending.
    * @param email
    *   the email to configure
    */
  def buildEmail(email: MimeMessage): Unit

  /** Process a reply to this email.
    * @param message
    *   the reply message
    * @return
    *   the identifier of any item created by the reply process or an error. This is used to correlate In-Reply-To
    *   headers if there is a subsequent reply to that item.
    */
  def processReply(message: MimeMessage): ErrorMessage \/ Option[Long]
end Email

object Email:
  final val EntityProperty  = "entity"
  final val SuccessProperty = "success"
  final val SentProperty    = "sent"
  final val UserIdProperty  = "user_id"
  final val UserProperty    = "user"

  // how to nicely make the initialization type-dependent on the implementation?
  // dependent types for sure, but the question is .. how /nicely/?

  /** Email initialization.
    * @param entity
    *   the entity associated with the email
    * @param body
    *   the email body configuration
    */
  case class Init(entity: Option[Long], body: Option[Any])
end Email
