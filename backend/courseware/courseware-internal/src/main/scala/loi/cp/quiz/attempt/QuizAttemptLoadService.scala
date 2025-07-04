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

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.quiz.persistence.QuizAttemptDao
import QuizAttemptOps.*
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.course.CourseSection
import loi.cp.quiz.Quiz
import loi.cp.reference.EdgePath
import scaloi.syntax.collection.*

// a second QuizAttemptService to break a cycle between the stupid QuizAttemptEventHandlers
// and QuizAttemptService
@Service
class QuizAttemptLoadService(
  quizAttemptDao: QuizAttemptDao
):

  def getUserAttempts(course: CourseSection, quizzes: Seq[Quiz], user: UserDTO): Seq[QuizAttempt] =
    val edgePaths = quizzes.validateContextOrThrow(course)
    val entities  = quizAttemptDao.getUserAttempts(course.id, edgePaths, user.id)

    val quizByPath = quizzes.groupUniqBy(_.edgePath)
    entities.map(e =>
      val quiz = quizByPath(EdgePath.parse(e.edgePath))
      e.toAttempt(quiz, user)
    )

  def countValidAttempts(course: CourseSection, quiz: Quiz, userId: UserId): Int =
    quizAttemptDao.countValidAttempts(course.id, quiz.edgePath, userId.value)
end QuizAttemptLoadService
