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
package event
import loi.cp.lwgrade.GradeService
import loi.cp.progress.{LightweightProgressService, ProgressChange}
import loi.cp.reference.EdgePath
import scaloi.syntax.disjunction.*

import scala.util.{Success, Try}

final class QuizProgressEventHandler(
  progressService: LightweightProgressService,
  gradeService: GradeService,
) extends QuizAttemptEventHandler:

  override def onEvent(params: QuizActionParameters, quizAttemptEvent: QuizAttemptEvent): Try[Unit] =
    quizAttemptEvent match
      case AttemptSubmittedEvent(false) => updateProgress(params, List(ProgressChange.visited))
      case AttemptFinalizedEvent(true)  => updateProgress(params, List(ProgressChange.visited))
      case AttemptFinalizedEvent(false) => updateProgress(params, Nil)
      case _                            => Success(())

  private def updateProgress(params: QuizActionParameters, changes: List[EdgePath => ProgressChange]): Try[Unit] =
    val gradebook = gradeService.getGradebook(params.quiz.section, params.attempt.user)
    progressService
      .updateProgress(params.quiz.section, params.attempt.user, gradebook, changes.map(_.apply(params.quiz.edgePath)))
      .toTry(e => new RuntimeException(e.msg))
      .map(_ => ())
end QuizProgressEventHandler
