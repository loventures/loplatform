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

import loi.cp.notification.NotificationService
import loi.cp.quiz.attempt.notification.AttemptInvalidationNotification
import loi.cp.submissionassessment.attempt.actions.SubmissionAttemptActionParameters

import scala.util.{Success, Try}

/** A submission assessment event handler to send out notifications to users on certain submission assessment events.
  */
class SubmissionAssessmentNotificationHandler(notificationService: NotificationService)
    extends SubmissionAttemptEventHandler:
  override def onEvent(environment: SubmissionAttemptActionParameters, event: SubmissionAttemptEvent): Try[Unit] =
    event match
      case AttemptInvalidatedEvent =>
        Try {
          val init: AttemptInvalidationNotification.Init =
            AttemptInvalidationNotification.Init(
              environment.assessment.title,
              environment.assessment.edgePath,
              environment.attempt.id,
              environment.attempt.contentId.contextId,
              environment.attempt.user.id,
              environment.time
            )
          notificationService.nοtify[AttemptInvalidationNotification](environment.attempt.user.id, init)
        }
      case _                       => Success(())
end SubmissionAssessmentNotificationHandler
