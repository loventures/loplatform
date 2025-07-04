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

package loi.cp.submissionassessment.attempt.event

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.eventing.{AbstractEventDispatchService, EventDispatchService, EventHandler}
import loi.cp.lwgrade.GradeService
import loi.cp.notification.NotificationService
import loi.cp.progress.LightweightProgressService
import loi.cp.submissionassessment.attempt.SubmissionAttemptLoadService
import loi.cp.submissionassessment.attempt.actions.SubmissionAttemptActionParameters
import loi.cp.user.UserService

@Service
trait SubmissionAssessmentEventDispatchService
    extends EventDispatchService[SubmissionAttemptActionParameters, SubmissionAttemptEvent]

@Service
class SubmissionAssessmentEventDispatchServiceImpl(
  gradeService: GradeService,
  notificationService: NotificationService,
  progressService: LightweightProgressService,
  submissionAttemptLoadService: SubmissionAttemptLoadService,
  userService: UserService,
) extends AbstractEventDispatchService[SubmissionAttemptActionParameters, SubmissionAttemptEvent]
    with SubmissionAssessmentEventDispatchService:
  override protected def handlers: Seq[? <: EventHandler[SubmissionAttemptActionParameters, SubmissionAttemptEvent]] =
    Seq(
      SubmissionAssessmentGradebookScoringEventHandler(gradeService, submissionAttemptLoadService, userService),
      new SubmissionAssessmentProgressEventHandler(progressService, gradeService, submissionAttemptLoadService),
      new SubmissionAssessmentNotificationHandler(notificationService)
    )
end SubmissionAssessmentEventDispatchServiceImpl
