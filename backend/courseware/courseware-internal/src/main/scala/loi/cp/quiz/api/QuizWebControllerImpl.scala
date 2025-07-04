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

package loi.cp.quiz.api

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.JTypes.JBoolean
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.api.AssessmentValidationUtils
import loi.cp.content.CourseContentService
import loi.cp.content.gate.ContentGateOverrideService
import loi.cp.context.ContextId
import loi.cp.course.{CourseAccessService, CourseSection, CourseWorkspaceService}
import loi.cp.customisation.{CourseCustomisationService, Customisation}
import loi.cp.quiz.Quiz
import loi.cp.quiz.question.api.QuestionDtoUtils.*
import loi.cp.quiz.question.api.QuestionRationaleDisplay
import loi.cp.quiz.question.api.QuestionRationaleDisplay.All
import loi.cp.quiz.question.{LinearQuestionSet, QuestionPool, QuestionService}
import loi.cp.reference.{ContentIdentifierWrapper, Identifier}
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.option.*

import scala.util.{Failure, Try}

@Component
class QuizWebControllerImpl(
  val componentInstance: ComponentInstance,
  courseAccessService: CourseAccessService,
  courseContentService: CourseContentService,
  courseCustomisationService: CourseCustomisationService,
  quizWebUtils: QuizWebUtils,
  questionService: QuestionService,
  assessmentValidationUtils: AssessmentValidationUtils,
  courseWorkspaceService: CourseWorkspaceService,
  overrideService: ContentGateOverrideService,
  time: TimeSource,
  user: => UserDTO
) extends QuizWebController
    with ComponentImplementation:
  import QuizWebController.*

  override def getQuiz(identifier: Identifier, context: ContextId, includeQuestions: Option[JBoolean]): Try[QuizDto] =
    def getQuestions(
      section: CourseSection,
      ws: AttachedReadWorkspace,
      quiz: Quiz,
      customisation: Customisation,
      instructor: Boolean
    ): Option[QuestionSelectionDto] =
      if includeQuestions.contains(true) && instructor then
        val dtos: QuestionSelectionDto =
          getQuestionsForQuiz(
            section,
            ws,
            quiz,
            customisation,
            includeCorrect = true,
            includedQuestionRationale = All,
            includeDistractorRationale = true
          )

        Some(dtos)
      else None

    identifier match
      case ContentIdentifierWrapper(contentId) =>
        for
          rights          <-
            courseAccessService.actualRights(contentId.contextId, user) <@~* new AccessForbiddenException("No access")
          (section, quiz) <- quizWebUtils
                               .readQuiz(contentId.contextId, contentId.edgePath, rights.some)
                               .toTry(new ResourceNotFoundException(_))
        yield
          val ws                = courseWorkspaceService.loadReadWorkspace(section)
          val instructor        = rights.likeInstructor
          val customisation     = courseCustomisationService.loadCustomisation(section.lwc)
          val possibleQuestions = getQuestions(section, ws, quiz, customisation, instructor)
          val pastDeadline      = assessmentValidationUtils.isPastDeadline(section, quiz, time.instant, user.id)
          val accommodation     = instructor.flatNoption(
            overrideService.loadAccommodations(quiz.section, user).toOption.flatMap(_.get(quiz.edgePath))
          )
          QuizDto(
            quiz,
            possibleQuestions,
            pastDeadline,
            accommodation,
          )

      case _ => Failure(new InvalidRequestException("Persistence ID-based requests not supported yet"))
    end match
  end getQuiz

  override def getQuestionsForQuiz(identifier: Identifier, context: ContextId): Try[QuestionSelectionDto] =
    identifier match
      case ContentIdentifierWrapper(contentId) =>
        for
          rights          <-
            courseAccessService.actualRights(contentId.contextId, user) <@~* new AccessForbiddenException("No access")
          (section, quiz) <- quizWebUtils
                               .readQuiz(contentId.contextId, contentId.edgePath, rights.some)
                               .toTry(new ResourceNotFoundException(_))
        yield
          val ws                    = courseWorkspaceService.loadReadWorkspace(section)
          val canViewCorrectAnswers = rights.likeInstructor
          val customisation         = courseCustomisationService.loadCustomisation(section.lwc)
          getQuestionsForQuiz(
            section,
            ws,
            quiz,
            customisation,
            canViewCorrectAnswers,
            includedQuestionRationale = All,
            includeDistractorRationale = true
          )
      case _                                   => Failure(new InvalidRequestException("Persistence ID-based requests not supported yet"))

  private def getQuestionsForQuiz(
    section: CourseSection,
    ws: AttachedReadWorkspace,
    quiz: Quiz,
    customisation: Customisation,
    includeCorrect: Boolean,
    includedQuestionRationale: QuestionRationaleDisplay,
    includeDistractorRationale: Boolean
  ): QuestionSelectionDto =

    val unassessables = courseContentService.findUnassessables(ws, section)

    questionService.getQuestions(quiz, unassessables, customisation, ws) match
      case LinearQuestionSet(questions)                    =>
        LinearQuestionSetDto(
          questions.map(_.toDto(includeCorrect, includedQuestionRationale, includeDistractorRationale))
        )
      case QuestionPool(questionCount, availableQuestions) =>
        QuestionPoolDto(
          questionCount,
          availableQuestions.map(_.toDto(includeCorrect, includedQuestionRationale, includeDistractorRationale))
        )
    end match
  end getQuestionsForQuiz
end QuizWebControllerImpl
