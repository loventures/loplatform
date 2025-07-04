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

package loi.cp.quiz.question.hotspot

import loi.asset.question.{HotspotQuestionContent, HotspotQuestion, HotspotChoice as AuthoringHotspotChoice}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.quiz.attempt.selection.Point
import loi.cp.quiz.question.*
import loi.cp.reference.VersionedAssetReference

case class Hotspot(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  text: Option[String],
  choices: Seq[HotspotChoice],
  rationales: Seq[Rationale],
  competencies: Seq[Competency],
  image: Option[VersionedAssetReference]
) extends AutoScorable:
  def selectedChoice(point: Point): Option[HotspotChoice] =
    choices.find(choice => choice.contains(point))
end Hotspot

case class HotspotChoice(x: Double, y: Double, shape: Shape, points: Double, correct: Boolean):
  def contains(point: Point): Boolean =
    this.shape match
      case Circle(radius)           =>
        val distance: Double = Math.sqrt(Math.pow(point.x - this.x, 2) + Math.pow(point.y - this.y, 2))
        distance <= radius
      case Rectangle(width, height) =>
        val validX: Boolean = point.x >= this.x && point.x <= this.x + width
        val validY: Boolean = point.y >= this.y && point.y <= this.y + height

        validX && validY
end HotspotChoice

object Hotspot:
  def apply(
    ref: VersionedAssetReference,
    content: Asset[HotspotQuestion],
    assetRemediations: Seq[Rationale],
    competencies: Seq[Competency],
    image: Option[VersionedAssetReference]
  ): Hotspot =
    val questionContent: HotspotQuestionContent = content.data.questionContent
    val text: Option[String]                    = questionContent.getPublishQuestionText()

    val questionChoices: Seq[HotspotChoice] = choices(content)

    val pointValue: Double = questionContent.pointsPossible.toDouble

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

    Hotspot(
      content,
      ref,
      pointValue,
      text,
      questionChoices,
      feedbackRationales ++ assetRemediations,
      competencies,
      image
    )
  end apply

  def choices(hotspotQuestion: Asset[HotspotQuestion]): Seq[HotspotChoice] =
    for choice <- hotspotQuestion.data.questionContent.choices
    yield
      val points: Double =
        if choice.correct then hotspotQuestion.data.questionContent.pointsPossible.toDouble
        else 0.0

      Hotspot.choice(choice, points)

  def choice(choice: AuthoringHotspotChoice, points: Double): HotspotChoice =
    choice.shape.toUpperCase match
      case "CIRCLE"    => HotspotChoice(choice.x, choice.y, Circle(choice.radius), points, choice.correct)
      case "RECTANGLE" =>
        HotspotChoice(choice.x, choice.y, Rectangle(choice.width, choice.height), points, choice.correct)
end Hotspot
