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

package loi.cp.quiz.question
package api

import enumeratum.{Enum, EnumEntry}
import loi.asset.question.QuestionScoringOption
import loi.cp.quiz.attempt.DistractorOrder.{AuthoredIndex, ResponseIndex}
import loi.cp.quiz.attempt.*
import loi.cp.quiz.attempt.exceptions.MismatchedDistractorOrderException
import loi.cp.quiz.question.bindrop.{Bin, BinDrop, BinOption}
import loi.cp.quiz.question.choice.{Choice, MultipleChoice, MultipleSelect, TrueFalse}
import loi.cp.quiz.question.essay.Essay
import loi.cp.quiz.question.fillintheblank.FillInTheBlank
import loi.cp.quiz.question.hotspot.{Hotspot, HotspotChoice}
import loi.cp.quiz.question.matching.{Definition, Matching, Term}
import loi.cp.quiz.question.ordering.Sequencing
import loi.cp.quiz.question.shortanswer.ShortAnswer
import scaloi.syntax.option.*

object QuestionDtoUtils:

  def dto(
    question: Question,
    includeCorrect: Boolean,
    includedQuestionRationale: QuestionRationaleDisplay,
    includeDistractorRationale: Boolean,
    distractorOrder: DistractorOrder
  ): QuestionDto =
    val filteredRationales: Seq[Rationale] =
      QuestionRationaleDisplay.filter(question.rationales, includedQuestionRationale)

    question match
      case e: Essay =>
        EssayDto(
          e.text.getOrElse(""),
          e.pointValue,
          filteredRationales,
          QuestionCompetencyDto(e.competencies),
          e.rubric,
          includeCorrect,
          question.contentReference
        )

      case o: Sequencing =>
        val correctSequence: Seq[Int] = (distractorOrder match
          case DistractorIndexList(indices) => indices
          case _: AuthoredOrder             => o.correctOrder
          case _                            => throw new MismatchedDistractorOrderException(question, distractorOrder)
        ).filter(_ => includeCorrect)

        SequencingDto(
          o.text.getOrElse(""),
          o.pointValue,
          filteredRationales,
          QuestionCompetencyDto(o.competencies),
          sortDistractorList(o, o.choices, distractorOrder),
          correctSequence,
          includeCorrect,
          question.contentReference
        )

      case m: Matching =>
        val filteredCorrectDefForTerm: Map[Int, Int]                  =
          if includeCorrect then m.correctDefinitionForTerm
          else Map.empty
        val (orderedTerms, orderedDefs, validatedResponseOrderAnswer) =
          distractorOrder match
            case _: AuthoredOrder               => (m.terms, m.definitions, filteredCorrectDefForTerm)
            case order: MatchingDistractorOrder =>
              val orderedTerms: Seq[Term]      = order.termsInResponseOrder(m.terms)
              val orderedDefs: Seq[Definition] = order.definitionsInResponseOrder(m.definitions)

              val authoredTermOrderToResponseTermOrder: Map[AuthoredIndex, ResponseIndex] = order.authoredTermMap
              val authoredDefOrderToResponseTermOrder: Map[AuthoredIndex, ResponseIndex]  = order.authoredDefinitionMap

              val orderedCorrectDefForTerm: Map[Int, Int] =
                filteredCorrectDefForTerm
                  .map { case (authoredTermIdx, authoredDefIdx) =>
                    authoredTermOrderToResponseTermOrder.get(authoredTermIdx) ->
                      authoredDefOrderToResponseTermOrder.get(authoredDefIdx)
                  }
                  .collect {
                    // Do not collect if we've deleted something and it is not there (thank you HW migration)
                    case (Some(term), Some(definition)) => term -> definition
                  }

              (orderedTerms, orderedDefs, orderedCorrectDefForTerm)
            case _                              => throw new MismatchedDistractorOrderException(question, distractorOrder)
        MatchingDto(
          m.text.getOrElse(""),
          QuestionScoringOption.ofPartialCredit(m.awardsPartialCredit),
          m.pointValue,
          filteredRationales,
          QuestionCompetencyDto(m.competencies),
          orderedTerms,
          orderedDefs,
          validatedResponseOrderAnswer,
          includeCorrect,
          question.contentReference
        )

      case b: BinDrop =>
        val (orderedBins, orderedOptions) =
          distractorOrder match
            case _: AuthoredOrder              => (b.bins.map(_.filterCorrect(includeCorrect)), b.options)
            case order: BinDropDistractorOrder =>
              val orderedBinsWithCorrectOptionIndices: Seq[Bin] = order.binsInResponseOrder(b.bins)
              val orderedOptions: Seq[BinOption]                = order.optionsInResponseOrder(b.options)
              (orderedBinsWithCorrectOptionIndices, orderedOptions)
            case _                             => throw new MismatchedDistractorOrderException(question, distractorOrder)
        BinDropDto(
          b.text.getOrElse(""),
          QuestionScoringOption.ofPartialCredit(b.awardsPartialCredit),
          b.pointValue,
          filteredRationales,
          QuestionCompetencyDto(b.competencies),
          orderedBins.map(_.filterCorrect(includeCorrect)),
          orderedOptions,
          includeCorrect,
          question.contentReference
        )

      case mc: MultipleChoice =>
        val orderedChoices: Seq[ChoiceDto] =
          sortDistractorList(mc, mc.choices, distractorOrder).map(_.toDto(includeCorrect, includeDistractorRationale))
        MultipleChoiceDto(
          mc.text.getOrElse(""),
          pointsPossible = mc.pointValue,
          filteredRationales,
          QuestionCompetencyDto(mc.competencies),
          orderedChoices,
          includeCorrect,
          question.contentReference
        )

      case ms: MultipleSelect =>
        val orderedChoices: Seq[ChoiceDto] =
          sortDistractorList(ms, ms.choices, distractorOrder).map(_.toDto(includeCorrect, includeDistractorRationale))
        MultipleSelectDto(
          ms.text.getOrElse(""),
          ms.scoringOption,
          ms.pointValue,
          filteredRationales,
          QuestionCompetencyDto(ms.competencies),
          orderedChoices,
          includeCorrect,
          question.contentReference
        )

      case tf: TrueFalse =>
        val orderedChoices: Seq[ChoiceDto] =
          sortDistractorList(tf, tf.choices, distractorOrder).map(_.toDto(includeCorrect, includeDistractorRationale))
        TrueFalseDto(
          tf.text.getOrElse(""),
          tf.pointValue,
          filteredRationales,
          QuestionCompetencyDto(tf.competencies),
          orderedChoices,
          includeCorrect,
          question.contentReference
        )

      case h: Hotspot =>
        HotspotDto(
          h.text.getOrElse(""),
          h.pointValue,
          filteredRationales,
          QuestionCompetencyDto(h.competencies),
          h.choices.map(_.toDto(includeCorrect)),
          includeCorrect,
          question.contentReference,
          h.image
        )

      case sa: ShortAnswer =>
        ShortAnswerDto(
          sa.text.getOrElse(""),
          sa.pointValue,
          filteredRationales,
          QuestionCompetencyDto(sa.competencies),
          Option(sa.answer).filter(_ => includeCorrect),
          includeCorrect,
          question.contentReference
        )

      case fitb: FillInTheBlank =>
        FillInTheBlankDto(
          fitb.text.getOrElse(""),
          QuestionScoringOption.ofPartialCredit(fitb.awardsPartialCredit),
          fitb.pointValue,
          filteredRationales,
          QuestionCompetencyDto(fitb.competencies),
          fitb.blanks.map(blank => if includeCorrect then blank else blank.copy(answers = Nil)),
          includeCorrect,
          question.contentReference
        )

      // This really shouldn't happen. If we seal over all question types, we can avoid having to do this.
      case shouldntHappen => throw new MissingQuestionException(shouldntHappen)
    end match
  end dto

  private def sortDistractorList[A](question: Question, elements: Seq[A], distractorOrder: DistractorOrder): Seq[A] =
    distractorOrder match
      case _: AuthoredOrder       => elements
      case d: DistractorIndexList => d.toResponseOrder(elements)
      case _                      => throw new MismatchedDistractorOrderException(question, distractorOrder)

  private implicit class BinFilterOps(bin: Bin):
    def filterCorrect(includeCorrect: Boolean): Bin =
      if includeCorrect then bin
      else bin.copy(correctOptionIndices = Set.empty)

  private implicit class HotspotChoiceToDtoOps(choice: HotspotChoice):
    def toDto(includeCorrect: Boolean): HotspotChoiceDto =
      HotspotChoiceDto(
        choice.x,
        choice.y,
        choice.shape,
        Option(choice.points).filter(_ => includeCorrect),
        Option(choice.correct).filter(_ => includeCorrect)
      )

  private implicit class ChoiceToDtoOps(choice: Choice):
    def toDto(includeCorrect: Boolean, includeRationale: Boolean): ChoiceDto =
      val filteredRationale: Seq[Rationale] =
        Some(choice.rationales).when(includeRationale).getOrElse(Nil)

      ChoiceDto(
        choice.text.renderedHtml.getOrElse(""),
        Option(choice.correct).filter(_ => includeCorrect),
        Option(choice.points).filter(_ => includeCorrect),
        filteredRationale
      )
    end toDto
  end ChoiceToDtoOps

  implicit class QuestionToDtoOps(question: Question):

    /** Builds a REST object for this question given a certain set of parameters.
      *
      * @param includeCorrect
      *   whether or not the correct answer should be filtered out of the REST object
      * @param includeQuestionRationale
      *   which [[Rationale]] to include in the REST object
      * @param includeDistractorRationale
      *   whether to include the [[Rationale]] for any distractors
      * @param distractorOrder
      *   what distractor order to apply to the question for presenting in display order
      * @return
      *   a REST object with potentially filtered value, in display order
      */
    def toDto(
      includeCorrect: Boolean,
      includeQuestionRationale: QuestionRationaleDisplay,
      includeDistractorRationale: Boolean,
      distractorOrder: DistractorOrder = AuthoredOrder.instance
    ): QuestionDto =
      dto(question, includeCorrect, includeQuestionRationale, includeDistractorRationale, distractorOrder)
  end QuestionToDtoOps
end QuestionDtoUtils

/** A request object specifying which of the [[Rationale]] to include with the overall question in the DTO.
  */
sealed abstract class QuestionRationaleDisplay extends EnumEntry

case object QuestionRationaleDisplay extends Enum[QuestionRationaleDisplay]:
  val values = findValues

  /** the DTO should include all [[Rationale]] for the question */
  case object All extends QuestionRationaleDisplay

  /** the DTO should include only [[Rationale]] for correct answers */
  case object OnlyCorrect extends QuestionRationaleDisplay

  /** the DTO should include only [[Rationale]] for incorrect answers */
  case object OnlyIncorrect extends QuestionRationaleDisplay

  /** the DTO should not include any [[Rationale]] */
  case object NoRationale extends QuestionRationaleDisplay

  /** Filters the given collection of [[Rationale]] based on the request.
    *
    * @param rationale
    *   the [[Rationale]] to filter
    * @param including
    *   the request to apply
    * @return
    *   the filtered rationale
    */
  def filter(rationale: Seq[Rationale], including: QuestionRationaleDisplay): Seq[Rationale] =
    including match
      case All => rationale

      case OnlyCorrect =>
        rationale collect { case r: CorrectRationale =>
          r
        }

      case OnlyIncorrect =>
        rationale collect { case r: Remediation =>
          r
        }

      case NoRationale =>
        Nil
end QuestionRationaleDisplay
