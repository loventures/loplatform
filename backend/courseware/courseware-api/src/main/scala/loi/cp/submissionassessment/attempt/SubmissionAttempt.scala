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

package loi.cp.submissionassessment.attempt

import java.time.Instant
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.attempt.{AssessmentAttempt, AttemptState}
import loi.cp.assessment.{AttemptId, Feedback, ResponseScore}
import loi.cp.attachment.AttachmentId
import loi.cp.submissionassessment.SubmissionAssessment

/** A single attempt against a [[loi.cp.submissionassessment.SubmissionAssessment]].
  *
  * @param id
  *   the persistence id of this quiz
  * @param state
  *   the current state of the attempt
  * @param valid
  *   whether this attempt is still valid
  * @param createTime
  *   when this attempt was created
  * @param responseTime
  *   when the response was most recently set
  * @param submitTime
  *   when this attempt was submitted
  * @param scoreTime
  *   the last time the score was updated (this is the latest time of when the quiz is submitted or last manually
  *   graded)
  * @param score
  *   the overall score for this quiz (the semantic meaning of a score before it is submitted is undefined)
  * @param scorer
  *   the user who scored this attempt; if this attempt is scored
  * @param essay
  *   the user's essay response to the assessment, if any
  * @param attachments
  *   the attachment ids for this attempt
  * @param feedback
  *   the instructor feedback for the assessment
  * @param feedbackReleased
  *   whether the feedback is released to the subject
  * @param userId
  *   the subject of the attempt; the attempt may be driven by someone different than the subject
  * @param assessment
  *   the submission assessment this attempt is against
  * @param folderId
  *   the folder containing the attempt
  * @param rootId
  *   the domain of the attempt
  */
case class SubmissionAttempt(
  override val id: AttemptId,
  state: AttemptState,
  valid: Boolean,
  createTime: Instant,
  responseTime: Option[Instant],
  submitTime: Option[Instant],
  scoreTime: Option[Instant],
  score: Option[ResponseScore],
  scorer: Option[Long],
  essay: Option[String],
  attachments: Seq[AttachmentId],
  feedback: Seq[Feedback],
  feedbackReleased: Boolean,
  assessment: SubmissionAssessment,
  user: UserDTO,
  folderId: Long,
  rootId: Long
) extends AssessmentAttempt:
  override def getId: JLong             = id.value
  override def maxMinutes: Option[Long] = None
  override def autoSubmitted: Boolean   = false

  /** Synthetic field because times are top-level, and the sources are more limited than
    * [[loi.cp.quiz.attempt.QuizAttempt.updateTime]]
    */
  override val updateTime: Instant = (scoreTime.toSeq ++ submitTime.toSeq ++ responseTime.toSeq ++ Seq(createTime)).max
end SubmissionAttempt
