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

package loi.cp.quiz.question.fillintheblank

import loi.asset.question.{FillInTheBlankContent, FillInTheBlankQuestion}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.quiz.question.*
import loi.cp.reference.VersionedAssetReference
import scaloi.syntax.option.*

case class FillInTheBlank(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  awardsPartialCredit: Boolean,
  text: Option[String],
  blanks: Seq[Blank],
  rationales: Seq[Rationale],
  competencies: Seq[Competency],
  caseSensitive: Boolean
) extends AutoScorable

case class Blank(offset: Int, answers: Seq[String])

object FillInTheBlank:

  def apply(
    ref: VersionedAssetReference,
    content: Asset[FillInTheBlankQuestion],
    assetRemediations: Seq[AssetRemediation],
    competencies: Seq[Competency]
  ): FillInTheBlank =
    val questionContent: FillInTheBlankContent = content.data.questionContent
    val text: Option[String]                   = questionContent.getPublishQuestionText().map(parseQuestionText)

    val questionBlanks: Seq[Blank] = blanks(content)

    val allowPartialCredit: Boolean = content.data.allowPartialCredit

    val pointValue: Double                 = questionContent.pointsPossible.toDouble
    val feedbackRationales: Seq[Rationale] =
      Seq(
        questionContent.richCorrectAnswerFeedback
          .flatMap(_.renderedHtml)
          .filter(_.nonEmpty)
          .map(CorrectRationale.apply),
        questionContent.richIncorrectAnswerFeedback
          .flatMap(_.renderedHtml)
          .filter(_.nonEmpty)
          .map(TextRemediation.apply)
      ).flatten

    FillInTheBlank(
      content,
      ref,
      pointValue,
      allowPartialCredit,
      text,
      questionBlanks,
      feedbackRationales ++ assetRemediations,
      competencies,
      caseSensitive = questionContent.caseSensitive.isTrue
    )
  end apply

  def parseQuestionText(questionText: String): String =
    FillInTheBlankQuestion.BlankPattern.replaceAllIn(questionText, "")

  def blanks(question: Asset[FillInTheBlankQuestion]): Seq[Blank] =
    def processTextForBlanks(remainingText: String): Seq[Blank] =
      FillInTheBlankQuestion.BlankPattern.findFirstMatchIn(remainingText) match
        case Some(m) =>
          val answers: Seq[String] = m.group(1).split(";").toSeq
          Blank(m.start, answers) +: processTextForBlanks(
            FillInTheBlankQuestion.BlankPattern.replaceFirstIn(remainingText, "")
          )
        case None    => Nil
    question.data.questionContent.getPublishQuestionText().map(processTextForBlanks).getOrElse(Nil)
  end blanks
end FillInTheBlank
