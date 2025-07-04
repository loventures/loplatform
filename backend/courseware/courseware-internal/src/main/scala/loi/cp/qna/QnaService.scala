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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.qna.{QnaMessageFinder, QnaQuestionFinder}
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.service.session.SessionDTO
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.HtmlUtils
import jakarta.persistence.LockTimeoutException
import loi.cp.admin.FolderParentFacade
import loi.cp.analytics.AnalyticsService
import loi.cp.analytics.event.QnaThreadPutEvent1
import loi.cp.notification.NotificationService
import loi.cp.reference.EdgePath
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.collection.*

import java.util.UUID
import scala.jdk.OptionConverters.*

@Service
class QnaService(implicit
  fs: FacadeService,
  is: ItemService,
  qs: QueryService,
  ns: NotificationService,
  aws: AttachmentWebService,
  as: AnalyticsService,
  ontology: Ontology,
  now: => TimeSource,
  user: => UserDTO,
  domain: => DomainDTO,
  session: => SessionDTO,
):
  import QnaService.*

  def addQuestion(
    sectionId: Long,
    edgePath: EdgePath,
    html: String,
    attachments: List[UploadInfo],
  ): QnaQuestionFinder =
    val section  = sectionId.finder[GroupFinder]
    val folder   = section.facade[FolderParentFacade].getOrCreateFolderByType(QnaFolderType)
    val question = folder.addChild[QnaQuestionFinder] { question =>
      question.section = section
      question.edgePath = edgePath.toString
      question.created = now.date
      question.creator = user.finder[UserFinder]
      question.modified = now.date
      question.open = true
      question.closed = false
    }
    question.thread = addMessageFinder(question, html, attachments)
    as.emitEvent(analyticsEvent(question))
    question
  end addQuestion

  def sendMessage(
    sectionId: Long,
    subject: String,
    html: String,
    attachments: List[UploadInfo],
    recipients: List[Long],
  ): QnaQuestionFinder =
    val section  = sectionId.finder[GroupFinder]
    val folder   = section.facade[FolderParentFacade].getOrCreateFolderByType(QnaFolderType)
    val question = folder.addChild[QnaQuestionFinder] { question =>
      question.section = section
      question.edgePath = EdgePath.Root.toString
      question.created = now.date
      question.creator = user.finder[UserFinder]
      question.modified = now.date
      question.recipients = recipients.map(long2Long).toArray
      question.subject = subject
      question.open = false
      question.closed = true
    }
    question.thread = addMessageFinder(question, html, attachments, Some(subject))

    ns.nοtify[InstructorMessageSentNotification](question.getId, InstructorMessageSentNotificationInit(question))

    // no analytics for instructor message
    question
  end sendMessage

  def addQuestionFromInstMsg(
    instMsg: QnaQuestionFinder,
    html: String,
    attachments: List[UploadInfo]
  ): (QnaQuestionFinder, List[QnaMessageFinder]) =
    val folder   = instMsg.section.facade[FolderParentFacade].getOrCreateFolderByType(QnaFolderType)
    val question = folder.addChild[QnaQuestionFinder] { question =>
      question.section = instMsg.section
      question.edgePath = instMsg.edgePath
      question.created = now.date
      question.creator = user.finder[UserFinder]
      question.modified = now.date
      question.subject = instMsg.subject
      question.replyTo = instMsg
      question.open = true
      question.closed = false
    }

    val msg1 = question.addChild[QnaMessageFinder] { message =>
      message.section = question.section
      message.created = instMsg.created
      message.creator = instMsg.creator
      message.edited = null
      message.message = instMsg.thread.message
      message.attachments = instMsg.thread.attachments
      message.search = Option(instMsg.subject).cata(_.concat(" "), "") + HtmlUtils.toPlaintext(message.message)
    }

    question.thread = addMessageFinder(question, html, attachments)
    (question, List(msg1, question.thread))
  end addQuestionFromInstMsg

  def addMessage(
    question: QnaQuestionFinder,
    html: String,
    attachments: List[UploadInfo],
    category: Option[String],
    subcategory: Option[String],
  ): QnaMessageFinder =
    lockAndCheckNotClosed(question)
    val message = addMessageFinder(question, html, attachments)
    question.modified = now.date
    if user.id == question.creator.getId then
      // student post
      if !question.open then
        question.open = true
        question.thread = message
        as.emitEvent(analyticsEvent(question))
    else
      // instructor post
      if question.open then
        question.open = false
        question.category = category.orNull
        question.subcategory = subcategory.orNull
        as.emitEvent(analyticsEvent(question, instructorReply = Some(message)))
      // For now, we only notify students when an instructor replies, presuming that instructors do not
      // care for real-time notification of new questions.
      ns.nοtify[QnaNotification](question.getId, QnaNotificationInit(question, message))
    end if
    message
  end addMessage

  def recategorizeQuestion(
    question: QnaQuestionFinder,
    category: Option[String],
    subcategory: Option[String],
    close: Boolean,
  ): Unit =
    if user.id == question.creator.getId then throw new IllegalStateException("Not instructor")
    lockAndCheckNotClosed(question)
    question.modified = now.date
    question.category = category.orNull
    question.subcategory = subcategory.orNull
    if close then
      question.closed = true
      question.open = false
    as.emitEvent(analyticsEvent(question, instructorClosed = close))
  end recategorizeQuestion

  // BE WARNED: Adding message deletion comes with complexity in the analytics so be wary

  def editMessage(
    question: QnaQuestionFinder,
    message: QnaMessageFinder,
    html: String,
    attachments: List[Long],
  ): Unit =
    lockAndCheckNotClosed(question)
    question.modified = now.date
    message.edited = now.date
    message.message = html
    message.attachments = message.attachments.intersect(attachments)
    message.search = HtmlUtils.toPlaintext(html)
  end editMessage

  def closeQuestion(
    question: QnaQuestionFinder,
  ): Unit =
    if user.id != question.creator.getId then throw new IllegalStateException("Not creator")
    lockAndCheckNotClosed(question)
    question.modified = now.date
    question.closed = true
    if question.open then
      question.open = false
      // we only emit a student-closed analytics event if the student closed an open question
      // without awaiting an instructor reply.
      as.emitEvent(analyticsEvent(question, studentClosed = true))
  end closeQuestion

  private def lockAndCheckNotClosed(question: QnaQuestionFinder): Unit =
    if !is.lock(question.owner, true, true, None.toJava) then throw new LockTimeoutException()
    if question.closed then throw new IllegalStateException("Question closed")

  private def addMessageFinder(
    question: QnaQuestionFinder,
    html: String,
    uploads: List[UploadInfo],
    subject: Option[String] = None,
  ): QnaMessageFinder =
    val attachments = uploads.map(upload => aws.createAttachment(question.id, upload))
    question.addChild[QnaMessageFinder] { message =>
      message.section = question.section
      message.created = now.date
      message.creator = user.finder[UserFinder]
      message.edited = null
      message.message = html
      message.attachments = attachments.toArray
      message.search = subject.cata(_.concat(" "), "") + HtmlUtils.toPlaintext(html)
    }
  end addMessageFinder

  private def analyticsEvent(
    question: QnaQuestionFinder,
    instructorReply: Option[QnaMessageFinder] = None,
    studentClosed: Boolean = false,
    instructorClosed: Boolean = false,
  ): QnaThreadPutEvent1 =
    QnaThreadPutEvent1(
      id = UUID.randomUUID(),
      session = Option(session.id),
      source = domain.hostName,
      time = now.date,
      threadId = question.thread.getId,
      questionId = question.getId,
      userId = question.creator.getId,
      actualUserId = None,
      sectionId = question.section.getId,
      edgePath = question.edgePath,
      // assetId = 0L,
      createTime = question.thread.created.toInstant,
      instructorReplyUserId = instructorReply.map(_.creator.getId),
      instructorReplyTime = instructorReply.map(_.created.toInstant),
      studentClosed = studentClosed, // student force-closed the question (not in the UI)
      instructorClosed = instructorClosed,
      category = Option(question.category),
      subcategory = Option(question.subcategory),
    )

  def queryQuestions(
    sectionId: Long,
    mine: Boolean,
  ): QueryBuilder =
    domain
      .queryAll[QnaQuestionFinder]
      .addCondition(QnaQuestionFinder.Section, Comparison.eq, sectionId)
      .addConjunction(mine.option(BaseCondition.getInstance(QnaQuestionFinder.Creator, Comparison.eq, user.id)))

  def getMessages(questions: Seq[QnaQuestionFinder]): List[QnaMessageFinder] = questions ?? {
    domain
      .queryAll[QnaMessageFinder]
      .addCondition(DataTypes.META_DATA_TYPE_PARENT_ID, Comparison.in, questions.map(_.id))
      .setOrder(QnaMessageFinder.Created, Direction.ASC)
      .getFinders[QnaMessageFinder]
      .toList
  }

  def getFirstMessage(question: QnaQuestionFinder): QnaMessageFinder =
    question
      .queryChildren[QnaMessageFinder]
      .setOrder(QnaMessageFinder.Created, Direction.ASC)
      .setLimit(1)
      .getFinder[QnaMessageFinder]
      .get
end QnaService

object QnaService:
  final val QnaFolderType = "qna"
