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

package loi.cp.quiz.question.shortanswer

import loi.asset.question.{ShortAnswerContent, ShortAnswerQuestion}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.quiz.question.{AutoScorable, *}
import loi.cp.reference.VersionedAssetReference

case class ShortAnswer(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  text: Option[String],
  answer: String,
  rationales: Seq[Rationale],
  competencies: Seq[Competency]
) extends AutoScorable

object ShortAnswer:
  def apply(
    ref: VersionedAssetReference,
    content: Asset[ShortAnswerQuestion],
    assetRemediations: Seq[AssetRemediation],
    competencies: Seq[Competency]
  ): ShortAnswer =
    val questionContent: ShortAnswerContent = content.data.questionContent

    val pointValue: Double         = questionContent.pointsPossible.toDouble
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

    ShortAnswer(
      content,
      ref,
      pointValue,
      questionContent.getPublishQuestionText(),
      questionContent.answer,
      rationales ++ assetRemediations,
      competencies
    )
  end apply
end ShortAnswer
