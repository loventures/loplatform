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

import loi.cp.quiz.attempt.{ConditionallyWrappedAction, QuizActionParameters}

case class ConditionallyScoreAttempt(action: ScoreAttempt) extends ConditionallyWrappedAction(action):
  // All responses have a submitted score /* and you made the deadline. */ else it goes in the queue.
  override def condition(parameters: QuizActionParameters): Boolean =
    parameters.attempt.responses.forall(_.state.scoreSubmitted)
  /* The original spec called for attempts to go into a grading queue. Now they get rejected
   * at the web controller layer so this is redundant.
      && parameters.attempt.maxMinutes.forall(limit =>
        parameters.attempt.submitTime.exists(submitted =>
          submitted.getEpochSecond - parameters.attempt.createTime.getEpochSecond <= limit * 60L
        )
      ) */

  override def targetClass: Class[ScoreAttempt] = classOf[ScoreAttempt]
end ConditionallyScoreAttempt

object ConditionallyScoreAttempt:
  def apply(): ConditionallyScoreAttempt =
    ConditionallyScoreAttempt(ScoreAttempt())
