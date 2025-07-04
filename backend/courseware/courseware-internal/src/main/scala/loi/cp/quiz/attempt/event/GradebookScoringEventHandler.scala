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

package loi.cp.quiz.attempt.event

import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.lwgrade.{GradeService, GradeStructure}
import loi.cp.quiz.attempt.{QuestionResponseState, QuizActionParameters, QuizAttempt, QuizAttemptLoadService}
import loi.cp.user.UserService

import java.time.Instant
import scala.util.Try

case class GradebookScoringEventHandler(
  gradeService: GradeService,
  quizAttemptLoadService: QuizAttemptLoadService,
  userService: UserService
) extends QuizAttemptEventHandler:

  override def onEvent(params: QuizActionParameters, quizAttemptEvent: QuizAttemptEvent): Try[Unit] =
    Try {
      quizAttemptEvent match
        case AttemptSubmittedEvent(_) =>
          val (section, gradeStructure, gradeColumn, attemptsAwaitingGrade, maybeGrade) = getEventInfo(params)

          gradeColumn.foreach(gradeColumn =>
            if attemptsAwaitingGrade.nonEmpty && maybeGrade.isEmpty then
              gradeService.setGradePending(
                params.attempt.user,
                section,
                params.attempt.assessment.courseContent,
                gradeStructure,
                gradeColumn,
                params.attempt.updateTime // This may not be the actual attempt that determines the grade
              )
          )

        case AttemptFinalizedEvent(_) =>
          val (section, gradeStructure, gradeColumn, attemptsAwaitingGrade, maybeGrade) = getEventInfo(params)

          gradeColumn.foreach(gradeColumn =>
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
                  params.attempt.updateTime, // This may not be the actual attempt that determines the grade,
                )
          )

        case _ => ()
    }

  private def getEventInfo(params: QuizActionParameters) =
    val section        = params.quiz.section
    val gradeStructure = GradeStructure(section.contents)
    val gradeColumn    = gradeStructure.findColumnForEdgePath(params.quiz.contentId.edgePath)

    if gradeColumn.isEmpty then
      // the grade column will not exist when the assessment is a course child
      // such a structure cannot be authored today, but was in the past and remains in use for diagnostic.1s
      // there could be other scenarios but no one has mentioned them to us in the past 2ish years
      logger.info(s"No column for path ${params.quiz.contentId.edgePath} in course ${params.quiz.contextId}")

    val allAttemptsForUser: Seq[QuizAttempt] =
      quizAttemptLoadService.getUserAttempts(section, Seq(params.quiz), params.attempt.user)
    val gradingPolicy                        = params.quiz.settings.gradingPolicy
    val attemptsAwaitingGrade                = gradingPolicy.attemptsAwaitingGrade(allAttemptsForUser)
    val grade                                = gradingPolicy.getGrade(allAttemptsForUser)

    (section, gradeStructure, gradeColumn, attemptsAwaitingGrade, grade)
  end getEventInfo

  private def lastScorerId(a: QuizAttempt): Option[Long] =
    a.responses
      .filter(_.state == QuestionResponseState.ResponseScoreReleased)
      .flatMap(r => for (scorerId <- r.scorer; scoreTime <- r.scoreTime) yield (scorerId, scoreTime))
      .sorted(using (x: (Long, Instant), y: (Long, Instant)) => y._2.compareTo(x._2))
      .headOption
      .map(_._1)

  private def loadUser(userId: Long): UserDTO =
    userService.getUser(userId).getOrElse(throw new IllegalStateException(s"No such user $userId"))
end GradebookScoringEventHandler
