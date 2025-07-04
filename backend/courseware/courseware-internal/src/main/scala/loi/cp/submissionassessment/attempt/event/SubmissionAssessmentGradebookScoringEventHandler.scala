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

import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.AssessmentGradingPolicy
import loi.cp.lwgrade.error.ColumnMissing
import loi.cp.lwgrade.{GradeService, GradeStructure}
import loi.cp.submissionassessment.attempt.actions.SubmissionAttemptActionParameters
import loi.cp.submissionassessment.attempt.{SubmissionAttempt, SubmissionAttemptLoadService}
import loi.cp.user.UserService
import scalaz.syntax.std.option.*

import scala.util.Try

/** A [[SubmissionAttemptEventHandler]] to assign grades when an attempt is finalized.
  */
case class SubmissionAssessmentGradebookScoringEventHandler(
  gradeService: GradeService,
  submissionAttemptLoadService: SubmissionAttemptLoadService,
  userService: UserService
) extends SubmissionAttemptEventHandler:

  override def onEvent(params: SubmissionAttemptActionParameters, event: SubmissionAttemptEvent): Try[Unit] =
    Try {
      event match
        case AttemptSubmittedEvent(_) =>
          val (section, gradeStructure, gradeColumn, attemptsAwaitingGrade, maybeGrade) = getEventInfo(params)

          if attemptsAwaitingGrade.nonEmpty && maybeGrade.isEmpty then
            gradeService.setGradePending(
              params.attempt.user,
              section,
              params.attempt.assessment.courseContent,
              gradeStructure,
              gradeColumn,
              params.attempt.updateTime // This may not be the actual attempt that determines the grade
            )

        case AttemptFinalizedEvent(_) =>
          val (section, gradeStructure, gradeColumn, attemptsAwaitingGrade, maybeGrade) = getEventInfo(params)

          maybeGrade match
            case None =>
              if attemptsAwaitingGrade.nonEmpty then
                gradeService.setGradePending(
                  params.attempt.user,
                  section,
                  params.attempt.assessment.courseContent,
                  gradeStructure,
                  gradeColumn,
                  params.attempt.updateTime // This may not be the actual attempt that determines the grade
                )

            case Some(gradePercent) =>
              gradeService.setGradePercent(
                params.attempt.user,
                section,
                params.attempt.assessment.courseContent,
                gradeStructure,
                gradeColumn,
                gradePercent,
                params.attempt.updateTime, // This may not be the actual attempt that determines the grade
              )
          end match

        case AttemptInvalidatedEvent =>
          val (section, gradeStructure, gradeColumn, attemptsAwaitingGrade, maybeGrade) = getEventInfo(params)

          maybeGrade match
            case None =>
              if attemptsAwaitingGrade.nonEmpty then
                gradeService.setGradePending(
                  params.attempt.user,
                  section,
                  params.attempt.assessment.courseContent,
                  gradeStructure,
                  gradeColumn,
                  params.attempt.updateTime
                )
              else
                // If all attempts for the user are invalidated or not finalize
                gradeService.unsetGrade(
                  params.attempt.user,
                  section,
                  params.attempt.assessment.courseContent
                )

            case Some(gradePercent) =>
              // otherwise we still need to recalc for the remaining valid attempts
              gradeService.setGradePercent(
                params.attempt.user,
                section,
                params.attempt.assessment.courseContent,
                gradeStructure,
                gradeColumn,
                gradePercent,
                params.attempt.updateTime,
              )
          end match
        case _                       => ()
    }

  private def getEventInfo(params: SubmissionAttemptActionParameters) =
    val section        = params.assessment.section
    val gradeStructure = GradeStructure(section.contents)
    val gradeColumn    = gradeStructure
      .findColumnForEdgePath(params.assessment.contentId.edgePath)
      .toRightDisjunction(
        ColumnMissing(params.assessment.contentId.contextId, params.assessment.contentId.edgePath)
      )
      .valueOr(colMissing => throw new IllegalStateException(colMissing.msg))

    val allAttemptsForUser: Seq[SubmissionAttempt] =
      submissionAttemptLoadService.getUserAttempts(section, Seq(params.assessment), params.attempt.user)

    val gradingPolicy: AssessmentGradingPolicy = params.assessment.settings.gradingPolicy
    val attemptsAwaitingGrade                  = gradingPolicy.attemptsAwaitingGrade(allAttemptsForUser)
    val grade                                  = gradingPolicy.getGrade(allAttemptsForUser)

    (section, gradeStructure, gradeColumn, attemptsAwaitingGrade, grade)
  end getEventInfo

  private def loadUser(userId: Long): UserDTO =
    userService.getUser(userId).getOrElse(throw new IllegalStateException(s"No such user $userId"))
end SubmissionAssessmentGradebookScoringEventHandler
