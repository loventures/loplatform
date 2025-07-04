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

package loi.cp.quiz.question.api

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import loi.asset.question.QuestionScoringOption
import loi.cp.assessment.Score
import loi.cp.assessment.rubric.AssessmentRubric
import loi.cp.quiz.question.Rationale
import loi.cp.quiz.question.bindrop.{Bin, BinOption}
import loi.cp.quiz.question.fillintheblank.Blank
import loi.cp.quiz.question.hotspot.Shape
import loi.cp.quiz.question.matching.{Definition, Term}
import loi.cp.quiz.question.ordering.SequencingChoice
import loi.cp.reference.VersionedAssetReference

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes(
  Array(
    new Type(name = "fillInTheBlank", value = classOf[FillInTheBlankDto]),
    new Type(name = "essay", value = classOf[EssayDto]),
    new Type(name = "trueFalse", value = classOf[TrueFalseDto]),
    new Type(name = "multipleSelect", value = classOf[MultipleSelectDto]),
    new Type(name = "binDrop", value = classOf[BinDropDto]),
    new Type(name = "hotspot", value = classOf[HotspotDto]),
    new Type(name = "shortAnswer", value = classOf[ShortAnswerDto]),
    new Type(name = "multipleChoice", value = classOf[MultipleChoiceDto]),
    new Type(name = "matching", value = classOf[MatchingDto]),
    new Type(name = "sequencing", value = classOf[SequencingDto])
  )
)
sealed trait QuestionDto:
  val includesCorrect: Boolean
  val questionText: String
  val scoringOption: QuestionScoringOption = QuestionScoringOption.AllOrNothing
  val manuallyGraded: Boolean              = false
  val pointsPossible: Double
  val rationales: Seq[Rationale]
  val competencies: Seq[QuestionCompetencyDto]
  val reference: VersionedAssetReference

  def _type: String = getClass.getName
end QuestionDto

sealed trait HasChoices:
  val choices: Seq[ChoiceDto]

case class MultipleChoiceDto(
  questionText: String,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  choices: Seq[ChoiceDto],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto
    with HasChoices

case class MultipleSelectDto(
  questionText: String,
  override val scoringOption: QuestionScoringOption,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  choices: Seq[ChoiceDto],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto
    with HasChoices

case class TrueFalseDto(
  questionText: String,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  choices: Seq[ChoiceDto],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto
    with HasChoices

case class ChoiceDto(choiceText: String, correct: Option[Boolean], points: Option[Double], rationales: Seq[Rationale]):

  override def equals(obj: scala.Any): Boolean =
    obj match
      case dto: ChoiceDto if dto.choiceText == choiceText && dto.correct == correct =>
        (dto.points, points) match
          case (Some(thatPoints), Some(thisPoints)) if Math.abs(thatPoints - thisPoints) < Score.Epsilon => true
          case (None, None)                                                                              => true
          case _                                                                                         => false
      case _                                                                        => false
end ChoiceDto

case class EssayDto(
  questionText: String,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  rubric: Option[AssessmentRubric],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto:
  override val scoringOption: QuestionScoringOption = QuestionScoringOption.AllowPartialCredit
  override val manuallyGraded: Boolean              = true
end EssayDto

case class SequencingDto(
  questionText: String,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  choices: Seq[SequencingChoice],
  correctOrder: Seq[Int],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto

case class MatchingDto(
  questionText: String,
  override val scoringOption: QuestionScoringOption,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  terms: Seq[Term],
  definitions: Seq[Definition],
  correctDefinitionForTerm: Map[Int, Int],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto

case class BinDropDto(
  questionText: String,
  override val scoringOption: QuestionScoringOption,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  bins: Seq[Bin],
  binOptions: Seq[BinOption],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto

case class HotspotDto(
  questionText: String,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  choices: Seq[HotspotChoiceDto],
  includesCorrect: Boolean,
  reference: VersionedAssetReference,
  image: Option[VersionedAssetReference]
) extends QuestionDto

case class HotspotChoiceDto(x: Double, y: Double, shape: Shape, points: Option[Double], correct: Option[Boolean]):
  override def equals(obj: scala.Any): Boolean = obj match
    case dto: HotspotChoiceDto if dto.shape == shape && dto.correct == correct =>
      val coordsMatch: Boolean = Math.abs(dto.x - x) < Score.Epsilon && Math.abs(dto.y - y) < Score.Epsilon
      (dto.points, points) match
        case (Some(thatPoints), Some(thisPoints)) if Math.abs(thatPoints - thisPoints) < Score.Epsilon && coordsMatch =>
          true
        case (None, None) if coordsMatch                                                                              => true
        case _                                                                                                        => false
    case _                                                                     => false
end HotspotChoiceDto

case class ShortAnswerDto(
  questionText: String,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  correctAnswer: Option[String],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto

case class FillInTheBlankDto(
  questionText: String,
  override val scoringOption: QuestionScoringOption,
  pointsPossible: Double,
  rationales: Seq[Rationale],
  competencies: Seq[QuestionCompetencyDto],
  blanks: Seq[Blank],
  includesCorrect: Boolean,
  reference: VersionedAssetReference
) extends QuestionDto
