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

package loi.cp.assessment

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.assessment.InstructorOverviews.{InstructorAttemptsOverview, UserGradingOverview}
import loi.cp.assessment.LearnerOverviews.LearnerAttemptOverview
import loi.cp.assessment.attempt.AttemptState.Open
import loi.cp.assessment.attempt.{AssessmentAttempt, AssessmentAttemptService, AssessmentParticipationData}
import loi.cp.content.CourseContents
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.lwgrade.{Grade, GradeService}
import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.{QuizAttempt, QuizAttemptService}
import loi.cp.submissionassessment.SubmissionAssessment
import loi.cp.submissionassessment.attempt.{SubmissionAttempt, SubmissionAttemptService}
import loi.cp.submissionassessment.settings.SubmissionAssessmentDriver.Observation
import scalaz.NonEmptyList

import java.time.Instant

@Service
class AssessmentAttemptServiceImpl(
  gradeService: GradeService,
  quizAttemptService: QuizAttemptService,
  submissionAttemptService: SubmissionAttemptService,
) extends AssessmentAttemptService:
  override def getAttempts(
    course: CourseSection,
    assessments: Seq[Assessment]
  ): Map[Assessment, Seq[AssessmentAttempt]] =
    val (quizzes, submissionAssessments) = Assessment.partition(assessments)

    val quizAttempts: Seq[QuizAttempt] =
      if quizzes.nonEmpty then quizAttemptService.getAttempts(course, quizzes)
      else Nil

    val submissionAttempts: Seq[SubmissionAttempt] =
      if submissionAssessments.nonEmpty then submissionAttemptService.getAttempts(course, submissionAssessments)
      else Nil

    val allAttempts: Seq[AssessmentAttempt] = quizAttempts ++ submissionAttempts

    assessments
      .map(assessment =>
        assessment ->
          allAttempts.filter(attempt => attempt.contentId == assessment.contentId)
      )
      .toMap
  end getAttempts

  override def countValidAttempts(course: CourseSection, assessments: Seq[Assessment]): AttemptCount =
    val (quizzes, submissionAssessments) = Assessment.partition(assessments)

    val qc  = quizAttemptService.countValidAttempts(course, quizzes)
    val sac = submissionAttemptService.countValidAttempts(course, submissionAssessments)

    qc + sac

  override def countValidAttempts(course: CourseSection, assessment: Assessment, userId: UserId): AttemptCount =
    assessment match
      case quiz: Quiz =>
        quizAttemptService.countValidAttempts(course, quiz, userId)

      case submissionAssessment: SubmissionAssessment =>
        submissionAttemptService.countValidAttempts(course, submissionAssessment, userId)

      case _ => 0

  override def getParticipationData(
    course: CourseSection,
    assessments: Seq[Assessment]
  ): Seq[AssessmentParticipationData] =
    val (quizzes, submissionAssessments) = Assessment.partition(assessments)

    (quizAttemptService.getParticipationData(course, quizzes)
      ++ submissionAttemptService.getParticipationData(course, submissionAssessments))

  override def getLearnerAttemptOverviews(
    course: CourseSection,
    assessments: Seq[Assessment],
    user: UserId
  ): Seq[LearnerAttemptOverview] =
    val (quizzes, submissionAssessments) = Assessment.partition(assessments)

    (quizAttemptService.getLearnerAttemptOverviews(course, quizzes, user)
      ++ submissionAttemptService.getLearnerAttemptOverviews(course, submissionAssessments, user))

  override def getInstructorAttemptsOverviews(
    course: CourseSection,
    assessments: Seq[Assessment]
  ): Seq[InstructorAttemptsOverview] =
    val (quizzes, submissionAssessments) = Assessment.partition(assessments)

    (quizAttemptService.getInstructorAttemptsOverviews(course, quizzes)
      ++ submissionAttemptService.getInstructorAttemptsOverviews(course, submissionAssessments))

  override def getGradingOverviews(
    courseId: ContextId,
    contents: CourseContents,
    attemptsByAssessment: Map[Assessment, Seq[AssessmentAttempt]],
    users: NonEmptyList[UserDTO]
  ): Seq[UserGradingOverview] =

    val gradebookByUser = gradeService.getCourseGradebooks(courseId, contents, users.map(u => UserId(u.id)))

    for
      (assessment, attempts) <- attemptsByAssessment.toSeq
      attemptsByUser          = attempts.groupBy(_.user.id)
      user                   <- users.list.toList
      userGradebook           = gradebookByUser(UserId(user.id))
    yield
      val possibleGrade: Option[Grade]   = userGradebook.grades.get(assessment.edgePath)
      val gradeValue: Option[BasicScore] =
        possibleGrade.flatMap { gr =>
          Grade.grade(gr).map(g => BasicScore(g, Grade.max(gr)))
        }

      attemptsByUser.get(user.id) match
        case Some(userAttempts) =>
          getGradingOverview(user, assessment, gradeValue, userAttempts)
        case None               =>
          UserGradingOverview(assessment.contentId, user, None, Nil, 0, 0, false, false, gradeValue)
    end for
  end getGradingOverviews

  private def getGradingOverview(
    user: UserDTO,
    assessment: Assessment,
    grade: Option[Score],
    attempts: Seq[AssessmentAttempt]
  ): UserGradingOverview =
    val attemptsAwaitingGrade: Seq[AssessmentAttempt] = assessment.gradingPolicy.attemptsAwaitingGrade(attempts)
    val maybeLatestSubmitTime: Option[Instant]        = attempts.flatMap(_.submitTime).sorted.lastOption
    val invalidAttemptCount: AttemptCount             = attempts.count(att => !att.valid)

    val hasViewableAttempts: Boolean = assessment match
      case sa: SubmissionAssessment if sa.settings.driver == Observation => attempts.nonEmpty
      case _                                                             => attempts.exists(att => att.state != Open)

    val hasValidViewableAttempts: Boolean = assessment match
      case sa: SubmissionAssessment if sa.settings.driver == Observation => attempts.exists(att => att.valid)
      case _                                                             => attempts.exists(att => att.valid && att.state != Open)

    UserGradingOverview(
      assessment.contentId,
      user,
      maybeLatestSubmitTime,
      attemptsAwaitingGrade.map(_.id),
      attempts.size,
      invalidAttemptCount,
      hasViewableAttempts,
      hasValidViewableAttempts,
      grade
    )
  end getGradingOverview
end AssessmentAttemptServiceImpl
