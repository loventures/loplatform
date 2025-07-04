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

package loi.cp.quiz.question.ordering

import loi.asset.contentpart.HtmlPart
import loi.asset.question.{OrderingContent, OrderingQuestion}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.quiz.attempt.{DistractorIndexList, DistractorOrder}
import loi.cp.quiz.question.*
import loi.cp.reference.VersionedAssetReference

case class Sequencing(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  text: Option[String],
  allowDistractorRandomization: Boolean,
  choices: Seq[SequencingChoice],
  correctOrder: Seq[Int],
  rationales: Seq[Rationale],
  competencies: Seq[Competency]
) extends RandomizableQuestion:

  override def generateDistractorOrder(): DistractorIndexList =
    DistractorIndexList(DistractorOrder.randomIndices(choices))
end Sequencing

case class SequencingChoice(text: HtmlPart)

object Sequencing:
  def apply(
    ref: VersionedAssetReference,
    content: Asset[OrderingQuestion],
    assetRemediations: Seq[AssetRemediation],
    competencies: Seq[Competency]
  ): Sequencing =
    val questionContent: OrderingContent = content.data.questionContent
    val choices: Seq[SequencingChoice]   = questionContent.choices.map(_.description).map(SequencingChoice.apply)
    val correctOrder: Seq[Int]           = questionContent.choices.map(_.answerIndex.toInt)

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

    Sequencing(
      content,
      ref,
      pointsPossible,
      questionContent.getPublishQuestionText(),
      allowDistractorRandomization,
      choices,
      correctOrder,
      rationales ++ assetRemediations,
      competencies
    )
  end apply
end Sequencing
