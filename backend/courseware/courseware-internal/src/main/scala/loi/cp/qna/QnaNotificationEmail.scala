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

package loi.cp.qna

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.{Component, Schema}
import com.learningobjects.cpxp.component.{ComponentDescriptor, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.service.attachment.{AttachmentFacade, AttachmentWebService}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.service.qna.{QnaMessageFinder, QnaQuestionFinder}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.content.CourseWebUtils
import loi.cp.email.{AttachmentAttacher, Email, EmailFacade}
import loi.cp.notification.{AbstractNotificationEmail, NotificationService}
import loi.cp.reply.ReplyService
import scaloi.misc.TimeSource

import javax.mail.internet.MimeMultipart
import scala.xml.Node

@Schema("qnaNotificationEmail")
trait QnaNotificationEmail extends Email

@Component
class QnaNotificationEmailImpl(
  val self: EmailFacade,
  val domain: DomainDTO,
  val componentInstance: ComponentInstance,
  courseWebUtils: CourseWebUtils,
  qnaService: QnaService,
  val replyService: ReplyService,
  val ns: NotificationService,
  val userDTO: UserDTO,
  val timeSource: TimeSource,
)(implicit
  cs: ComponentService,
  is: ItemService,
  val fs: FacadeService,
  mapper: ObjectMapper,
  aws: AttachmentWebService,
  mws: MimeWebService
) extends AbstractNotificationEmail[QnaNotification]
    with QnaNotificationEmail:
  import AbstractNotificationEmail.UnparsedStringOps
  import QnaNotificationEmailImpl.*

//  Deliberately not supporting replies for now because a "thanks" would re-open the question.
//
//  override def processReply(email: MimeMessage): ErrorMessage \/ Option[Long] =
//    \/.attempt {
//      qnaService.addMessage(question, replyContent(email)).id.longValue.some
//    } { e =>
//      logger.warn(e)("Error processing discussion reply")
//      InvalidDiscussionReply
//    }

  override protected def addAttachments(part: MimeMultipart): Unit =
    val attacher: AttachmentAttacher = new AttachmentAttacher()
    message.attachments foreach { att =>
      part.addBodyPart(attacher.attachmentPart(att.facade[AttachmentFacade]))
    }

  // will have domain, user, course from super
  override protected def getBindings: Map[String, Any] =
    super.getBindings ++ Map(
      "created" -> initial.created,
      "replied" -> message.created
    )

  override protected def parts(bindings: Map[String, Any])(implicit cd: ComponentDescriptor): Seq[Node] =
    Seq(
      Intro.i18n(bindings).u,
      <p>{message.message.u}</p>,
      Prefix.i18n(bindings).u,
      <blockquote><p>{initial.message.u}</p></blockquote>,
      Outro.i18n(bindings).u
    )

  override protected def subject: I18nMessage = Subject

  private lazy val question: QnaQuestionFinder = notification.questionId.finder[QnaQuestionFinder]
  private lazy val message: QnaMessageFinder   = notification.messageId.finder[QnaMessageFinder]
  private lazy val initial: QnaMessageFinder   = qnaService.getFirstMessage(question)
end QnaNotificationEmailImpl

object QnaNotificationEmailImpl:
  private final val logger = org.log4s.getLogger

  val MAX_TITLE_LENGTH = 60

  private final val Subject = I18nMessage(
    "QnaNotificationEmail_Subject",
    """Re: Your question in {course.name}"""
  )

  private final val Intro = I18nMessage(
    "QnaNotificationEmail_Intro_html",
    """<p>Hi {user.givenName}, you have an answer to your question in {course.name}:</p>"""
  )

  private final val Prefix = I18nMessage(
    "QnaNotificationEmail_Prefix_html",
    """<div>On {created,time,d MMM yyyy 'at' hh:mm aaa z} you wrote:</div>"""
  )

  private final val Outro = I18nMessage(
    "QnaNotificationEmail_Outro_html",
    """<p>Please do not reply to this message. To ask another question,
      |please log into your course and follow up in the Q&A section.</p>""".stripMargin
  )
end QnaNotificationEmailImpl
