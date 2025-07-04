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

package loi.cp.assessment.api

import com.learningobjects.cpxp.service.user.UserId
import loi.cp.assessment.attempt.AssessmentAttempt
import loi.cp.context.ContextId
import loi.cp.course.CourseAccessService
import scalaz.\/
import scaloi.syntax.boolean.*

/** Common validations for [[loi.cp.assessment.attempt.AssessmentAttempt]] s.
  */
object AttemptValidationUtils:
  def validateUserForAttempt(attempt: AssessmentAttempt, user: UserId): String \/ Unit =
    (attempt.user.id == user.id)
      .elseLeft(s"User ${user.id} cannot view attempt ${attempt.id}")

  def validateUserForAttemptOrInstructor(
    attempt: AssessmentAttempt,
    user: UserId,
    courseAccessService: CourseAccessService,
  ): String \/ Unit =
    validateUserForAttempt(attempt, user)
      .orElse(validateIsInstructorlike(attempt.contentId.contextId, courseAccessService))

  def validateAttemptInContext(attempt: AssessmentAttempt, context: ContextId): String \/ Unit =
    (attempt.contentId.contextId == context)
      .elseLeft(s"Attempt ${attempt.id} does not belong in context ${context.id}")

  def validateIsInstructorlike(contextId: ContextId, courseAccessService: CourseAccessService): String \/ Unit =
    courseAccessService
      .hasInstructorAccess(contextId)
      .elseLeft("You do not have permission to perform this action.")
end AttemptValidationUtils
