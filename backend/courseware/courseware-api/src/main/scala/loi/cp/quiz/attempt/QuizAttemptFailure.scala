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

package loi.cp.quiz.attempt

import com.learningobjects.cpxp.Id
import loi.cp.Widen
import loi.cp.assessment.rubric.RubricScoringFailure
import loi.cp.quiz.attempt.selection.QuestionResponseSelection
import loi.cp.quiz.question.Question

/** Any sort of failing a quiz attempt could have interacting with it.
  */
sealed abstract class QuizAttemptFailure(val message: String) extends Widen[QuizAttemptFailure]

////////////////////////////////////////////////

/** Any sort of failing resulting from user input for a response selection.
  */
sealed abstract class QuizAttemptSelectionFailure(message: String) extends QuizAttemptFailure(message)

/** An question/response index references a non-existent question position.
  *
  * @param requestedIndex
  *   the requested index
  * @param questionCount
  *   the number of questions in the attempt in question
  */
case class QuestionIndexOutOfBounds(requestedIndex: Int, questionCount: Int)
    extends QuizAttemptSelectionFailure(
      s"Question at $requestedIndex does not exist. There are $questionCount questions"
    )

/** A selection provided does not align with the question it was provided for (e.g. a free text response was somehow
  * given for a multiple choice question).
  *
  * @param question
  *   the question
  * @param selection
  *   the selection given for the question
  */
case class MismatchedResponseType(question: Question, selection: QuestionResponseSelection)
    extends QuizAttemptSelectionFailure(
      s"Unknown combination of Question ${question.getClass} and QuestionResponseSelection ${selection.getClass}"
    )

/** A selection provided has selected more options than allowed by the question type.
  *
  * @param maxSelectionsAllowed
  *   the maximum number of options allowed to be chosen by the question
  * @param selectionsMade
  *   the number of options selected by the user
  */
case class TooManySelections(maxSelectionsAllowed: Int, selectionsMade: Int)
    extends QuizAttemptSelectionFailure(s"$maxSelectionsAllowed selections allowed, but $selectionsMade were chosen")

/** A selection provided attempts to select an option that does not exist in the question (e.g. a user selected option 4
  * on a three choice question).
  */
object InvalidSelectionIndex
    extends QuizAttemptSelectionFailure("Selection must be within the bounds of the available choices")

/** A user attempts to upload a response attachment (not feedback) against a disallowed question type.
  */
case class IllegalUploadTarget(question: Question)
    extends QuizAttemptSelectionFailure(s"You may not upload attachments against ${question.getClass.getSimpleName}")

////////////////////////////////////////////////

/** Any sort of failing from submitting or making a selection disallowed by the current attempt/response state. (e.g.
  * changing a selection after an attempt has been submitted)
  */
sealed abstract class QuizAttemptResponseStateFailure(message: String) extends QuizAttemptFailure(message)

/** A user attempted to interact with a response that is closed to further modification by them.
  */
object ClosedResponseModification extends QuizAttemptResponseStateFailure("Cannot modify closed question response")

/** The user attempted to skip or bypass a question when the quiz settings disallows that.
  */
object NoSkippingAllowed extends QuizAttemptResponseStateFailure("Cannot skip questions")

/** A user attempted to modify a invalidated attempt.
  */
object InvalidatedAttemptModification extends QuizAttemptResponseStateFailure("Cannot modify invalidated attempt")

////////////////////////////////////////////////

/** Any sort of failing when scoring a response.
  */
sealed abstract class QuizAttemptScoringFailure(message: String) extends QuizAttemptFailure(message)

/** The user attempted to score a response, but didn't provide an actual score.
  */
case object MissingResponseScore extends QuizAttemptScoringFailure("Score required to submit score")

case class QuizRubricScoringFailure(rubricFailure: RubricScoringFailure)
    extends QuizAttemptScoringFailure(rubricFailure.message)

/** A user attempts to complete scoring for an attempt, but is missing one or more response scores.
  *
  * @param missingScoreIndices
  *   the response indices that do not have scores associated with them
  */
case class MissingResponseScoresFailure(missingScoreIndices: Seq[Int])
    extends QuizAttemptScoringFailure(s"Missing scores for responses at $missingScoreIndices")

/** A user attempted to score an attempt that is invalidated.
  */
case object InvalidatedAttemptScoringFailure extends QuizAttemptScoringFailure("Cannot modify invalidated attempt")

////////////////////////////////////////////////

/** Any sort of failing reopening an attempt.
  */
sealed abstract class ReopeningAttemptFailure(message: String) extends QuizAttemptFailure(message)

/** A user attempted to reopen an attempt that is not finalized.
  *
  * @param attempt
  *   the attempt in question
  */
case class NotFinalizedFailure(attempt: Id)
    extends ReopeningAttemptFailure(s"Attempt ${attempt.getId} not finalized. Cannot reopen.")

/** A user attempted to reopen an attempt that is invalid.
  *
  * @param attempt
  *   the attempt in question
  */
case class InvalidatedAttemptReopeningFailure(attempt: Id)
    extends ReopeningAttemptFailure(s"Attempt ${attempt.getId} is invalidated. Cannot reopen.")

////////////////////////////////////////////////

/** A user attempted to invalidate an already invalid attempt.
  *
  * @param attempt
  *   the attempt in question
  */
case class AlreadyInvalidFailure(attempt: Id)
    extends QuizAttemptFailure(s"Attempt ${attempt.getId} already invalidated. Cannot invalidate again.")

/** A user attempted to start an attempt, but had already reached their maximum number of attempts.
  *
  * @param maxAttempts
  *   the maximum number of the attempts for the assessment
  */
case class AttemptLimitExceeded(maxAttempts: Int)
    extends QuizAttemptFailure(s"No more attempts allowed. Max attempts = $maxAttempts")

/** A user attempted to test out of an unscored attempt.
  *
  * @param attempt
  *   the attempt in question
  */
case class NotYetScored(attempt: Id)
    extends QuizAttemptFailure(s"Attempt ${attempt.getId} has not yet received a score.")
