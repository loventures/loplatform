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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.scala.util.JTypes.JBoolean
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.asset.assessment.model.AssessmentType
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.AssessmentGradingPolicy
import loi.cp.assessment.settings.AttemptLimit
import loi.cp.context.ContextId
import loi.cp.course.right.{ReadCourseRight, TeachCourseRight}
import loi.cp.instructions.Instructions
import loi.cp.quiz.Quiz
import loi.cp.quiz.api.QuizWebController.{QuestionSelectionDto, QuizDto}
import loi.cp.quiz.question.api.{QuestionCompetencyDto, QuestionDto}
import loi.cp.quiz.settings.{NavigationPolicy, QuizSettings, ResultsPolicy}
import loi.cp.reference.{ContentIdentifier, EdgePath, Identifier, VersionedAssetReference}

import scala.util.Try

@Controller(root = true, value = "lwQuiz")
@RequestMapping(path = "lwQuiz")
trait QuizWebController extends ApiRootComponent:

  @RequestMapping(path = "{id}", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[ReadCourseRight]))
  def getQuiz(
    @PathVariable("id") identifier: Identifier,
    @SecuredAdvice @MatrixParam("context") context: ContextId,
    @QueryParam(required = false) includeQuestions: Option[JBoolean],
  ): Try[QuizDto]

  @RequestMapping(path = "{id}/questions", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def getQuestionsForQuiz(
    @PathVariable("id") identifier: Identifier,
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): Try[QuestionSelectionDto]
end QuizWebController

object QuizWebController:
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
  @JsonSubTypes(
    Array(
      new Type(name = "linear", value = classOf[LinearQuestionSetDto]),
      new Type(name = "questionPool", value = classOf[QuestionPoolDto])
    )
  )
  sealed trait QuestionSelectionDto:
    def competencies: Seq[QuestionCompetencyDto]
  case class LinearQuestionSetDto(questions: Seq[QuestionDto])                extends QuestionSelectionDto:
    override def competencies: Seq[QuestionCompetencyDto] = questions.flatMap(_.competencies).distinct
  case class QuestionPoolDto(selectionSize: Int, questions: Seq[QuestionDto]) extends QuestionSelectionDto:
    override def competencies: Seq[QuestionCompetencyDto] = questions.flatMap(_.competencies).distinct

  case class QuizSettingsDto(
    maxAttempts: AttemptLimit,
    softAttemptLimit: AttemptLimit,
    softAttemptLimitMessage: Option[String],
    navigationPolicy: NavigationPolicy,
    resultsPolicy: ResultsPolicy,
    gradingPolicy: AssessmentGradingPolicy,
    displayConfidenceIndicators: Boolean,
    assessmentType: AssessmentType,
    maxMinutes: Option[Long],
    hasCompetencies: Boolean,
    isDiagnostic: Boolean,
    isCheckpoint: Boolean,
    testsOut: Map[EdgePath, Double],
  )
  object QuizSettingsDto:
    def apply(
      settings: QuizSettings,
      hasCompetencies: Boolean,
      isDiagnostic: Boolean,
      isCheckpoint: Boolean,
      accommodation: Option[Long],
      testsOut: Map[EdgePath, Double],
    ): QuizSettingsDto =
      QuizSettingsDto(
        settings.maxAttempts,
        settings.softAttemptLimit,
        settings.softAttemptLimitMessage,
        settings.navigationPolicy,
        settings.resultsPolicy,
        settings.gradingPolicy,
        settings.displayConfidenceIndicators,
        settings.assessmentType,
        accommodation.orElse(settings.maxMinutes).filter(_ != 0),
        hasCompetencies,
        isDiagnostic,
        isCheckpoint,
        testsOut,
      )
  end QuizSettingsDto

  case class QuizDto(
    assetReference: VersionedAssetReference,
    contentId: ContentIdentifier,
    title: String,
    settings: QuizSettingsDto,
    instructions: Option[Instructions],
    questions: Option[QuestionSelectionDto],
    pastDeadline: Boolean,
  )

  object QuizDto:
    def apply(
      quiz: Quiz,
      possibleQuestions: Option[QuestionSelectionDto],
      pastDeadline: Boolean,
      accommodation: Option[Long],
    ): QuizDto =
      val hasCompetencies: Boolean = possibleQuestions.exists(_.competencies.nonEmpty)

      QuizDto(
        quiz.assetReference,
        quiz.contentId,
        quiz.title,
        QuizSettingsDto(
          quiz.settings,
          hasCompetencies,
          quiz.isDiagnostic,
          quiz.isCheckpoint,
          accommodation,
          quiz.testsOut,
        ),
        quiz.instructions,
        possibleQuestions,
        pastDeadline
      )
    end apply
  end QuizDto
end QuizWebController
