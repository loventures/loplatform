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

package loi.cp.submissionassessment.api
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.course.{CourseSection, CourseWorkspaceService}
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.reference.EdgePath
import loi.cp.submissionassessment.attempt.{AttemptNotFound, SubmissionAttempt, SubmissionAttemptService}
import loi.cp.submissionassessment.{SubmissionAssessment, SubmissionAssessmentService}
import scalaz.\/
import scalaz.syntax.std.option.*

/** A utility for loading submission assessment content with less ceremony.
  */
@Service
class SubmissionAssessmentWebUtils(
  courseWebUtils: CourseWebUtils,
  submissionAttemptService: SubmissionAttemptService,
  submissionAssessmentService: SubmissionAssessmentService,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  courseWorkspaceService: CourseWorkspaceService,
):

  /** Loads a course and submission assessment for read access.
    *
    * @param edgePath
    *   the location of the submission assessment
    * @param user
    *   the requesting user
    * @return
    *   the course and submission assessment, if it could be loaded
    */
  def readSubmissionAssessment(
    contextId: ContextId,
    edgePath: EdgePath,
    user: UserId,
    rights: Option[CourseRights] = None,
  ): String \/ (CourseSection, SubmissionAssessment) =
    for
      section    <- courseWebUtils.loadCourseSection(contextId.id, rights)
      ws          = courseWorkspaceService.loadReadWorkspace(section)
      assessment <- readSubmissionAssessment(section, ws, edgePath, user)
    yield (section, assessment)

  /** Loads a submission assessment for read access.
    *
    * @param section
    *   the section containing the assessment
    * @param edgePath
    *   the location of the submission assessment
    * @param user
    *   the requesting user
    * @return
    *   the course and submission assessment, if it could be loaded
    */
  def readSubmissionAssessment(
    section: CourseSection,
    ws: AttachedReadWorkspace,
    edgePath: EdgePath,
    user: UserId,
  ): String \/ SubmissionAssessment =
    for
      content              <- section.contents
                                .get(edgePath)
                                .toRightDisjunction(s"No such submission assessment: $edgePath in ${section.id}")
      policies              = courseAssessmentPoliciesService.getPolicies(section)
      submissionAssessment <- submissionAssessmentService
                                .getSubmissionAssessment(section, content, policies, ws)
                                .toRightDisjunction(s"No submission assessment found for $edgePath in ${section.id}")
    yield submissionAssessment

  def loadAttempt(
    lwc: CourseSection,
    ws: AttachedReadWorkspace,
    attemptId: Long
  ): AttemptNotFound \/ SubmissionAttempt =
    // Being lazy here, as this only gets used in one place and its in attachments were
    // policies don't matter
    submissionAttemptService.fetch(lwc, ws, attemptId, List.empty).toRightDisjunction(AttemptNotFound(attemptId))
end SubmissionAssessmentWebUtils
