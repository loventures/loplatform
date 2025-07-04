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

package loi.cp.quiz
package attempt
package api

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.util.FileInfo
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.api.FeedbackRequest
import loi.cp.assessment.{AttemptId, ResponseScore}
import loi.cp.attachment.AttachmentId
import loi.cp.context.ContextId
import loi.cp.course.right.{InteractCourseRight, ReadCourseRight, TeachCourseRight}
import loi.cp.quiz.attempt.DistractorOrder.DisplayResponseSelection
import loi.cp.reference.{ContentIdentifier, EdgePath}
import scalaz.\/

import java.util.UUID
import scala.util.Try

/** A web controller for getting and creating quiz attempts.
  */
@Controller(root = true, value = "quizAttempt")
trait QuizAttemptWebController extends ApiRootComponent:
  import QuizAttemptWebController.*

  /** Returns an attempt for the given persistence id.
    *
    * @param attemptId
    *   the persistence id
    * @return
    *   an attempt for the given persistence id
    */
  @RequestMapping(path = "quizAttempt/{attemptId}", method = Method.GET)
  def getAttempt(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId,
    @MatrixParam(value = "userId", required = false) userId: Option[JLong] = None
  ): Try[QuizAttemptDto]

  /** Returns all the user's attempts against a given quiz.
    *
    * @param contentId
    *   the serialized [[loi.cp.reference.ContentIdentifier]] for the quiz
    * @return
    *   all user attempts against the quiz
    */
  @RequestMapping(path = "quizAttempt", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[ReadCourseRight]))
  def getAttempts(
    @SecuredAdvice @MatrixParam("quizId") contentId: ContentIdentifier,
    @MatrixParam(required = false) userId: Option[JLong] = None
  ): Try[Seq[QuizAttemptDto]]

  /** Creates a new attempt against a quiz.
    *
    * @param request
    *   the request containing what quiz to create an attempt against.
    * @return
    *   the new attempt
    */
  @RequestMapping(path = "quizAttempt", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def createAttempt(
    @RequestBody request: AttemptCreationRequest,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): Try[QuizAttemptDto]

  /** Respond to multiple questions.
    *
    * @param attemptId
    *   the attempt being responded to.
    * @param bulkResponse
    *   the response request.
    * @param context
    *   the course containing the attempt.
    * @return
    *   The updated quiz attempt.
    */
  @RequestMapping(path = "quizAttempt/{attemptId}", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def respond(
    @PathVariable("attemptId") attemptId: AttemptId,
    @RequestBody bulkResponse: RespondToAttemptDto,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): Try[QuizAttemptDto]

  /** Submits the given attempt. This will close any open responses. Redundant.
    *
    * @param attemptId
    *   the id of the attempt to close
    * @param context
    *   the course containing the attempt
    * @return
    *   the updated attempt
    */
  @RequestMapping(path = "quizAttempt/{attemptId}/submit", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def submit(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId,
  ): Try[QuizAttemptDto]

  /** Accepts testing out of a diagnostic, all test out gates will be processed. */
  @RequestMapping(path = "quizAttempt/{attemptId}/testOut", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def testOut(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): Try[List[EdgePath]]

  /** Issues feedback to a question response.
    *
    * @param attemptId
    *   the id of the attempt to issue feedback to
    * @param feedback
    *   the feedback to issue
    * @param context
    *   the course containing the attempt
    * @return
    *   the updated attempt
    */
  @RequestMapping(path = "quizAttempt/{attemptId}/feedback", method = Method.POST)
  def feedback(
    @PathVariable("attemptId") attemptId: AttemptId,
    @RequestBody feedback: ArgoBody[QuestionResponseFeedbackRequest],
    @SecuredAdvice @MatrixParam("context") context: ContextId,
  ): Try[QuizAttemptDto]

  /** Retrieves an attachment in a given attempt. The attachment may be from a response or feedback.
    *
    * @param attemptId
    *   the id of the attempt containing the attachment
    * @param attachmentId
    *   the id of the attachment
    * @param download
    *   whether the client requests to download the attachment
    * @param direct
    *   whether or not a CDN should be used
    * @param size
    *   the specified size, if any
    * @param context
    *   the course containing the attempt
    * @return
    */
  @RequestMapping(path = "quizAttempt/{attemptId}/attachments/{attachmentId}", method = Method.GET)
  def attachment(
    @PathVariable("attemptId") attemptId: AttemptId,
    @PathVariable("attachmentId") attachmentId: AttachmentId,
    @QueryParam(value = "download", required = false) download: Option[JBoolean],
    @QueryParam(value = "direct", required = false) direct: Option[JBoolean],
    @QueryParam(value = "size", required = false) size: String,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ FileResponse[? <: FileInfo]

  /** Invalidate a specified attempt. Only valid for instructors or as preview learner.
    *
    * @param attemptId
    *   The ID of the attempt being invalidated.
    * @param context
    *   The course the quiz belongs to.
    * @return
    *   The updated attempt data.
    */
  @RequestMapping(path = "quizAttempt/{attemptId}/invalidate", method = Method.POST)
  def invalidateAttempt(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): Try[QuizAttemptDto]

  /** Draft or submit a response score for a question.
    *
    * @param attemptId
    *   The ID of the attempt being scored.
    * @param context
    *   The course the quiz belongs to.
    * @param scoringRequest
    *   The score request data.
    * @return
    *   The scored attempt data.
    */
  @RequestMapping(path = "quizAttempt/{attemptId}/score", method = Method.POST)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight]))
  def scoreResponse(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId,
    @RequestBody scoringRequest: ResponseScoringRequest
  ): Try[QuizAttemptDto]
end QuizAttemptWebController

object QuizAttemptWebController:

  case class QuestionSelectionDto(
    questionIndex: Int,
    selection: Option[DisplayResponseSelection],
    attachments: Option[List[Long]],
    uploads: Option[List[UploadInfo]],
    submitResponse: Boolean
  )

  case class RespondToAttemptDto(
    responses: List[QuestionSelectionDto],
    submit: Option[Boolean] = None,
    autoSubmit: Option[Boolean] = None
  )

  /** A request object to create a new [[loi.cp.quiz.attempt.QuizAttempt]].
    *
    * @param contentId
    *   the content id of the quiz
    */
  case class AttemptCreationRequest(contentId: ContentIdentifier, competencies: Option[Set[UUID]] = None)

  /** A request object for all pieces of feedback for a single question response.
    *
    * @param questionIndex
    *   the question index to apply feedback to
    * @param values
    *   all feedback for the response
    * @param submit
    *   whether this is a draft or submission
    */
  final case class QuestionResponseFeedbackRequest(
    questionIndex: Int,
    values: List[FeedbackRequest],
    submit: Boolean,
  )
  object QuestionResponseFeedbackRequest:
    implicit val codec: CodecJson[QuestionResponseFeedbackRequest] =
      CodecJson.derive[QuestionResponseFeedbackRequest]

  /** A request object for drafting or submitting a score to a response.
    *
    * @param questionIndex
    *   The question index the score applies to.
    * @param score
    *   The score for the response.
    * @param submit
    *   Whether this response should be submitted or drafted.
    */
  case class ResponseScoringRequest(questionIndex: Int, score: ResponseScore, submit: Boolean)

  final case class SubmitRequest(
    autoSubmit: Option[Boolean]
  )

  object SubmitRequest:
    implicit val codec: CodecJson[SubmitRequest] = CodecJson.derive[SubmitRequest]
end QuizAttemptWebController
