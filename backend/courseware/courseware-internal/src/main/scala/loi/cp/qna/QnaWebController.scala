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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.qna.{QnaMessageFinder, QnaQuestionFinder}
import com.learningobjects.cpxp.service.query.{Projection, QueryService, Function as QFunction}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import loi.cp.attachment.AttachmentComponent
import loi.cp.content.{ContentAccessService, CourseContentService, CourseWebUtils}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.{CourseAccessService, CourseEnrollmentService}
import loi.cp.notification.NotificationService
import loi.cp.reference.EdgePath
import loi.cp.user.UserService
import scalaz.std.anyVal.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.`class`.*
import scaloi.syntax.boolean.*
import scaloi.syntax.boxes.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.lang
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

//noinspection ScalaUnusedSymbol
@Component
@Controller(root = true)
class QnaWebController(
  val componentInstance: ComponentInstance,
  courseAccessService: CourseAccessService,
  courseWebUtils: CourseWebUtils,
  contentAccessService: ContentAccessService,
  contentService: CourseContentService,
  notificationService: NotificationService,
  courseEnrollmentService: CourseEnrollmentService,
  qnaService: QnaService,
  userService: UserService,
  cus: CurrentUrlService,
  user: UserDTO,
  domain: DomainDTO,
)(implicit is: ItemService, cs: ComponentService, ontology: Ontology, qs: QueryService)
    extends ApiRootComponent
    with ComponentImplementation:
  @DeIgnore
  protected def this() =
    this(null, null, null, null, null, null, null, null, null, null, null, null)(using null, null, null, null)

  @RequestMapping(path = "lwc/{context}/qna/{id}", method = Method.GET)
  def getQuestion(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
  ): Try[QnaQuestion] = for
    course   <- contentAccessService.getCourseAsLearner(context, user)
    question <- accessQuestion(course, id, forUpdate = false)
  yield
    val instructor = courseAccessService.hasInstructorAccess(course)
    val messages   = qnaService.getMessages(question :: Nil)
    QnaQuestion(
      question,
      qnaMessages(messages),
      Option(question.recipients).when(instructor).map(_.toList.finders[UserFinder].map(QnaProfile.apply))
    )

  @RequestMapping(path = "lwc/{context}/qna", method = Method.GET)
  def getQuestions(
    @PathVariable("context") context: Long,
    query: ApiQuery,
  ): Try[ApiQueryResults[QnaQuestion]] = for course <- contentAccessService.getCourseAsLearner(context, user)
  yield
    val instructor         = courseAccessService.hasInstructorAccess(course)
    val qb                 = qnaService.queryQuestions(course.id, !instructor)
    val questions          = ApiQueries.queryFinder[QnaQuestionFinder](qb, query)
    val messages           = qnaService.getMessages(questions.asScala.toSeq)
    val messageDtos        = qnaMessages(messages).groupUniqBy(_.id)
    val messagesByQuestion = messages.groupMap(_.parent.getId)(msg => messageDtos(msg.id))
    val recipientIds       = for
      q <- questions.asScala.toList if instructor && (q.recipients ne null)
      r <- q.recipients
    yield r
    val recipients         = recipientIds.finders[UserFinder].map(QnaProfile.apply).groupUniqBy(_.id)

    questions.map(question =>
      QnaQuestion(
        question,
        messagesByQuestion.getOrElse(question.id, Nil),
        Option(question.recipients).when(instructor).map(_.toList.unboxInside().flatMap(recipients.get))
      )
    )

  @RequestMapping(path = "lwc/{context}/qna/ids", method = Method.GET)
  def getQuestionIds(
    @PathVariable("context") context: Long,
    query: ApiQuery,
  ): Try[Seq[Long]] = for course <- contentAccessService.getCourseAsLearner(context, user)
  yield
    val instructor = courseAccessService.hasInstructorAccess(course)
    val qb         = qnaService.queryQuestions(course.id, !instructor)
    val questions  = ApiQueries.queryBuilder[QnaQuestionFinder](qb, query)
    questions.setProjection(Projection.ID).getValues[lang.Long].unboxInside()

  @RequestMapping(path = "lwc/{context}/qna/summary", method = Method.GET)
  def getQuestionSummary(
    @PathVariable("context") context: Long,
    query: ApiQuery,
  ): Try[Seq[QnaSummary]] = for course <- contentAccessService.getCourseAsLearner(context, user)
  yield
    val instructor = courseAccessService.hasInstructorAccess(course)
    val qb         = qnaService.queryQuestions(course.id, !instructor)
    ApiQueries
      .queryBuilder[QnaQuestionFinder](qb, query)
      .setGroup(QnaQuestionFinder.EdgePath, QnaQuestionFinder.Open, QnaQuestionFinder.Closed)
      .setFunction(QFunction.COUNT)
      .getValues[Array[AnyRef]]
      .collect({ case Array(count: lang.Long, pathStr: String, open: lang.Boolean, closed: lang.Boolean) =>
        val edgePath = EdgePath.parse(pathStr)
        val total    = count.longValue
        edgePath -> QnaSummary(edgePath, total, open ?? total, !(open || closed) ?? total)
      })
      .sumToMap
      .values
      .toSeq

  @RequestMapping(path = "lwc/{context}/qna", method = Method.POST)
  def addQuestion(
    @PathVariable("context") id: Long,
    @RequestBody dto: NewQuestion,
  ): Try[QnaQuestion] = for (course, content) <- contentAccessService.readContent(id, dto.edgePath, user)
  yield
    val question = qnaService.addQuestion(
      course.id,
      content.edgePath,
      dto.html,
      dto.attachments,
    )
    QnaQuestion(question, qnaMessages(question.thread :: Nil))

  // send a notification to many students. If they reply, the original notification message and the student's
  // reply become a qnathread
  @RequestMapping(path = "lwc/{context}/qna/multicast", method = Method.POST)
  def addMulticast(
    @PathVariable("context") contextId: Long,
    @RequestBody dto: NewInstructorMessage
  ): Try[QnaQuestion] = for
    section <- contentAccessService.getCourseAsInstructor(contextId, user)
    students = courseEnrollmentService.getEnrolledStudentIds(section.getId).toSet
    _       <- dto.recipients.forall(students.contains) <@~* new InvalidRequestException("id")
  yield
    val question = qnaService.sendMessage(
      section.id,
      dto.subject,
      dto.html,
      dto.attachments,
      dto.recipients,
    )
    QnaQuestion(question, qnaMessages(question.thread :: Nil))

  // convert an instructor message into a qnathread
  // front-end needs to replace its fake QnaQuestion with the real one this returns
  @RequestMapping(path = "lwc/{context}/qna/multicast/{id}/reply", method = Method.POST)
  def replyToMulticast(
    @PathVariable("context") contextId: Long,
    @PathVariable("id") id: Long,
    @RequestBody dto: NewMessage
  ): Try[QnaQuestion] = for
    section  <- contentAccessService.getCourseAsLearner(contextId, user)
    question <- accessQuestion(section, id, forUpdate = false)
    _        <- (question.recipients ne null) <@~* new InvalidRequestException("not an instructor message")
  yield
    val (q, msgs) = qnaService.addQuestionFromInstMsg(question, dto.html, dto.attachments)
    QnaQuestion(q, qnaMessages(msgs))

  // Only returns the new message
  @RequestMapping(path = "lwc/{context}/qna/{id}/message", method = Method.POST)
  def addMessage(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
    @RequestBody dto: NewMessage
  ): Try[QnaQuestion] = for
    course   <- contentAccessService.getCourseAsLearner(context, user)
    question <- accessQuestion(course, id, forUpdate = true)
  yield
    val message = qnaService.addMessage(question, dto.html, dto.attachments, dto.category, dto.subcategory)
    QnaQuestion(question, qnaMessages(message :: Nil))

  // Only returns the edited message
  @RequestMapping(path = "lwc/{context}/qna/{qid}/message/{mid}", method = Method.PUT)
  def editMessage(
    @PathVariable("context") context: Long,
    @PathVariable("qid") qid: Long,
    @PathVariable("mid") mid: Long,
    @RequestBody dto: EditMessage
  ): Try[QnaQuestion] = for
    course   <- contentAccessService.getCourseAsLearner(context, user)
    question <- accessQuestion(course, qid, forUpdate = true)
    message  <- mid.finder_?[QnaMessageFinder] <@~* new InvalidRequestException("Bad id")
    _        <- (message.parent.getId == question.id) <@~* new InvalidRequestException("Bad question")
    _        <- (message.creator.getId == user.id) <@~* new AccessForbiddenException()
  yield
    qnaService.editMessage(question, message, dto.html, dto.attachments)
    QnaQuestion(question, qnaMessages(message :: Nil))

  // Returns no messages
  @RequestMapping(path = "lwc/{context}/qna/{id}/recategorize", method = Method.POST)
  def recategorize(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
    @RequestBody dto: Recategorize
  ): Try[QnaQuestion] = for
    course   <- contentAccessService.getCourseAsLearner(context, user)
    question <- accessQuestion(course, id, forUpdate = true)
  yield
    qnaService.recategorizeQuestion(question, dto.category, dto.subcategory, close = false)
    QnaQuestion(question, Nil)

  // Returns no messages
  @RequestMapping(path = "lwc/{context}/qna/{id}/close", method = Method.POST)
  def closeQuestion(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
  ): Try[QnaQuestion] = for
    course   <- contentAccessService.getCourseAsLearner(context, user)
    question <- accessQuestion(course, id, forUpdate = true)
    _        <- (question.creator.getId == user.id) <@~* new AccessForbiddenException()
    _        <- !question.closed <@~* new InvalidRequestException("Already closed")
  yield
    qnaService.closeQuestion(question)
    QnaQuestion(question, Nil)

  @RequestMapping(path = "lwc/{context}/qna/{id}/instructorClose", method = Method.POST)
  def instructorClose(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
    @RequestBody dto: Recategorize
  ): Try[QnaQuestion] = for
    course   <- contentAccessService.getCourseAsInstructor(context, user)
    question <- accessQuestion(course, id, forUpdate = true)
  yield
    qnaService.recategorizeQuestion(question, dto.category, dto.subcategory, close = true)
    QnaQuestion(question, Nil)

  @RequestMapping(path = "lwc/{context}/qna/{qid}/message/{mid}/{aid}", method = Method.GET)
  def getAttachment(
    @PathVariable("context") context: Long,
    @PathVariable("qid") qid: Long,
    @PathVariable("mid") mid: Long,
    @PathVariable("aid") aid: Long,
    @QueryParam(value = "download", decodeAs = classOf[Boolean]) download: Option[Boolean] = None,
  ): Try[WebResponse] = for
    course   <- contentAccessService.getCourseAsLearner(context, user)
    question <- accessQuestion(course, qid, forUpdate = false)
    message  <- mid.finder_?[QnaMessageFinder] <@~* new InvalidRequestException("Bad id")
    _        <- (message.parent.getId == question.id) <@~* new InvalidRequestException("Bad question")
    _        <- message.attachments.contains(aid) <@~* new InvalidRequestException("Bad attachment")
  yield aid.component[AttachmentComponent].view(download.isTrue, false, null)

  @RequestMapping(path = "lwc/{context}/qna/{qid}/message/{mid}/{aid}/url", method = Method.GET)
  def getAttachmentUrl(
    @PathVariable("context") context: Long,
    @PathVariable("qid") qid: Long,
    @PathVariable("mid") mid: Long,
    @PathVariable("aid") aid: Long,
  ): Try[String] = for
    course    <- contentAccessService.getCourseAsLearner(context, user)
    question  <- accessQuestion(course, qid, forUpdate = false)
    message   <- mid.finder_?[QnaMessageFinder] <@~* new InvalidRequestException("Bad id")
    _         <- (message.parent.getId == question.id) <@~* new InvalidRequestException("Bad question")
    _         <- message.attachments.contains(aid) <@~* new InvalidRequestException("Bad attachment")
    attachment = aid.component[AttachmentComponent]
    response   = attachment.viewInternal(false, false, null, false, true)
    file      <- classOf[FileResponse.Any].option(response) <@~* new ResourceNotFoundException()
  yield file.fileInfo.getDirectUrl("GET", 1.hour.toMillis)

  private def qnaMessages(messages: List[QnaMessageFinder]): List[QnaMessage] =
    val attachments = messages.flatMap(_.attachments).finders[AttachmentFinder].groupMapUniq(_.id)(QnaAttachment.apply)
    messages.map(message => QnaMessage(message, message.attachments.toList.flatMap(attachments.get)))

  private def accessQuestion(course: LightweightCourse, id: Long, forUpdate: Boolean): Try[QnaQuestionFinder] = for
    question <- id.finder_?[QnaQuestionFinder] <@~* new InvalidRequestException("Bad id")
    _        <- checkQuestionAccess(question, course, forUpdate)
  yield question

  private def checkQuestionAccess(
    question: QnaQuestionFinder,
    course: LightweightCourse,
    forUpdate: Boolean
  ): Try[QnaQuestionFinder] = for
    _         <- (question.section.getId == course.id) <@~* new InvalidRequestException("Bad section")
    recipient  = Option(question.recipients).exists(r => r.isEmpty || r.contains(user.id))
    instructor = courseAccessService.hasInstructorAccess(course)
    _         <- (question.creator.getId == user.id || instructor || recipient) <@~* new AccessForbiddenException()
    _         <- !(forUpdate && question.closed) <@~* new InvalidRequestException("Question closed")
  yield question
end QnaWebController
