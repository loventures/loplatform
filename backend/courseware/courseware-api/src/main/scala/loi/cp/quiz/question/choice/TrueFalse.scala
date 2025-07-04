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

package loi.cp.quiz.question.choice

import loi.asset.question.{ChoiceQuestionContent, TrueFalseQuestion}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.quiz.question.{AssetRemediation, CorrectRationale, Rationale, TextRemediation}
import loi.cp.reference.VersionedAssetReference

case class TrueFalse(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  text: Option[String],
  override val allowDistractorRandomization: Boolean,
  choices: Seq[Choice],
  rationales: Seq[Rationale],
  competencies: Seq[Competency]
) extends ChoiceQuestion

object TrueFalse:
  def apply(
    ref: VersionedAssetReference,
    content: Asset[TrueFalseQuestion],
    assetRemediations: Seq[AssetRemediation],
    competencies: Seq[Competency]
  ): TrueFalse =
    val questionContent: ChoiceQuestionContent = content.data.questionContent
    val choices: Seq[Choice]                   = questionContent.choices.map(ChoiceQuestion.choice)

    val pointsPossible: Double                = questionContent.pointsPossible.toDouble
    val allowDistractorRandomization: Boolean = questionContent.allowDistractorRandomization.contains(true)

    val rationales: Seq[Rationale] =
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

    TrueFalse(
      content,
      ref,
      pointsPossible,
      questionContent.getPublishQuestionText(),
      allowDistractorRandomization,
      choices,
      rationales ++ assetRemediations,
      competencies
    )
  end apply
end TrueFalse
