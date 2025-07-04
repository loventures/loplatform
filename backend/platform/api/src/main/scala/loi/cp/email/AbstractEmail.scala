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

import java.lang.Long as JLong
import java.util.Date
import javax.mail.Message
import javax.mail.internet.MimeMessage
import com.learningobjects.cpxp.component.annotation.PostCreate
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInterface, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.email.UnmarshalEmailSupport.{plaintextContent, stripReplyEmailAddresses}
import loi.cp.localmail.LocalmailWebController
import loi.cp.reply.ReplyService
import loi.cp.user.UserComponent
import org.apache.commons.text.StringEscapeUtils
import scaloi.syntax.option.*

import scala.reflect.ClassTag
import scalaz.syntax.std.boolean.*

import java.nio.charset.StandardCharsets

/** Base class for email component implementations
  * @tparam A
  *   the component type of the entity associated with this email
  */
abstract class AbstractEmail[A <: ComponentInterface: ClassTag](implicit cs: ComponentService)
    extends Email
    with ComponentImplementation:

  private final val logger = org.log4s.getLogger

  val self: EmailFacade
  val replyService: ReplyService
  val bodyClass: Class[?]

  /** Initializing the email.
    * @param init
    *   the email initialization
    */
  @PostCreate
  private def create(init: Email.Init): Unit =
    self.setEntity(init.entity)
    self.setBody(init.body)

  /** Get the id of this email.
    * @return
    *   the id of this email
    */
  override def getId: JLong = componentInstance.getId

  /** Get the entity associated with the email.
    * @return
    *   the entity associated with the email
    */
  override def getEntity: Option[Long] = self.getEntity

  /** Get the entity associated with this email as a component.
    * @return
    *   the entity associated with this email
    */
  protected def getEntityComponent(implicit fs: FacadeService): Option[A] =
    getEntity.map(_.component[A])

  /** Get the email body configuration.
    * @return
    *   the configuration specified when the email was cereahed
    */
  override def getBody: Option[Any] =
    self.getBody(bodyClass)

  /** Get whether this email was sent successfully.
    * @return
    *   whether this email was sent successfully, or none if not yet attempted
    */
  override def getSuccess: Option[Boolean] = self.getSuccess

  /** Get when this email was sent.
    * @return
    *   when this email was attempted, or none if not yet attempted
    */
  override def getSent: Option[Date] = self.getSent

  /** Get whether replies are disallowed.
    * @return
    *   whether replies are disallowed
    */
  override def getNoReply: Boolean = self.getNoReply.isTrue

  /** Set whether replies are disallowed.
    * @param noReply
    *   whether replies are disallowed
    */
  override def setNoReply(noReply: Boolean): Unit = self.setNoReply(noReply)

  /** Block replies to this.
    */
  override def blockReplies(): Unit = setNoReply(true)

  /** Get the user pk associated with this email.
    * @return
    *   the user pk
    */
  def getUserId: Long = self.getParentId.longValue

  /** Get the user associated with this email.
    * @return
    *   the user
    */
  override def getUser: UserComponent = self.getParentId.component[UserComponent]

  /** Add subject, date, to and from addresses to the email.
    * @param email
    *   the email to configure
    * @param senderName
    *   the sender name
    * @param subject
    *   the subject line
    * @param date
    *   the sent date
    */
  protected def initEmail(email: MimeMessage, senderName: String, subject: String, date: Date): Unit =
    email.setSubject(subject, StandardCharsets.UTF_8.name)
    email.setSentDate(date)
    val recipient   = getUser
    // for testing purposes, if delivering to localmail then always generate a valid reply address
    val isLocalmail =
      recipient.getEmailAddress.endsWith(LocalmailWebController.AtLocalmail)
    val replyDomain = replyService.replyDomain.orElse(isLocalmail.option(LocalmailWebController.Domain))
    email.setFrom(MarshalEmailSupport.replyAddress(senderName, recipient.getId, self.getId, replyDomain))
    email.addRecipient(
      Message.RecipientType.TO,
      MarshalEmailSupport.toAddress(recipient.getFullName, recipient.getEmailAddress)
    )
    email.addHeader("Precedence", "bulk")
  end initEmail

  /** Add an In-Reply-To header if this email is a response to an email the recipient has already received. This allows
    * the mail user agent to track email threads correctly.
    * @param email
    *   the email to configure
    * @param replyEntity
    *   if this mail is potentially a reply to an email from the recipient, the entity key by which to look up the
    *   user's reply email
    * @param emailEntity
    *   if this mail is potentially a reply to an entity that has already been emailed to the user, the entity key by
    *   which to look up the email
    */
  protected def addInReplyTo(email: MimeMessage, replyEntity: Option[Long], emailEntity: => Option[Long]): Unit =
    replyEntity flatMap { irt =>
      // search for the message id of an email from this user concerning the reply entity
      replyService.findReplyMessageId(getUserId, irt)
    } orElse {
      emailEntity flatMap { eid =>
        // otherwise search for an email to this user concerting the email entity
        replyService.findEmail(getUserId, eid)
      } map { email =>
        // reproduce the message id from that email
        MarshalEmailSupport.domainMessageId(email)
      }
    } foreach { messageId =>
      // add a corresponding in-reply-to header
      email.addHeader(MessageHeaders.InReplyTo, messageId)
    }

  /** Returns the visible parts of the email as HTML. */
  protected def replyContent(email: MimeMessage): String =
    EmailReply(plainText(email)).sections
      .filterNot(_.hidden)
      .map(
        _.lines.map(StringEscapeUtils.escapeHtml4).mkString("<br />")
      )
      .mkString("<p>", "</p><p>", "</p>")

  /** Returns the plaintext of the email with sensitive email addresses stripped. */
  private def plainText(email: MimeMessage): String =
    stripReplyEmailAddresses(plaintextContent(email), replyService.replyDomain)
end AbstractEmail
