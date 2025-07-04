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

package loi.cp.quiz.api
import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.course.CourseSection
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.quiz.Quiz
import loi.cp.reference.EdgePath
import scalaz.\/
import scalaz.syntax.std.option.*

/** A utility for loading quiz content with less ceremony.
  */
@Service
class QuizWebUtils(
  courseWebUtils: CourseWebUtils,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
):

  /** Loads a course and quiz for read access.
    *
    * @param edgePath
    *   the location of the quiz
    * @return
    *   the course and quiz, if it could be loaded
    */
  def readQuiz(
    contextId: ContextId,
    edgePath: EdgePath,
    rights: Option[CourseRights],
  ): String \/ (CourseSection, Quiz) =
    for
      section <- courseWebUtils.loadCourseSection(contextId.id, rights)
      quiz    <- readQuiz(section, edgePath)
    yield (section, quiz)

  /** Loads a quiz for read access.
    *
    * @param section
    *   the course section
    * @param edgePath
    *   the location of the quiz
    * @return
    *   the course and quiz, if it could be loaded
    */
  def readQuiz(section: CourseSection, edgePath: EdgePath): String \/ Quiz =
    for
      content                 <- section.contents.get(edgePath).toRightDisjunction(s"No such quiz: $edgePath in ${section.id}")
      courseAssessmentPolicies = courseAssessmentPoliciesService.getPolicies(section)
      quiz                    <- Quiz
                                   .fromContent(content, section, courseAssessmentPolicies)
                                   .toRightDisjunction(s"No quiz found for $edgePath in ${section.id}")
    yield quiz
end QuizWebUtils
