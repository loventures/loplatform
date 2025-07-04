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

import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.event.QuizAttemptEvent
import loi.cp.quiz.question.Question
import scalaz.syntax.either.*
import scalaz.{\/, \/-}

import java.time.Instant
import scala.annotation.tailrec

/** An action that may be performed against an attempt, returning the attempt and an indication of failure or success.
  */
trait QuizAttemptAction:

  /** Executes this action and returns the state of the resulting attempt. This does not persist any effects appiled to
    * the attempt.
    *
    * @param parameters
    *   the parameters for this action
    * @return
    *   the updated attempt
    */
  def exec(parameters: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt

  /** Returns all events that should be raised when this action executes successfully.
    *
    * @param params
    *   the parameters to evaluate what events are raised
    * @return
    *   all events spawned from this action
    */
  def events(params: QuizActionParameters): Seq[QuizAttemptEvent]

  /** Creates a compound action of this action followed by a second action.
    *
    * @param rhs
    *   the action to execute with the output of this action
    * @return
    */
  def andThen(rhs: QuizAttemptAction): QuizAttemptAction =
    (this, rhs) match
      case (AggregateAttemptAction(leftActions), AggregateAttemptAction(rightActions)) =>
        AggregateAttemptAction(leftActions ++ rightActions)
      case (AggregateAttemptAction(leftActions), _)                                    => AggregateAttemptAction(leftActions :+ rhs)
      case (_, AggregateAttemptAction(rightActions))                                   => AggregateAttemptAction(this +: rightActions)
      case _                                                                           => AggregateAttemptAction(Seq(this, rhs))
end QuizAttemptAction

/** A object containing all information for a [[QuizAttemptAction]] to execute.
  *
  * @param attempt
  *   the attempt to act upon
  * @param quiz
  *   the quiz for this particular attempt
  * @param attemptQuestions
  *   the questions for this particular attempt
  */
case class QuizActionParameters(attempt: QuizAttempt, quiz: Quiz, attemptQuestions: Seq[Question], time: Instant)

/** An action that groups a collection of actions together, to be performed in sequence.
  *
  * @param actions
  *   The actions that will be performed.
  */
case class AggregateAttemptAction(actions: Seq[QuizAttemptAction]) extends QuizAttemptAction:

  def exec(params: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    @tailrec def run(
      currAttempt: QuizAttemptFailure \/ QuizAttempt,
      remainingActions: List[QuizAttemptAction]
    ): QuizAttemptFailure \/ QuizAttempt =
      currAttempt match
        case \/-(att) =>
          remainingActions match
            case a :: as => run(a.exec(params.copy(attempt = att)), as)
            case Nil     => currAttempt
        case f        => f

    run(params.attempt.right, actions.toList)
  end exec

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] =
    actions.flatMap(_.events(params))
end AggregateAttemptAction

trait ConditionalAttemptAction[A <: QuizAttemptAction] extends QuizAttemptAction:

  def condition(parameters: QuizActionParameters): Boolean

  def action(parameters: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt

  override def exec(parameters: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    if condition(parameters) then action(parameters)
    else parameters.attempt.right

  def targetClass: Class[A]
end ConditionalAttemptAction

abstract class ConditionallyWrappedAction[A <: QuizAttemptAction](wrappedAction: A) extends ConditionalAttemptAction[A]:
  override def action(parameters: QuizActionParameters): QuizAttemptFailure \/ QuizAttempt =
    wrappedAction.exec(parameters)

  override def events(params: QuizActionParameters): Seq[QuizAttemptEvent] =
    if condition(params) then wrappedAction.events(params)
    else Nil
