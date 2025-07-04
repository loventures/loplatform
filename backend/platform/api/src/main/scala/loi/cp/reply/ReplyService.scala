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
import javax.mail.internet.MimeMessage

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.scala.util.Misc.ErrorMessage
import loi.cp.email.Email
import loi.cp.user.UserComponent

/** Support for processing email replies.
  */
@Service
trait ReplyService:

  /** Tests whether the reply service is configured.
    * @return
    *   whether the reply service is configured
    */
  def isConfigured: Boolean

  /** Find the message id of a previous reply email from a particular user concerning a given entity.
    * @param user
    *   the user
    * @param entity
    *   the entity
    * @return
    *   the message identifier of a previous email from this user concerning this entity
    */
  def findReplyMessageId(user: Long, entity: Long): Option[String]

  /** Find the email of a previous email to a particular user concerning a given entity.
    * @param user
    *   the user
    * @param entity
    *   the entity
    * @return
    *   the email to this user concerning this entity
    */
  def findEmail(user: Long, entity: Long): Option[Long]

  /** Process a reply email or send a bounce to the sender.
    * @param message
    *   the email
    * @param failure
    *   an existing error condition
    * @param now
    *   the current time
    */
  def processReply(message: MimeMessage, failure: Option[ErrorMessage])(now: Date): Unit

  /** TODO: KILL ME when we have a proper route to suspend emails via the recipient of spam from email. Look up an email
    * component from a reply address.
    * @param address
    *   the reply address
    * @return
    *   the email, if found
    */
  def lookupEmail(address: String): Option[Email]

  /** Schedule an email to be sent.
    *
    * @param user
    *   the user to whom the email should be sent
    * @param impl
    *   the email type
    * @param init
    *   the email initialization data
    * @tparam A
    *   the email type
    * @return
    *   the email, if scheduled
    */
  def scheduleEmail[A <: Email](user: UserComponent, impl: Class[A], init: Email.Init): Option[A]

  /** Get the reply domain, if configured.
    * @return
    *   the configured reply domain
    */
  def replyDomain: Option[String]
end ReplyService

object ReplyService:

  /* These don't necessarily belong outside internal, but they are here for the
   * benefit of the message email integration test. If the notification vertical
   * sinks into platform these can probably return to the implementation companion.
   */

  val MessageDeliveryFailureSubject = I18nMessage("Message delivery failure: {message.subject}")
  val MessageDeliveryFailureBody    = I18nMessage("Your message could not be delivered: {error}")

  val InvalidAddressError   = I18nMessage("No valid recipient address was found.")
  val InvalidMessageIdError = I18nMessage("This does not appear to be a reply to an email sent from this system.")
  val UnknownError          = I18nMessage("An error occurred while processing your reply.")
  val MessageReplayError    = I18nMessage("Message replay detected.")
  val AddressSuspendedError = I18nMessage("This reply address has been suspended.")
  val AddressExpiredError   = I18nMessage("This reply address has expired.")
  val DevNullError          = I18nMessage(
    "Dev null."
  ) // This is internal. Suggests that I want to pass around something other than a string
  val AutoReplyError = I18nMessage(
    "Ignoring auto-reply."
  ) // This is internal. Suggests that I want to pass around something other than a string
end ReplyService
