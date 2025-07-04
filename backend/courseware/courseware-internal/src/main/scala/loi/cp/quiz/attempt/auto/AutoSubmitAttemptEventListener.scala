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

package loi.cp.quiz.attempt.auto

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import loi.cp.appevent.{OnEvent, OnEventBinding, OnEventComponent}
import loi.cp.assessment.AssessmentLogMeta
import loi.cp.assessment.attempt.AttemptState
import loi.cp.course.{CourseSectionService, CourseWorkspaceService}
import loi.cp.notification.NotificationService
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.quiz.attempt.{QuizAttemptFailure, QuizAttemptService}

@Component
@OnEventBinding(Array(classOf[AutoSubmitAttemptEvent]))
class AutoSubmitAttemptEventListener(
  val componentInstance: ComponentInstance,
  courseSectionService: CourseSectionService,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  courseWorkspaceService: CourseWorkspaceService,
  quizAttemptService: QuizAttemptService,
  notificationService: NotificationService,
) extends OnEventComponent
    with ComponentImplementation:

  import AutoSubmitAttemptEventListener.*

  @OnEvent
  def onAutoSubmitAttemptEvent(event: AutoSubmitAttemptEvent): Unit =
    log info s"Executing $event"
    for
      section <- courseSectionService.getCourseSection(event.sectionId)
      policies = courseAssessmentPoliciesService.getPolicies(section)
      attempt <- quizAttemptService.fetchForUpdate(section, event.attemptId, policies)
      _        = AssessmentLogMeta.attemptMetadata(attempt)
      if attempt.valid && attempt.state == AttemptState.Open
    do
      val ws = courseWorkspaceService.loadReadWorkspace(section)
      log info "Auto-submitting quiz attempt"

      val submitted = quizAttemptService
        .submitAttempt(ws, section, attempt, autoSubmit = true)
        .valueOr(e => throw AutoSubmitException(e))

      notificationService.nοtify[AutoSubmitAttemptNotification](
        attempt.user.id,
        AutoSubmitAttemptNotification.Init(
          submitted.assessment.title,
          submitted.assessment.edgePath,
          submitted.id,
          submitted.contentId.contextId,
          submitted.user.id,
          submitted.submitTime.get
        )
      )
    end for
  end onAutoSubmitAttemptEvent
end AutoSubmitAttemptEventListener

object AutoSubmitAttemptEventListener:
  final val log = org.log4s.getLogger

final case class AutoSubmitException(failure: QuizAttemptFailure) extends RuntimeException(failure.message)
