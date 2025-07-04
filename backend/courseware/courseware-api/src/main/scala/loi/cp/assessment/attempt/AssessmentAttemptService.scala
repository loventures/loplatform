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

package loi.cp.assessment.attempt

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.assessment.Assessment
import loi.cp.assessment.InstructorOverviews.{InstructorAttemptsOverview, UserGradingOverview}
import loi.cp.assessment.LearnerOverviews.LearnerAttemptOverview
import loi.cp.content.CourseContents
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import scalaz.NonEmptyList

/** A service for getting generalized attempts against any type of [[Assessment]].
  */
@Service
trait AssessmentAttemptService:

  /** Returns all attempts against the given [[Assessment]] s.
    *
    * @param course
    *   the course containing the assessments
    * @param assessments
    *   the assessments to get attempts for
    * @return
    *   all [[AssessmentAttempt]] from the given [[Assessment]] s, grouped by assessment
    */
  def getAttempts(course: CourseSection, assessments: Seq[Assessment]): Map[Assessment, Seq[AssessmentAttempt]]

  def countValidAttempts(course: CourseSection, assessments: Seq[Assessment]): Int

  def countValidAttempts(course: CourseSection, assessment: Assessment, userId: UserId): Int

  /** Gets aggregate attempt data for grading the given assessments for all learners.
    *
    * @param course
    *   the course containing the assessments
    * @param assessments
    *   the assessments to aggregate over
    * @return
    *   the assessment aggregation data for all users
    */
  def getParticipationData(course: CourseSection, assessments: Seq[Assessment]): Seq[AssessmentParticipationData]

  /** Counts open and valid attempts by user for all assessments specified by the given content ids.
    *
    * @param course
    *   the course containing the assessments
    * @param assessments
    *   the assessments to aggregate over
    * @param user
    *   the subject of the attempts
    * @return
    *   an overview of attempts by state for all given assessments for the given user
    */
  def getLearnerAttemptOverviews(
    course: CourseSection,
    assessments: Seq[Assessment],
    user: UserId
  ): Seq[LearnerAttemptOverview]

  /** Gets attempt counts by users grouped by state for the given assessments.
    *
    * @param course
    *   the course containing the assessments
    * @param assessments
    *   the assessments to aggregate over
    * @return
    *   the attempt overviews
    */
  def getInstructorAttemptsOverviews(
    course: CourseSection,
    assessments: Seq[Assessment]
  ): Seq[InstructorAttemptsOverview]

  /** Calculates information for overviews for grading pages for a given set of attempts.
    *
    * @param courseId
    *   the context id of the course containing the assessments
    * @param contents
    *   the contents of the `course`
    * @param attemptsByAssessment
    *   the attempts to aggregate into overviews
    * @param users
    *   the subjects of the attempts
    * @return
    *   a map of assessment ids to a collection of overviews for the users
    */
  def getGradingOverviews(
    courseId: ContextId,
    contents: CourseContents,
    attemptsByAssessment: Map[Assessment, Seq[AssessmentAttempt]],
    users: NonEmptyList[UserDTO]
  ): Seq[UserGradingOverview]
end AssessmentAttemptService
