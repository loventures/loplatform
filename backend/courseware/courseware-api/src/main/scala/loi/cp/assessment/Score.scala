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

import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonSubTypes, JsonTypeInfo}
import loi.asset.rubric.model.RubricCriterion
import loi.authoring.asset.Asset
import loi.cp.assessmentservices.services.rest.feedback.ScoredRubricSection

import java.util.UUID

/** A simple trait representing a score with a points awarded and points possible. Any further semantic meaning is
  * covered by implementations.
  */
trait Score:

  /** the number of points awarded */
  @JsonProperty
  def pointsAwarded: Double

  /** the maximum number of points the {{pointsPossible}} is out of */
  @JsonProperty
  def pointsPossible: Double

  /** Returns whether this score is for a correct value. Correct values are any case where all the points for the
    * question are awarded.
    *
    * @return
    *   whether this score is for a correct response
    */
  @JsonIgnore
  def isCorrect: Boolean = pointsAwarded - pointsPossible + Score.Epsilon > 0

  /** Returns this score as a percentage. This method throws an exception if [[pointsPossible]] is 0.
    *
    * @return
    *   this score as a percentage
    */
  @JsonIgnore
  def asPercentage: Double = pointsAwarded / pointsPossible
end Score

object Score:
  final val Basic  = "basic"
  final val Rubric = "rubric"

  def byAwarded[T <: Score]: Ordering[T]    = Ordering.by(_.pointsAwarded)
  def byPercentage[T <: Score]: Ordering[T] =
    Ordering.by({ score =>
      if score.pointsPossible == 0 then 0
      else score.pointsAwarded / score.pointsPossible
    })

  /** The minimal significant difference between [[Double]]s in the Quiz system */
  final val Epsilon = 0.001
end Score

/** A score against a single logical response.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "scoreType")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[BasicScore], name = Score.Basic),
    new JsonSubTypes.Type(value = classOf[RubricScore], name = Score.Rubric),
  )
)
sealed trait ResponseScore extends Score

case class BasicScore(pointsAwarded: Double, override val pointsPossible: Double) extends ResponseScore:
  override def equals(that: Any): Boolean =
    that match
      case that: BasicScore =>
        (Math.abs(that.pointsAwarded - pointsAwarded) < Score.Epsilon) &&
        (Math.abs(that.pointsPossible - pointsPossible) < Score.Epsilon)
      case _                => false

case class RubricScore(pointsPossible: Double, nullableCriterionScores: Map[UUID, SectionScore]) extends ResponseScore:
  val pointsAwarded: Double =
    val summedRubricPointsAwarded: Double = criterionScores.values.toSeq.map(_.pointsAwarded).sum
    val summedPointsPossible: Double      = criterionScores.values.toSeq.map(_.pointsPossible).sum

    if Math.abs(summedPointsPossible) < Score.Epsilon then 0
    else summedRubricPointsAwarded / summedPointsPossible * pointsPossible

  // Jackson gives nulls rather than empty maps :(
  def criterionScores: Map[UUID, SectionScore] = Option(nullableCriterionScores).getOrElse(Map.empty)
end RubricScore

case class SectionScore(pointsAwarded: Double, pointsPossible: Double):
  @JsonIgnore
  def asPercentage: Double = pointsAwarded / pointsPossible

object SectionScore:
  def apply(sectionScore: ScoredRubricSection, criterion: Asset[RubricCriterion]): SectionScore =
    SectionScore(sectionScore.getLevelGrade, getTotalPoints(criterion).toDouble)

  private def getTotalPoints(criterion: Asset[RubricCriterion]): Int =
    if criterion.data.levels.isEmpty then 0
    else criterion.data.levels.map(_.points).max
