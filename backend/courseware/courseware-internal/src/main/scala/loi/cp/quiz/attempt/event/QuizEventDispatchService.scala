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

package loi.cp.quiz.attempt.event

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.eventing.{AbstractEventDispatchService, EventDispatchService, EventHandler}
import loi.cp.lwgrade.GradeService
import loi.cp.progress.LightweightProgressService
import loi.cp.quiz.attempt.{QuizActionParameters, QuizAttemptLoadService}
import loi.cp.user.UserService

@Service
trait QuizEventDispatchService extends EventDispatchService[QuizActionParameters, QuizAttemptEvent]

@Service
class QuizEventDispatchServiceImpl(
  gradeService: GradeService,
  progressService: LightweightProgressService,
  quizAttemptLoadService: QuizAttemptLoadService,
  userService: UserService,
) extends AbstractEventDispatchService[QuizActionParameters, QuizAttemptEvent]
    with QuizEventDispatchService:

  override protected def handlers: Seq[? <: EventHandler[QuizActionParameters, QuizAttemptEvent]] = Seq(
    GradebookScoringEventHandler(gradeService, quizAttemptLoadService, userService),
    new QuizProgressEventHandler(progressService, gradeService),
  )
end QuizEventDispatchServiceImpl
