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
import javax.mail.Message.RecipientType
import javax.mail.*
import javax.mail.internet.{InternetAddress, MimeMessage}

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentSupport}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import scaloi.syntax.AnyOps.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.DateOps.*
import com.learningobjects.cpxp.scala.util.Misc.ErrorMessage
import scaloi.syntax.OptionOps.*
import com.learningobjects.cpxp.schedule.Scheduled
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.EntityContext
import com.learningobjects.cpxp.util.EntityContextOps.*
import com.typesafe.config.Config
import de.tomcat.juli.LogMeta
import loi.cp.email.UnmarshalEmailSupport.*
import loi.cp.email.*
import loi.cp.user.UserComponent
import org.apache.commons.io.FileUtils
import scala.util.Using

import scala.concurrent.duration.*
import scalaz.\/
import scalaz.std.string.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*

/** Support for processing email replies.
  */
@Service
class ReplyServiceImpl(ec: => EntityContext)(implicit
  ce: ComponentEnvironment,
  dws: DomainWebService,
  es: EmailService,
  ew: EmailWorker,
  fs: FacadeService,
  config: Config
) extends ReplyService:
  import ReplyService.*
  import ReplyServiceImpl.*

  /** Tests whether the reply service is configured.
    */
  override def isConfigured: Boolean = ReplyConfiguration.get.isDefined

  /** Find the message id of a previous reply email from a particular user concerning a given entity.
    */
  override def findReplyMessageId(user: Long, entity: Long): Option[String] =
    user.facade[EmailParentFacade].findReplyByEntity(entity).flatMap(_.getMessageId)

  /** Find the email of a previous email to a particular user concerning a given entity.
    */
  override def findEmail(user: Long, entity: Long): Option[Long] =
    user.facade[EmailParentFacade].findEmailByEntity(entity).map(_.getId)

  /** Process a reply email or send a bounce to the sender.
    */
  override def processReply(message: MimeMessage, failure: Option[ErrorMessage])(now: Date): Unit =
    (failure <\/- processImpl(message)(now)) leftMap { m =>
      logger.warn(s"Messaging processing failure: ${m.message}")
      if (m != MessageReplayError) && (m != DevNullError) && (m != AutoReplyError)
      then // Send a bounce if failure occurred
        sendFailure(message, m)
    }

  /** TODO: KILL ME when we have a proper route to suspend emails via the recipient of spam from email. Look up an email
    * component from a reply address.
    */
  override def lookupEmail(address: String): Option[Email] =
    for
      recipientInfo <- EmailKeys.deobfuscate(address).map(ids => RecipientInfo(ids._1, ids._2)).toOption
      user          <- Option(recipientInfo.recipientId.facade[EmailParentFacade])
      email         <- user.getEmail(recipientInfo.emailId)
    yield email

  /** Schedule an email to be sent.
    */
  def scheduleEmail[A <: Email](user: UserComponent, impl: Class[A], init: Email.Init): Option[A] =
    for _ <- OptionNZ(user.getEmailAddress)
    yield
      val parent = user.facade[EmailParentFacade]
      parent.addEmail(impl, init) <| { email =>
        ec afterCommit {
          ew.offer(parent.getRootId, email.getId)
        }
      }

  /** Get the reply domain, if configured.
    */
  override def replyDomain: Option[String] = ReplyRootApiImpl.replyDomain

  /** Process a reply email.
    * @param message
    *   the email
    * @param now
    *   the current time
    * @return
    *   success or an error
    */
  private def processImpl(message: MimeMessage)(now: Date): ErrorMessage \/ ? =
    try
      for
        _             <- checkDevNull(message)
        recipientInfo <- decodeRecipientAddress(message)
        _              = logger.info(s"Recipient info: $recipientInfo")
        user          <- setupReplyContext(recipientInfo)
        _              = LogMeta.domain(user.getRootId)
        _              = LogMeta.user(user.getId)
        _              = logger.info(s"User: ${user.getEmailAddress.orNull}")
        email         <- getEmailComponent(user, recipientInfo) orElse getEmailFromMessageId(user, message)
        _              = logger.info(s"Email: ${email.getId}")
        _             <- checkAutoReply(message)
        _             <- checkCanReply(email)
        _             <- checkSentDate(email)(now)
        _             <- lookupReplay(user, message)
        entity        <- email.processReply(message)
      yield saveReply(user, message, entity)
    catch
      case e: Exception =>
        logger.warn(e)("Error processing reply")
        UnknownError.left

  /** Check whether this message should just be dropped. Messages to devnull@ are dropped, as are messages sent by the
    * platform itself.
    * @param message
    *   the message
    * @return
    *   success or an error
    */
  private def checkDevNull(message: MimeMessage): ErrorMessage \/ Unit =
    (sentByDe(message) || devNullRecipient(message).isDefined) `thenLeft` DevNullError

  /** Check whether this messages was sent by the platform.
    * @param message
    *   the message
    * @return
    *   whether it was sent by the platform
    */
  private def sentByDe(message: MimeMessage): Boolean =
    message.headerOpt(EmailService.X_SENT_BY).contains(EmailService.DIFFERENCE_ENGINE)

  /** Get any dev null recipient.
    * @param message
    *   the message
    * @return
    *   any recipient address that is devnull@
    */
  private def devNullRecipient(message: MimeMessage): Option[InternetAddress] =
    message.getAllRecipients.collectFirst {
      case a: InternetAddress if a.getAddress.startsWith(DevNullAt) => a
    }

  /** Check whether the email supports replies.
    * @param email
    *   the email
    * @return
    *   either success, if replies are supported, or an error
    */
  private def checkCanReply(email: Email): ErrorMessage \/ Unit =
    email.getNoReply `thenLeft` AddressSuspendedError

  /** Check whether the email is recent enough to allow replies.
    * @param email
    *   the email
    * @param now
    *   the rcurrent time
    * @return
    *   either the sent date, if recent enough, or an error
    */
  private def checkSentDate(email: Email)(now: Date): ErrorMessage \/ Date =
    email.getSent.filter(d => (now - d) < MaxReplyAge) \/> AddressExpiredError

  /** Check whether this message is an auto-reply.
    * @param message
    *   the message
    * @return
    *   success or an error
    */
  private def checkAutoReply(message: MimeMessage): ErrorMessage \/ Unit =
    (message.headerOpt(MessageHeaders.Precedence).contains(MessageHeaders.Precedence_AutoReply) ||
      message.headerOpt(MessageHeaders.X_Precedence).contains(MessageHeaders.Precedence_AutoReply) ||
      message.headerOpt(MessageHeaders.X_AutoRespond).isDefined ||
      (message.headerOpt(
        MessageHeaders.AutoSubmitted
      ) - MessageHeaders.AutoSubmitted_No).isDefined) `thenLeft` AutoReplyError

  /** Check whether this message is a re-delivery of an already-processed message.
    * @param user
    *   the recipient
    * @param message
    *   the message
    * @return
    *   success or an error
    */
  private def lookupReplay(user: EmailParentFacade, message: MimeMessage): ErrorMessage \/ Unit =
    for
      // does the message have a message id
      messageId <- Option(message.getMessageID).right
      // look up an existing stored reply from this user with this message id
      reply      = messageId flatMap user.findReplyByMessageId
      // if a reply was found, convert it to an error, else success
      replay    <- reply.map(_ => MessageReplayError) <\/ (())
    yield replay

  /** Information from a recipient address.
    * @param recipientId
    *   the recipient id
    * @param emailId
    *   the original email id (or, historically, the sender)
    */
  case class RecipientInfo(recipientId: Long, emailId: Long)

  /** Extracts the key part of a recipient address that matches a reply address created by the system.
    * @param message
    *   the message
    * @return
    *   a tuple of recipient id and sender id or an error
    */
  private def decodeRecipientAddress(message: Message): ErrorMessage \/ RecipientInfo =
    message.getAllRecipients
      .collectFirst {
        // If it matches one of our from addresses
        case PlusEmailAddress(_, plus, _) => plus
        case EmailAddress(name, _)        => name
      }
      .flatMap { key =>
        // Try to deobfuscate the identifiers
        EmailKeys.deobfuscate(key).map(ids => RecipientInfo(ids._1, ids._2)).toOption
      } \/> InvalidAddressError

  /** Look up the email for which a reply is being processed. Legacy emails include the email id in the message-id
    * (included in the in-reply-to header), contemporary emails include it in the email address.
    * @param user
    *   the current user
    * @param recipientInfo
    *   the recipient info from the to address
    * @return
    *   the email component or an error
    */
  private def getEmailComponent(user: EmailParentFacade, recipientInfo: RecipientInfo): ErrorMessage \/ Email =
    user.getEmail(recipientInfo.emailId) \/> InvalidAddressError

  /** Look up the email for which a reply is being processed from the legacy message id.
    * @param user
    *   the current user
    * @param message
    *   the message
    * @return
    *   the email component or an error
    */
  private def getEmailFromMessageId(user: EmailParentFacade, message: Message): ErrorMessage \/ Email =
    for
      messageIdInfo <- decodeMessageIdentifier(message)
      _              = logger.info(s"Message id info: $messageIdInfo")
      email         <- user.getEmail(messageIdInfo.emailId) \/> InvalidMessageIdError
    yield email

  /** Information from a message identifier.
    * @param hostName
    *   the hostname
    * @param emailId
    *   the email id
    * @param domainId
    *   the domain id
    */
  case class MessageIdInfo(hostName: String, emailId: Long, domainId: Long)

  /** Extracts the key part of an in-reply-to header or, if that is not present, the subject.
    *
    * Note that MUAs support for in-reply-to headers is unreliable so this processing only really exists for legacy
    * purposes.
    *
    * @param message
    *   the message
    * @return
    *   a triple of the domain name, email id and domain id or an error
    */
  private def decodeMessageIdentifier(message: Message): ErrorMessage \/ MessageIdInfo =
    decodeInReplyTo(message) \/> InvalidMessageIdError

  /** Extracts the key part of an in-reply-to header that matches a message identifier created by the system.
    * @param message
    *   the message
    * @return
    *   a triple of the domain name, email id and domain id
    */
  private def decodeInReplyTo(message: Message): Option[MessageIdInfo] =
    message
      .headerOpt(MessageHeaders.InReplyTo)
      .collectFirst { case MessageIdentifier(name, domain) =>
        (name, domain)
      }
      .flatMap { case (name, domain) =>
        // Try to deobfuscate the identifiers
        EmailKeys.deobfuscate(name).toOption.map(ids => MessageIdInfo(domain, ids._1, ids._2))
      }

  /** Validates that information from the recipient address is usable and sets up the component environment for the
    * user.
    * @param recipient
    *   the recipient address information
    * @return
    *   the user PK or an error
    */
  private def setupReplyContext(recipient: RecipientInfo): ErrorMessage \/ EmailParentFacade =
    (for
      // the recipient must be valid
      user <- Option(recipient.recipientId.facade[EmailParentFacade])
    yield
      // As the user who sent this email
      dws.setupUserContext(user.getId)
      user
    ) \/> InvalidAddressError

  /** Save the reply email for posterity.
    * @param user
    *   the recipient
    * @param message
    *   the message
    * @param entityId
    *   the entity created during processing
    */
  private def saveReply(user: EmailParentFacade, message: Message, entityId: Option[Long]): Unit =
    // Store metadata about the reply in the database
    val reply = user.addReply { r =>
      r.setEntity(entityId)
      r.setMessageId(message.headerOpt(MessageHeaders.MessageID))
      r.setDate(Option(message.getSentDate))
      r.setSender(Option(message.getFrom) flatMap { addresses =>
        addresses collectFirst { case ia: InternetAddress =>
          ia.getAddress
        }
      })
    }
    // Store raw email as an attachment
    Using.resource(UploadInfo.tempFile) { upload =>
      Using.resource(FileUtils.openOutputStream(upload.getFile))(message.writeTo)
      reply.addAttachment(upload)
      ()
    }
  end saveReply

  /** Send a failure report.
    * @param m
    *   the inbound message
    * @param e
    *   the error string
    */
  private def sendFailure(m: Message, e: ErrorMessage): Unit =
    es sendEmail { email =>
      m.replyTo foreach { from =>
        email.setRecipient(RecipientType.TO, from)
      }
      // arbitrary component until we rebuild i18n scoping
      implicit val cd    =
        ComponentSupport.getComponentDescriptor(classOf[ReplyRootApiImpl])
      val i18nParameters = List("message" -> m, "error" -> e.i18n)
      email.setFrom(m.getAllRecipients.head)
      email.setSubject(MessageDeliveryFailureSubject.i18n(i18nParameters))
      m.headerOpt(MessageHeaders.MessageID) foreach { mid =>
        email.addHeader(MessageHeaders.InReplyTo, mid)
      }
      email.setText(MessageDeliveryFailureBody.i18n(i18nParameters))
      email.setHeader(MessageHeaders.AutoSubmitted, MessageHeaders.AutoSubmitted_AutoReplied)
    }

  /** Keep the SQS queue alive by sending an email once a day.
    */
  @Scheduled(value = "01:23", singleton = true)
  @SuppressWarnings(Array("unused"))
  def keepQueueAlive(): Unit =
    ReplyConfiguration.get foreach { settings =>
      es.sendTextEmail(
        s"noone@${settings.domainName}",
        "No one",
        s"$DevNullAt${settings.domainName}",
        null,
        "Stay with me",
        "forever",
        false
      )
    }
end ReplyServiceImpl

/** Reply service singleton. */
object ReplyServiceImpl:

  /** The _logger. */
  private val logger = org.log4s.getLogger

  /** Address that drops messages. */
  private val DevNullAt = "devnull@"

  /** Don't support replies to messages over 180 days old. */
  private val MaxReplyAge = 180.days
end ReplyServiceImpl
