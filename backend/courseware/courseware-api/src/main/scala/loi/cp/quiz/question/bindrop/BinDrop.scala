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

package loi.cp.quiz.question.bindrop

import loi.asset.question.{BinContent, BinDropContent, BinDropQuestion, OptionContent}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.quiz.attempt.{BinDropDistractorOrder, DistractorOrder}
import loi.cp.quiz.question.*
import loi.cp.reference.VersionedAssetReference

case class BinDrop(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  text: Option[String],
  allowDistractorRandomization: Boolean,
  awardsPartialCredit: Boolean,
  bins: Seq[Bin],
  options: Seq[BinOption],
  rationales: Seq[Rationale],
  competencies: Seq[Competency]
) extends RandomizableQuestion:
  override def generateDistractorOrder(): BinDropDistractorOrder =
    BinDropDistractorOrder(DistractorOrder.randomIndices(bins), DistractorOrder.randomIndices(options))
end BinDrop

case class Bin(text: String, correctOptionIndices: Set[Int]):
  def isCorrect(selectedOptions: Set[Int]): Boolean =
    // You are only correct if you select all of the correct bins and none of the incorrect bins
    correctOptionIndices.equals(selectedOptions)
case class BinOption(text: String)

object BinDrop:
  def apply(
    ref: VersionedAssetReference,
    content: Asset[BinDropQuestion],
    assetRemediations: Seq[AssetRemediation],
    competencies: Seq[Competency]
  ): BinDrop =
    val questionContent: BinDropContent = content.data.questionContent

    val options: Seq[BinOption] = questionContent.options.map(option)

    val correctResponses: Seq[(Int, Int)] =
      for
        (optionIndex, option) <- questionContent.options.indices zip questionContent.options
        correctBinIndex       <- option.binIndex
      yield correctBinIndex.toInt -> optionIndex

    val correctOptionsForBin: Map[Int, Set[Int]] =
      correctResponses.groupBy(_._1).transform((_, tuples) => tuples.map(_._2).toSet)

    val bins: Seq[Bin] =
      for
        (bin, binIndex) <- questionContent.bins.zipWithIndex
        correctOptions   = correctOptionsForBin.getOrElse(binIndex, Set.empty)
      yield Bin(bin.label, correctOptions)

    val allowPartialCredit: Boolean = content.data.allowPartialCredit

    val pointsPossible: Double             = questionContent.pointsPossible.toDouble
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
    BinDrop(
      content,
      ref,
      pointsPossible,
      questionContent.getPublishQuestionText(),
      questionContent.allowDistractorRandomization.contains(true),
      allowPartialCredit,
      bins,
      options,
      feedbackRationales ++ assetRemediations,
      competencies
    )
  end apply

  def bin(content: BinContent, correctOptionIndices: Set[Int]): Bin = Bin(content.label, correctOptionIndices)

  def option(content: OptionContent): BinOption = BinOption(content.label)
end BinDrop
