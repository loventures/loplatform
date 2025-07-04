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

package loi.cp.submissionassessment

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.CourseAssessmentPolicy
import loi.cp.content.CourseContent
import loi.cp.course.CourseSection

/** A service to gets submission assessments from a context.
  */
@Service
trait SubmissionAssessmentService:

  /** Creates submission assessments for the given content. If content does not represent a submission assessment, then
    * no result is returned.
    *
    * @param course
    *   the course containing the contents
    * @param content
    *   the content to derive assessments from
    * @return
    *   all submission assessments found in the given content
    */
  def getSubmissionAssessment(
    course: CourseSection,
    content: CourseContent,
    policies: List[CourseAssessmentPolicy],
    ws: AttachedReadWorkspace,
  ): Option[SubmissionAssessment] =
    getSubmissionAssessments(course, Seq(content), policies, ws).headOption

  /** Creates submission assessments for the given content. If content does not represent a submission assessment, then
    * no result is returned.
    *
    * @param course
    *   the course containing the contents
    * @param contents
    *   the content to derive assessments from
    * @return
    *   all submission assessments found in the given content
    */
  def getSubmissionAssessments(
    course: CourseSection,
    contents: Seq[CourseContent],
    policies: List[CourseAssessmentPolicy],
    ws: AttachedReadWorkspace
  ): Seq[SubmissionAssessment]
end SubmissionAssessmentService

object SubmissionAssessmentService:
  def filterSubmissionAssessmentContent(contents: Seq[CourseContent]): Seq[CourseContent] =
    contents.filter(content => SubmissionAssessmentAsset(content.asset).isDefined)
