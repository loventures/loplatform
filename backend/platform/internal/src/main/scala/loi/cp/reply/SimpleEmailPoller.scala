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

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.scala.cpxp.Service.*
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.scala.util.Misc.{ErrorMessage, ErrorString, now}
import com.learningobjects.cpxp.service.attachment.{AttachmentProvider, AttachmentService}
import com.learningobjects.cpxp.service.script.ScriptService
import com.learningobjects.cpxp.util.ManagedUtils
import com.typesafe.config.Config
import de.tomcat.juli.LogMeta
import loi.cp.email.{MarshalEmailSupport, UnmarshalEmailSupport}
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.DisjunctionOps.*
import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, Message as SqsMessage}
import software.amazon.awssdk.services.sqs.SqsClient

import javax.mail.Address
import javax.mail.internet.MimeMessage
import scala.annotation.meta.field
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.control.NonFatal

/** AWS Simple Email Service poller. After due configuration, when SES receives inbound mail it delivers the MIME
  * message to S3 and then sends a notification to SNS which delivers that notification to SQS. This poller receives the
  * SQS notification, downloads the email and processes it accordingly.
  */
@Service(unique = true)
class SimpleEmailPoller(implicit as: AttachmentService, sm: ServiceMeta, config: Config):

  import SimpleEmailPoller.*

  private val state = ReplyConfiguration.get map { config =>
    val sqs = newSqsClient(config)
    sqs -> new Thread(() => poller(using sqs, config), "SimpleEmailPoller")
  }

  /** Whether this worker is stopped. */
  @(SingletonState @field)
  private var stopped = false

  /** Start this email poller.
    */
  @PostLoad
  @SuppressWarnings(Array("unused"))
  private def start(): Unit =
    logger.info("Scheduling poller")
    // start the polling loop
    state foreach { s =>
      s._2.start()
    }

  /** Shut down this email poller.
    */
  @PreUnload
  @PreShutdown
  @SuppressWarnings(Array("unused"))
  def shutdown(): Unit =
    logger.info("Shutting down poller")
    stopped = true
    state foreach:
      case (sqs, poller) =>
        poller.interrupt()
        poller.join(PollInterval.toMillis)
        sqs.close()
    logger.info("Poller shut down")
  end shutdown

  /** Poll for SQS messages and process them as they arrive.
    */
  private def poller(implicit sqs: SqsClient, config: ReplyConfiguration): Unit =
    while !stopped do
      treither {
        logger.info(s"Polling for SQS messages: ${config.queueUrl}")
        while !stopped do
          // if i am currently the DAS then poll
          if sm.isDas then
            logger.debug("SQS receive message")
            sqs.receiveMessage(receiveMessageRequest).messages.asScala foreach processSqsMessage
          LogMeta.clear()
          Thread.sleep(PollInterval.toMillis)
        logger.info("SQS poller shut down")
      } -<| { e =>
        logger.warn(e)("SQs poller error")
      }

  /** Create an SQS receive message request. This is configured to long-poll for messages.
    * @return
    *   the receive message request
    */
  private def receiveMessageRequest(implicit config: ReplyConfiguration): ReceiveMessageRequest =
    ReceiveMessageRequest
      .builder()
      .queueUrl(config.queueUrl)
      .maxNumberOfMessages(MaxMessages)
      .waitTimeSeconds(PollInterval.toSeconds.toInt)
      .build()

  /** Process an SQS message notification and then delete it.
    * @param message
    *   the SQS message
    */
  private def processSqsMessage(message: SqsMessage)(implicit sqs: SqsClient, config: ReplyConfiguration): Unit =
    logger.info(s"Processing SQS message ${message.messageId}")
    try
      processSqsMessageImpl(message) leftMap { m =>
        logger.warn(s"Error processing SQS message ${message.messageId}: $m")
      }
    catch
      case NonFatal(e) =>
        logger.warn(e)(s"Error processing SQS message ${message.messageId}")
    logger.debug(s"Deleting SQS message ${message.messageId}")
    sqs.deleteMessage(
      DeleteMessageRequest.builder().queueUrl(config.queueUrl).receiptHandle(message.receiptHandle).build()
    )
  end processSqsMessage

  /** Parse an SQS message and pass the MIME email into the reply processor.
    * @param message
    *   the SQS message
    * @return
    *   success or a failure message
    */
  private def processSqsMessageImpl(message: SqsMessage): ErrorString \/ ? =
    for
      sns          <- parseSnsMessage(message)
      _             = logger.info(s"SNS message: $sns")
      notification <- parseSnsNotification(sns)
      _             = logger.info(s"SNS notification: $notification")
      received     <- parseSesReceivedNotification(notification)
      _             = logger.info(s"SES received: $received")
      email        <- downloadEmail(received) // what about a transient S3 error...
      _             = LogMeta.put("from", addressesOf(email.getFrom))
      _             =
        logger.info(
          s"Email downloaded: ${addressesOf(email.getFrom)} -> ${addressesOf(email.getAllRecipients)}: ${email.getSubject} / ${bodyOf(email)}"
        )
    // failures up to this point are infrastructural issues and can't effectively trigger a bounce email
    // without refactoring around bouncing against the SES receipt headers
    yield ManagedUtils perform { () =>
      service[ScriptService].initComponentEnvironment()
      service[ReplyService].processReply(email, receiptError(received.receipt))(now)
    }

  /** Get a stringified list ef email addresses.
    * @param addresses
    *   the nullable addresses
    * @return
    *   the string
    */
  private def addressesOf(addresses: Array[Address]): String =
    Option(addresses).fold("")(_.mkString(", "))

  /** Get a summary of the body of an email.
    * @param email
    *   the email
    * @return
    *   part of the body
    */
  private def bodyOf(email: MimeMessage): String =
    UnmarshalEmailSupport.htmlContent(email).take(255)

  /** Identify any rejection reason from the SES receipt.
    * @param receipt
    *   the SES receipt
    * @return
    *   the error, if any
    */
  def receiptError(receipt: SesReceipt): Option[ErrorMessage] =
    receipt.virusVerdict.exists(_.status != SesVerdict.Pass).option(VirusScanError)

  /** Parse the SNS message from an SQS message.
    * @param message
    *   the SQS message
    * @return
    *   the SNS message or an error
    */
  def parseSnsMessage(message: SqsMessage): ErrorString \/ SnsMessage =
    ComponentUtils.fromJson(message.body, classOf[SnsMessage]).right

  /** Parse the SNS notification from an SNS message.
    * @param message
    *   the SNS message
    * @return
    *   the SNS notification or an error
    */
  def parseSnsNotification(message: SnsMessage): ErrorString \/ SnsNotification =
    if message.messageType != NotificationMessageType then s"Unknown SNS message type: ${message.messageType}".left
    else ComponentUtils.fromJson(message.message, classOf[SnsNotification]).right

  /** The SES notification SNS message type. */
  val NotificationMessageType = "Notification"

  /** Parse the SES received notification from an SNS notification.
    * @param notification
    *   the SNS notification
    * @return
    *   the SES received notification or an error
    */
  def parseSesReceivedNotification(notification: SnsNotification): ErrorString \/ SesReceivedNotification =
    notification match
      case received: SesReceivedNotification        => received.right
      case SnsUnknownNotification(notificationType) =>
        s"Unknown SES notification type: $notificationType".left

  /** Download the MIME message from an SES received notification.
    * @param received
    *   the received notification
    * @return
    *   the MIME message or an error
    */
  def downloadEmail(received: SesReceivedNotification): ErrorString \/ MimeMessage =
    for
      s3       <- parseS3Action(received)
      provider <- attachmentProvider(s3.bucketName)
    yield Using.resource(provider.blobStore.getBlob(s3.bucketName, s3.objectKey).getPayload.openStream) { in =>
      new MimeMessage(MarshalEmailSupport.mailSession, in)
    }

  /** Parse the S3 action from an SES received notification.
    * @param received
    *   the received notification
    * @return
    *   the S3 action or an error
    */
  def parseS3Action(received: SesReceivedNotification): ErrorString \/ S3Action =
    received.receipt.action match
      case s3: S3Action                 => s3.right
      case SesUnknownAction(actionType) =>
        s"Unknown SES action type: $actionType".left

  /** Get the attachment provider that supports a particular S3 bucket.
    * @param bucket
    *   the S3 bucket name
    * @return
    *   the attachment provider or an error
    */
  def attachmentProvider(bucket: String): ErrorString \/ AttachmentProvider =
    as.getAttachmentProviders.asScala.find(_.container == bucket) \/> s"Unknown S3 bucket: $bucket"
end SimpleEmailPoller

/** Simple email poller singleton.
  */
object SimpleEmailPoller:

  /** Create a new SQS client.
    * @param settings
    *   the reply settings
    * @return
    *   a sew SQS client
    */
  private def newSqsClient(settings: ReplyConfiguration): SqsClient =
    SqsClient
      .builder()
      .credentialsProvider(
        AwsCredentialsProviderChain.of(
          ProfileCredentialsProvider.create(),
          InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(true).build(),
          StaticCredentialsProvider.create(AwsBasicCredentials.create(settings.accessKey, settings.secretKey))
        )
      )
      .build

  // sqs.setRegion(Region.getRegion(Regions.US_WEST_2))???

  /** The _logger. */
  private val logger = org.log4s.getLogger

  /** Max messages to retrieve. */
  private val MaxMessages = 10

  /** Poll wait time. */
  private val PollInterval = 20.seconds

  val VirusScanError = I18nMessage("Virus scanner rejected the email.")
end SimpleEmailPoller
