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

package loi.authoring.exchange.docx

import loi.cp.i18n.AuthoringBundle.message
import loi.cp.i18n.BundleMessage
import scaloi.syntax.box.*

private[exchange] object DocxErrors:

  sealed abstract class DocxValidationError(msg: BundleMessage):
    lazy val error: String = msg.value

  sealed abstract class DocxValidationWarning(msg: BundleMessage):
    lazy val error: String = msg.value

  sealed trait QuestionError:
    val question: Int

  case object NoQuestionsError extends DocxValidationError(message("docx.error.noQuestions"))

  case class BlankTextError(question: Int, row: Int, column: Int)
      extends DocxValidationError(
        message("docx.error.missingText", question.box, row.box, column.box)
      )
      with QuestionError

  case class ColumnCountError(question: Int, row: Int, expected: Int, actual: Int)
      extends DocxValidationError(
        message("docx.error.columnCount", question.box, row.box, expected.box, actual.box)
      )
      with QuestionError

  case class MissingRowError(question: Int, row: Int)
      extends DocxValidationError(
        message("docx.error.missingRow", question.box, row.box)
      )
      with QuestionError

  case class InsufficientRowsError(question: Int, expected: Int, actual: Int)
      extends DocxValidationError(
        message("docx.error.insufficientRows", question.box, expected.box, actual.box)
      )
      with QuestionError

  case class UnknownQuestionTypeError(question: Int, `type`: String)
      extends DocxValidationError(
        message("docx.error.invalidQuestionType", question.box, `type`, DocxQuestionType.values.mkString(",  "))
      )
      with QuestionError

  case class InvalidQuestionNumberError(question: Int, text: String)
      extends DocxValidationError(
        message("docx.error.invalidQuestionNumber", question.box, text)
      )
      with QuestionError

  case class RequiresOneTrueError(question: Int)
      extends DocxValidationError(
        message("docx.error.tf.oneTrue", question.box)
      )
      with QuestionError

  case class RequiresOneCorrectError(question: Int)
      extends DocxValidationError(
        message("docx.error.oneCorrect", question.box)
      )
      with QuestionError

  case class RequiresSomeCorrectError(question: Int)
      extends DocxValidationError(
        message("docx.error.someCorrect", question.box)
      )
      with QuestionError

  case class RequiresSomeIncorrectError(question: Int)
      extends DocxValidationError(
        message("docx.error.someIncorrect", question.box)
      )
      with QuestionError

  case class TooManyChoicesError(question: Int, expected: Int, actual: Int)
      extends DocxValidationError(
        message("docx.error.tooManyChoices", question.box, expected.box, actual.box)
      )
      with QuestionError

  case class UnknownCompetencyWarning(question: Int, competency: String)
      extends DocxValidationWarning(
        message("docx.error.unknownCompetency", question.box, competency)
      )
      with QuestionError

  case class UnknownRowTypeWarning(question: Int, tpe: String)
      extends DocxValidationWarning(
        message("docx.error.unknownRowType", question.box, tpe)
      )
      with QuestionError

  case class DuplicateFeedbackWarning(question: Int, tpe: String)
      extends DocxValidationWarning(
        message("docx.error.duplicateFeedback", question.box, tpe)
      )
      with QuestionError

  case class MissingBlankError(question: Int, blank: String)
      extends DocxValidationError(
        message("docx.error.missingBlank", question.box, blank)
      )
      with QuestionError

  case class UnknownBlankError(question: Int, blank: String)
      extends DocxValidationError(
        message("docx.error.unknownBlank", question.box, blank)
      )
      with QuestionError

  case class InvalidBlankError(question: Int, blank: String)
      extends DocxValidationError(
        message("docx.error.invalidBlank", question.box, blank)
      )
      with QuestionError
end DocxErrors
