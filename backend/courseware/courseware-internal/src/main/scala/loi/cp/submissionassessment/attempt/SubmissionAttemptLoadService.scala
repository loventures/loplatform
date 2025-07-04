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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.course.CourseSection
import loi.cp.reference.EdgePath
import loi.cp.submissionassessment.SubmissionAssessment
import loi.cp.submissionassessment.attempt.SubmissionAssessmentAttemptOps.*
import loi.cp.submissionassessment.persistence.SubmissionAttemptDao
import scaloi.syntax.collection.*

// a second SubmissionAttemptService to break a cycle between the stupid SubmissionAssessmentEventHandlers
// and SubmissionAttemptService
@Service
class SubmissionAttemptLoadService(submissionAttemptDao: SubmissionAttemptDao):

  def countValidAttempts(course: CourseSection, submissionAssessment: SubmissionAssessment, userId: UserId): Int =
    submissionAttemptDao.countValidAttempts(course.id, submissionAssessment.edgePath, userId.value)

  def getUserAttempts(
    course: CourseSection,
    assessments: Seq[SubmissionAssessment],
    user: UserDTO
  ): Seq[SubmissionAttempt] =
    val edgePaths = assessments.validateContextOrThrow(course)
    val entities  =
      submissionAttemptDao.getUserAttempts(course.id, edgePaths, user.id)

    val quizByPath = assessments.groupUniqBy(_.edgePath)
    entities.map(e =>
      val quiz = quizByPath(EdgePath.parse(e.edgePath))
      e.toAttempt(quiz, user)
    )
  end getUserAttempts
end SubmissionAttemptLoadService
