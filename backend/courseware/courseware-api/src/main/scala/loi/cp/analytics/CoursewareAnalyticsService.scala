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

package loi.cp.analytics

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.asset.Asset
import loi.authoring.workspace.ReadWorkspace
import loi.cp.analytics.entity.{CourseId, ExternallyIdentifiableEntity, Score}
import loi.cp.analytics.event.PublishContentEvent1
import loi.cp.assessment.attempt.AssessmentAttempt
import loi.cp.content.CourseContent
import loi.cp.course.lightweight.{LightweightCourse, Lwc}
import loi.cp.course.{CourseComponent, CourseSection}
import loi.cp.quiz.attempt.selection.QuestionResponseSelection
import loi.cp.quiz.question.Question
import loi.cp.reference.EdgePath
import loi.cp.survey.SurveyQuestionResponseDto

@Service
trait CoursewareAnalyticsService:

  def emitSectionEntryEvent(sectionId: Long, role: String, originSectionId: Option[Long]): Unit

  def emitSectionCreateEvent(section: CourseComponent, offering: LightweightCourse): Unit

  def emitSectionUpdateEvent(section: CourseComponent): Unit

  def emitPublishEvent(ws: ReadWorkspace, offering: Lwc, sections: List[Lwc]): PublishContentEvent1

  /** @param content
    *   the content being surveyed
    * @param responses
    *   responses to the survey questions
    */
  def emitSurveySubmissionEvent(
    section: Lwc,
    content: CourseContent,
    survey: Asset[?],
    surveyEdgePath: EdgePath,
    responses: List[SurveyQuestionResponseDto]
  ): Unit

  /** @param manualScore
    *   whether or not an instructor must score `attempt`
    */
  def emitAttemptPutEvent(attempt: AssessmentAttempt, manualScore: Boolean, maintenance: Boolean = false): Unit

  def emitGradePutEvent(
    user: UserDTO,
    section: CourseSection,
    content: CourseContent,
    score: Score,
    maintenance: Boolean = false
  ): Unit

  def emitGradeUnsetEvent(
    content: CourseContent,
    section: CourseSection,
    learner: UserDTO,
    scorer: UserDTO,
    maintenance: Boolean = false
  ): Unit
end CoursewareAnalyticsService

object CoursewareAnalyticsService:

  case class QuizQuestionInfo(
    question: Question,
    selection: Option[QuestionResponseSelection]
  )

  def courseId(section: CourseSection): CourseId = CourseId(
    section = ExternallyIdentifiableEntity(section.id, section.externalId),
    offeringId = Some(section.offeringId),
    branchId = Some(section.branch.id),
    commitId = Some(section.commitId),
    assetGuid = Some(section.asset.info.name),
    projectId = None
  )
end CoursewareAnalyticsService
