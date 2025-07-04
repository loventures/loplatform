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

package loi.cp.submissionassessment.api

import java.time.Instant

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.util.FileInfo
import com.learningobjects.cpxp.util.Id.*
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.api.FeedbackRequest
import loi.cp.assessment.attempt.AttemptState
import loi.cp.assessment.{AttemptId, Feedback, ResponseScore}
import loi.cp.attachment.{AttachmentId, AttachmentInfo}
import loi.cp.context.ContextId
import loi.cp.course.right.{InteractCourseRight, ReadCourseRight, TeachCourseRight}
import loi.cp.reference.ContentIdentifier
import loi.cp.submissionassessment.attempt.SubmissionAttempt
import scalaz.\/
import scaloi.syntax.option.*

/** A web controller for serving and manipulating [[loi.cp.submissionassessment.attempt.SubmissionAttempt]] s.
  */
@Controller(root = true, value = "submissionAssessmentAttempt")
@RequestMapping(path = "submissionAssessmentAttempt")
trait SubmissionAssessmentAttemptWebController extends ApiRootComponent:

  /** Returns an attempt for the given persistence id.
    *
    * @param attemptId
    *   the persistence id
    * @return
    *   an attempt for the given persistence id
    */
  @RequestMapping(path = "{attemptId}", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[ReadCourseRight]))
  def getAttempt(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId,
    @MatrixParam(required = false) userId: Option[JLong] = None
  ): ErrorResponse \/ SubmissionAttemptDto

  /** Returns all the user's attempts against a given assessment.
    *
    * @param contentId
    *   the serialized [[loi.cp.reference.ContentIdentifier]] for the assessment
    * @return
    *   all user attempts against the assessment
    */
  @RequestMapping(method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[ReadCourseRight]))
  def getAttempts(
    @SecuredAdvice @MatrixParam("assessmentId") contentId: ContentIdentifier,
    @MatrixParam(required = false) userId: Option[JLong] = None
  ): ErrorResponse \/ Seq[SubmissionAttemptDto]

  /** Creates a new attempt against a submission assessment.
    *
    * @param request
    *   the request containing what assessment to create an attempt against.
    * @return
    *   the new attempt
    */
  @RequestMapping(method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def createAttempt(
    @RequestBody request: AttemptCreationRequest,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto

  /** Respond to an assessment in a given attempt. Can be used to either draft or save a response.
    *
    * @param attemptId
    *   the attempt being responded to
    * @param response
    *   the response, whether it is a draft
    * @param context
    *   the course containing the attempt
    * @return
    *   the updated submission assessment attempt
    */
  @RequestMapping(path = "{attemptId}", method = Method.POST)
  def respondToAttempt(
    @PathVariable("attemptId") attemptId: AttemptId,
    @RequestBody response: SubmissionResponseRequest,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto

  @RequestMapping(path = "{attemptId}/submit", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def submit(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto

  @RequestMapping(path = "{attemptId}/score", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def score(
    @PathVariable("attemptId") attemptId: AttemptId,
    @RequestBody request: ScoreRequest,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto

  @RequestMapping(path = "{attemptId}/invalidate", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def invalidateAttempt(
    @PathVariable("attemptId") attemptId: AttemptId,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ SubmissionAttemptDto

  @RequestMapping(path = "{attemptId}/feedback", method = Method.POST)
  def feedback(
    @PathVariable("attemptId") attemptId: AttemptId,
    @RequestBody feedback: ArgoBody[SubmissionFeedbackRequest],
    @SecuredAdvice @MatrixParam context: ContextId,
  ): ErrorResponse \/ SubmissionAttemptDto

  @RequestMapping(path = "{attemptId}/attachments/{attachmentId}", method = Method.GET)
  def attachment(
    @PathVariable("attemptId") attemptId: AttemptId,
    @PathVariable("attachmentId") attachmentId: AttachmentId,
    @QueryParam(value = "download", required = false) download: Option[JBoolean],
    @QueryParam(value = "direct", required = false) direct: Option[JBoolean],
    @QueryParam(value = "size", required = false) size: String,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ FileResponse[? <: FileInfo]

  @RequestMapping(path = "{attemptId}/attachments/{attachmentId}/url", method = Method.GET)
  def attachmentUrl(
    @PathVariable("attemptId") attemptId: AttemptId,
    @PathVariable("attachmentId") attachmentId: AttachmentId,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ErrorResponse \/ String
end SubmissionAssessmentAttemptWebController

case class AttemptCreationRequest(contentId: ContentIdentifier, subjectId: Option[JLong])

/** This is a payload expressing what needs to be added / kept in an essay response. 'submit' determines whether to
  * close out the attempt (submit=true), or save it for future (submit=false).
  *
  * In the case of files, uploads represent something that will be *added*, while attachments represent the things we
  * wish to still keep. For example, if we submit
  *
  * SubmissionResponseRequest(Some("text1", Seq(upload1, upload2), Seq(), false)
  *
  * we will have a response with TWO attachments. If we want to change the text, we use the returned ids and submit
  *
  * SubmissionResponseRequest(Some("text2", Seq(), Seq(attachment1, attachment2), false)
  *
  * this will update the TEXT ONLY and keep TWO attachments. If we then remove an attachment id
  *
  * SubmissionResponseRequest(Some("text2", Seq(), Seq(attachment2), false)
  *
  * we will retain the same text and have only ONE attachment left. Any number of attachments can be dropped, and at the
  * same time any new uploads can also be added.
  *
  * @param essay
  *   text provided in the essay text field, if any
  * @param uploads
  *   the new files to upload - attachments will be added
  * @param attachments
  *   attachment IDs that we still wish to keep
  * @param submit
  *   whether to submit (true = submit, false = save)
  */
case class SubmissionResponseRequest(
  essay: Option[String],
  uploads: Option[List[UploadInfo]],
  attachments: Option[List[AttachmentId]],
  submit: Boolean
)

final case class SubmissionFeedbackRequest(
  values: List[FeedbackRequest],
  submit: Boolean,
)
object SubmissionFeedbackRequest:
  implicit val codec: CodecJson[SubmissionFeedbackRequest] =
    CodecJson.derive[SubmissionFeedbackRequest]

case class SubmissionAttemptDto(
  id: AttemptId,
  subjectId: Long,
  essay: Option[String],
  attachments: Seq[AttachmentId],
  score: Option[ResponseScore],
  valid: Boolean,
  state: AttemptState,
  createTime: Instant,
  responseTime: Option[Instant],
  submitTime: Option[Instant],
  scoreTime: Option[Instant],
  scorer: Option[Long],
  feedback: Seq[Feedback],
  attachmentInfos: Map[Long, AttachmentInfo]
)

case class ScoreRequest(score: Option[ResponseScore], submit: Boolean)

object SubmissionAttemptDto:
  def apply(
    attempt: SubmissionAttempt,
    instructorLike: Boolean,
    alwaysReleaseFeedback: Boolean,
    attachments: Seq[AttachmentInfo]
  ): SubmissionAttemptDto =
    SubmissionAttemptDto(
      attempt.id,
      attempt.user.id,
      attempt.essay,
      attempt.attachments,
      attempt.score.when(instructorLike || attempt.state == AttemptState.Finalized),
      attempt.valid,
      attempt.state,
      attempt.createTime,
      attempt.responseTime,
      attempt.submitTime,
      attempt.scoreTime.when(instructorLike || attempt.state == AttemptState.Finalized),
      attempt.scorer.when(instructorLike || attempt.state == AttemptState.Finalized),
      Option(attempt.feedback).when(instructorLike || attempt.feedbackReleased || alwaysReleaseFeedback).getOrElse(Nil),
      attachments.byId
    )
end SubmissionAttemptDto
