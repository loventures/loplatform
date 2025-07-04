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

package loi.cp.mastery

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.assessment.model.AssessmentType
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.attempt.AttemptState
import loi.cp.course.CourseSection
import loi.cp.mastery.store.MasteryDao
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.QuizAttemptService
import loi.cp.submissionassessment.SubmissionAssessmentService
import loi.cp.submissionassessment.attempt.SubmissionAttemptService

import java.util.UUID

@Service
class RecomputeMasteryService(
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  quizAttemptService: QuizAttemptService,
  submissionAssessmentService: SubmissionAssessmentService,
  submissionAttemptService: SubmissionAttemptService,
  masteryDao: MasteryDao,
  masteryService: MasteryService,
  legacyMasteryService: LegacyMasteryService,
):
  import RecomputeMasteryService.*

  /** Gets the user's mastered competencies, recomputing them if the user has unprocessed legacy mastery. When we
    * recompute everyone's mastery this can go away.
    */
  def getRecomputedMasteredCompetencies(ws: AttachedReadWorkspace, user: UserDTO, section: CourseSection): Set[UUID] =
    masteryDao.getMasteryFacade(user.id, section.id).map(_.getState) match
      case Some(state) if state.recomputed =>
        state.competencyMastery
      case _                               =>
        // if you have no user competency, or it has not been recomputed (say you
        // submitted evidence before viewing your competency) then recompute
        recomputeMastery(ws, user, section).competencyMastery

  def recomputeMastery(
    ws: AttachedReadWorkspace,
    user: UserDTO,
    section: CourseSection,
  ): UserMasteryState =
    val policies = courseAssessmentPoliciesService.getPolicies(section)

    val quizzes      = section.contents.nonRootElements
      .flatMap(Quiz.fromContent(_, section, policies))
      .filter(_.assessmentType == AssessmentType.Summative)
    val quizAttempts =
      quizAttemptService
        .getUserAttempts(section, quizzes, user)
        .filter(_.state == AttemptState.Submitted)
        .sortBy(_.submitTime)
        .reverse
        .distinctBy(_.assessment.edgePath)
        .map(Left.apply)

    val submissionAssessments =
      submissionAssessmentService
        .getSubmissionAssessments(section, section.contents.nonRootElements, policies, ws)
        .filter(_.assessmentType == AssessmentType.Summative)
    val submissionAttempts    = submissionAttemptService
      .getUserAttempts(section, submissionAssessments, user)
      .filter(_.state == AttemptState.Submitted)
      .sortBy(_.submitTime)
      .reverse
      .distinctBy(_.assessment.edgePath)
      .map(Right.apply)

    // Legacy mastery spans courses!
    val legacyMastery = legacyMasteryService
      .getLearnerMasteryData(user)
      .filter(_.mastered)
      .map(_.competency)
      .flatMap(ws.getNodeName)
      .toSet

    logger.info(
      s"Recomputing mastery for ${user.userName} in ${section.groupId} from ${quizAttempts.size} quiz attempts and ${submissionAssessments.size} submission attempts"
    )

    val state =
      masteryService.recomputeUserMasteryForAttempts(
        ws,
        user,
        section,
        legacyMastery,
        quizAttempts ++ submissionAttempts
      )

    logger.info(s"Added mastery of ${(state.competencyMastery -- legacyMastery).size} new competencies")

    state
  end recomputeMastery
end RecomputeMasteryService

object RecomputeMasteryService:
  private final val logger = org.log4s.getLogger
