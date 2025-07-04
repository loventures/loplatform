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

package loi.cp.quiz.attempt.actions

import loi.cp.attachment.AttachmentId
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.event.QuizAttemptEvent
import loi.cp.quiz.question.Question
import loi.cp.quiz.question.essay.Essay
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.syntax.boolean.*

/** An action to set all attachments for a response. This does not preserve any of the prior attachments that were
  * associated with the response.
  *
  * @param questionIndex
  *   the question the attachments are
  * @param attachments
  *   the attachment for the response
  */
case class SetResponseAttachments(questionIndex: Int, attachments: Seq[AttachmentId]) extends QuizAttemptAction:
  import AttemptResponseUtils.*

  override def exec(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    val attempt: QuizAttempt = params.attempt

    for
      _ <- attempt.valid.elseLeft(InvalidatedAttemptModification).widenl
      _ <- validateResponseIndex(params.attemptQuestions, questionIndex).widenl
      _ <- validateResponseIsOpen(attempt.responses(questionIndex)).widenl
      _ <- validateEssayQuestionOrEmptyAttachments(params.attemptQuestions(questionIndex)).widenl
    yield updateResponse(attempt.copy(updateTime = params.time), questionIndex) { response =>
      response.copy(attachments = attachments)
    }
  end exec

  def validateEssayQuestionOrEmptyAttachments(question: Question): IllegalUploadTarget \/ Unit =
    question match
      case Essay(_, _, _, _, _, _, _)          => ().right
      case q: Question if attachments.nonEmpty => IllegalUploadTarget(q).left
      case _                                   => ().right

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] = Nil
end SetResponseAttachments
