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

package loi.cp.quiz.attempt

import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import loi.cp.quiz.attempt.DistractorOrder.*
import loi.cp.quiz.attempt.exceptions.InvalidSelectionOrderingException
import loi.cp.quiz.attempt.selection.{ChoiceSelection, GroupingSelection, OrderingSelection, QuestionResponseSelection}
import loi.cp.quiz.question.bindrop.{Bin, BinOption}
import loi.cp.quiz.question.matching.{Definition, Term}

import scala.util.{Failure, Random, Success, Try}

/** A specific order for distractors for a single usage of a question in an attempt.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "_type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[AuthoredOrder], name = DistractorOrder.AUTHORED),
    new JsonSubTypes.Type(value = classOf[DistractorIndexList], name = DistractorOrder.LIST),
    new JsonSubTypes.Type(value = classOf[BinDropDistractorOrder], name = DistractorOrder.BIN_DROP),
    new JsonSubTypes.Type(value = classOf[MatchingDistractorOrder], name = DistractorOrder.MATCHING)
  )
)
sealed trait DistractorOrder:
  def _type: String

  /** Convert a display response into a version with selections that match the underlying authored question. This should
    * never be used to prepare a user-facing selection, as this will cause the selections to be non-randomized.
    *
    * @param selection
    *   The selection to be converted into authored order.
    * @return
    *   The given selection with the selections in authored order.
    */
  def toAuthoredOrder(selection: DisplayResponseSelection): Try[AuthoredResponseSelection]

  /** Convert an authored response into a version with selections that incorporate the orderings described by the
    * current distractor order.
    *
    * @param selection
    *   The selection with orders that are in line with the authored information.
    * @return
    *   The selections ordered by the given distractor order.
    */
  def toDisplayOrder(selection: AuthoredResponseSelection): Try[DisplayResponseSelection]
end DistractorOrder

object DistractorOrder:

  /** an index referring to a distractor's index in the authored order */
  type AuthoredIndex = Int

  /** an index referring to a distractor's index in the response order */
  type ResponseIndex = Int

  /** A type indicating that the selection has been properly ordered based on a distractor order.
    */
  type DisplayResponseSelection <: QuestionResponseSelection

  /** A type indicating that the selection has been converted to be in line with the authored question ordering.
    */
  type AuthoredResponseSelection <: QuestionResponseSelection

  final val AUTHORED = "authored"
  final val LIST     = "list"
  final val BIN_DROP = "binDrop"
  final val MATCHING = "matching"

  def randomIndices(elements: Seq[?]): Seq[Int] = Random.shuffle(elements.indices.toList)

  /** Create a mapping from an authored index to the resulting response index.
    *
    * @param responseIndices
    *   The Response index ordering.
    * @return
    *   Mapping from authored index to response index.
    */
  def authoredOrderToResponseOrder(responseIndices: Seq[ResponseIndex]): Map[AuthoredIndex, ResponseIndex] =
    responseIndices.indices.map(idx => idx -> responseIndices.indexOf(idx)).toMap

  /** Return the authored order elements in the specified response order.
    *
    * @param authoredOrderElements
    *   The elements to be rearranged.
    * @param responseOrder
    *   The order the elements should be arranged into.
    * @tparam A
    * @return
    *   The rearranged elements.
    */
  def elementsInResponseOrder[A](authoredOrderElements: Seq[A], responseOrder: Seq[ResponseIndex]): Seq[A] =
    responseOrder.map(authoredOrderElements)

  implicit class SelectionResponseOrderOps(val selection: QuestionResponseSelection) extends AnyVal:

    /** Fiats that the given [[QuestionResponseSelection]] is ordered relative to the authored question. Do NOT use this
      * to 'convert' from a [[DisplayResponseSelection]]. To convert from [[DisplayResponseSelection]] to
      * [[AuthoredResponseSelection]], use {{toAuthoredOrder}} from the appropriate [[DistractorOrder]] for the use of
      * the question.
      *
      * @return
      *   the response cast as [[AuthoredResponseSelection]]
      */
    def tagAsAuthoredOrder: AuthoredResponseSelection =
      selection.asInstanceOf[AuthoredResponseSelection]

    /** Fiats that the given [[QuestionResponseSelection]] is ordered relative to the randomized display order. Do NOT
      * use this to 'convert' from a [[AuthoredResponseSelection]]. To convert from [[AuthoredResponseSelection]] to
      * [[DisplayResponseSelection]], use {{toDisplayOrder}} from the appropriate [[DistractorOrder]] for the use of the
      * question.
      *
      * @return
      *   the response cast as [[DisplayResponseSelection]]
      */
    def tagAsDisplayOrder: DisplayResponseSelection =
      selection.asInstanceOf[DisplayResponseSelection]
  end SelectionResponseOrderOps
end DistractorOrder

/** A [[DistractorOrder]] signifying that no alterations from the authored order should be done. All question must
  * support this DistractorOrder.
  *
  * Authored order will keep the same selection ordering when converting to and from display and authored order.
  */
case class AuthoredOrder()
    extends ChoiceDistractorOrderType
    with BinDropDistractorOrderType
    with MatchingDistractorOrderType:
  import DistractorOrder.*
  override def _type: String = DistractorOrder.AUTHORED

  override def toAuthoredOrder(selection: DisplayResponseSelection): Try[AuthoredResponseSelection] =
    Success(selection.asInstanceOf[AuthoredResponseSelection])

  override def toDisplayOrder(selection: AuthoredResponseSelection): Try[DisplayResponseSelection] =
    Success(selection.asInstanceOf[DisplayResponseSelection])
end AuthoredOrder

object AuthoredOrder:
  // We need authored order to have an actual type so that Jackson can play with it, otherwise AuthoredOrder would be an
  // object
  val instance: AuthoredOrder = AuthoredOrder()

/** A single distractor list with a randomized collection of [[indices]]. If you zip the {{indices}} array with the
  * index in the array, you will have an array of (responseIndex, authoredIndex). That is, if the elements are [A, B, C]
  * in authored order, and [C, A, B] in response order, the {{indices}} are [2, 0, 1]. The 0th index of {{indices}} (the
  * display order) is 2, which means the 2th element of the authored order (C) is the 0th element of the display order.
  * i.e. each element in {{indices}} tell you where to pull the element from in the authored order.
  *
  * @param indices
  *   the index of the element in authored order
  */
case class DistractorIndexList(indices: Seq[AuthoredIndex]) extends ChoiceDistractorOrderType:
  import DistractorOrder.*
  override def _type: String = DistractorOrder.LIST

  /** Get the given elements in the distractor index list's specified order.
    *
    * @param authoredOrderElements
    *   The authored element order.
    * @tparam A
    * @return
    *   The rearranged elements.
    */
  def toResponseOrder[A](authoredOrderElements: Seq[A]): Seq[A] =
    elementsInResponseOrder(authoredOrderElements, indices)

  def toAuthoredIndexMap: Map[AuthoredIndex, ResponseIndex] = authoredOrderToResponseOrder(indices)

  /** Convert the selection to authored order.
    *
    * Example: Given a distractor index list of (2, 1, 3, 0) and a selection of (2, 3), the authored order of the
    * selection will be (3, 0), because those are the values stored at the 2nd and 3rd indices of the distractor list
    * respectively.
    *
    * @param selection
    *   The selection to be converted into authored order.
    * @return
    *   The given selection with the selections in authored order.
    */
  def toAuthoredOrder(selection: DisplayResponseSelection): Try[AuthoredResponseSelection] = selection match
    case choice @ ChoiceSelection(_, _, _, sel)  =>
      Success(choice.copy(selectedIndexes = sel.map(idx => indices(idx))).asInstanceOf[AuthoredResponseSelection])
    case ord @ OrderingSelection(_, _, _, order) =>
      Success(ord.copy(order = order.map(idx => indices(idx))).asInstanceOf[AuthoredResponseSelection])
    case _                                       => Failure(InvalidSelectionOrderingException(selection, this))

  /** Convert the selection to display order.
    *
    * Example: Given a distractor index list of (2, 1, 3, 0) and an authored selection of (3, 0), the display order will
    * be (2, 3) because those are the indices of the values 3 and 0 in the distractor list.
    *
    * @param selection
    *   The selection with orders that are in line with the authored information.
    * @return
    *   The selections ordered by the given distractor order.
    */
  override def toDisplayOrder(selection: AuthoredResponseSelection): Try[DisplayResponseSelection] = selection match
    case choice @ ChoiceSelection(_, _, _, sel)  =>
      Success(
        choice.copy(selectedIndexes = sel.map(idx => indices.indexOf(idx))).asInstanceOf[DisplayResponseSelection]
      )
    case ord @ OrderingSelection(_, _, _, order) =>
      Success(ord.copy(order = order.map(idx => indices.indexOf(idx))).asInstanceOf[DisplayResponseSelection])
    case _                                       => Failure(InvalidSelectionOrderingException(selection, this))
end DistractorIndexList

/** A distractor order for a classification question with distinct ordering for the bins and options. If you zip the
  * {{indices}} array with the index in the array, you will have an array of (responseIndex, authoredIndex). See
  * [[DistractorIndexList]] for how each of the individual {{bins}} and {{options}} list operate.
  *
  * @param bins
  *   the index of the bin in authored order
  * @param options
  *   the index of the option in authored order
  */
case class BinDropDistractorOrder(bins: Seq[AuthoredIndex], options: Seq[AuthoredIndex])
    extends BinDropDistractorOrderType:
  import DistractorOrder.*
  override def _type: String = DistractorOrder.BIN_DROP

  /** Convert the selection to authored order.
    *
    * Example: Given a bin drop distractor order with bins (2, 1, 0) and options (1, 2, 0), and a selection of {0: [1,
    * 2], 1: [0], 2: []}, the authored order will be {2: [2, 0], 1: [1], 0: []} because those are the values at the
    * respective indices.
    *
    * @param selection
    *   The selction to be converted into authored order.
    * @return
    *   The given selection with the selections in authored order.
    */
  override def toAuthoredOrder(selection: DisplayResponseSelection): Try[AuthoredResponseSelection] = selection match
    case grp @ GroupingSelection(_, _, _, elems) =>
      val updatedElems: Map[ResponseIndex, Seq[ResponseIndex]] = elems.toSeq
        .map({ case (keyIdx, selected) =>
          bins(keyIdx) -> selected.map(idx => options(idx))
        })
        .toMap
      Success(grp.copy(elementIndexesByGroupIndex = updatedElems).asInstanceOf[AuthoredResponseSelection])
    case _                                       => Failure(InvalidSelectionOrderingException(selection, this))

  /** Convert the selection to display order.
    *
    * Example: Given a bin drop distractor order with bins (2, 1, 0) and options (1, 2, 0) and a selection of {2: [2,
    * 0], 1: [1], 0: []}, the display order will be {0: [1, 2], 1: [0], 2}, because those are the indices of each
    * respective value.
    *
    * @param selection
    *   The selection with orders that are in line with the authored information.
    * @return
    *   The selections ordered by the given distractor order.
    */
  override def toDisplayOrder(selection: AuthoredResponseSelection): Try[DisplayResponseSelection] = selection match
    case grp @ GroupingSelection(_, _, _, elems) =>
      val updatedElems: Map[ResponseIndex, Seq[ResponseIndex]] = elems.toSeq
        .map({ case (keyIdx, selected) =>
          bins.indexOf(keyIdx) -> selected.map(idx => options.indexOf(idx))
        })
        .toMap
      Success(grp.copy(elementIndexesByGroupIndex = updatedElems).asInstanceOf[DisplayResponseSelection])
    case _                                       => Failure(InvalidSelectionOrderingException(selection, this))

  /** Get the given bins in the distractor order's specified bin order.
    *
    * @param authoredOrderBins
    *   The bins in authored order.
    * @return
    *   The rearranged bins.
    */
  def binsInResponseOrder(authoredOrderBins: Seq[Bin]): Seq[Bin] =
    elementsInResponseOrder(authoredOrderBins, bins).map { bin =>
      val responseOrderOptionIndices: Set[AuthoredIndex] = bin.correctOptionIndices.map(options.indexOf)
      bin.copy(correctOptionIndices = responseOrderOptionIndices)
    }
  def authoredBinMap: Map[AuthoredIndex, ResponseIndex]          = authoredOrderToResponseOrder(bins)

  /** Get the given options in the distractor order's specified option order.
    *
    * @param authoredOrderOptions
    *   The options in authored order.
    * @return
    *   The rearranged options.
    */
  def optionsInResponseOrder(authoredOrderOptions: Seq[BinOption]): Seq[BinOption] =
    elementsInResponseOrder(authoredOrderOptions, options)
  def authoredOptionMap: Map[AuthoredIndex, ResponseIndex]                         = authoredOrderToResponseOrder(options)
end BinDropDistractorOrder

/** A distractor order for a matching question with distinct ordering for the terms and definitions. If you zip the
  * indices of the {{terms}} or {{definitions}} array with the index in the array, you will have an array of
  * (responseIndex, authoredIndex). See [[DistractorIndexList]] for how each of the individual {{terms}} and
  * {{definitions}} list operate.
  *
  * @param terms
  *   the index of the term in authored order
  * @param definitions
  *   the index of the definition in authored order
  */
case class MatchingDistractorOrder(terms: Seq[AuthoredIndex], definitions: Seq[AuthoredIndex])
    extends MatchingDistractorOrderType:
  import DistractorOrder.*
  override def _type: String = DistractorOrder.MATCHING

  /** Get the given terms in the distractor order's specified term order.
    *
    * @param authoredOrderTerms
    *   The terms in authored order.
    * @return
    *   The rearranged terms.
    */
  def termsInResponseOrder(authoredOrderTerms: Seq[Term]): Seq[Term] =
    elementsInResponseOrder(authoredOrderTerms, terms)
  def authoredTermMap: Map[AuthoredIndex, ResponseIndex]             = authoredOrderToResponseOrder(terms)

  /** Get the given definitions in the distractor order's specified definition order.
    *
    * @param authoredOrderDefs
    *   The definitions in authored order.
    * @return
    *   The rearranged definitions.
    */
  def definitionsInResponseOrder(authoredOrderDefs: Seq[Definition]): Seq[Definition] =
    elementsInResponseOrder(authoredOrderDefs, definitions)
  def authoredDefinitionMap: Map[AuthoredIndex, ResponseIndex]                        = authoredOrderToResponseOrder(definitions)

  /** Convert the selection to authored order from display order.
    *
    * Example: Given a matching distractor order with terms (2, 1, 0) and definitions (1, 2, 0), and a selection of {0:
    * [2], 1: [0], 2: [1]}, the authored order will be {2: [0], 1: [1], 0: [2]} because those are the values at the
    * respective indices.
    *
    * @param selection
    *   The selction to be converted into authored order.
    * @return
    *   The given selection with the selections in authored order.
    */
  override def toAuthoredOrder(selection: DisplayResponseSelection): Try[AuthoredResponseSelection] = selection match
    case grp @ GroupingSelection(_, _, _, elems) =>
      val updatedElems: Map[ResponseIndex, Seq[ResponseIndex]] = elems.toSeq
        .map({ case (keyIdx, selected) =>
          terms(keyIdx) -> selected.map(idx => definitions(idx))
        })
        .toMap
      Success(grp.copy(elementIndexesByGroupIndex = updatedElems).asInstanceOf[AuthoredResponseSelection])
    case _                                       => Failure(InvalidSelectionOrderingException(selection, this))

  /** Convert the selection from authored response order to display response order.
    *
    * Example: Given a matching distractor order with terms (2, 1, 0) and definitions (1, 2, 0), and a selection of {2:
    * [0], 1: [1], 0: [2]}, the display order will be {0: [2], 1: [0], 2: [1]} because those are the indices of the
    * respective values.
    *
    * @param selection
    *   The selection with orders that are in line with the authored information.
    * @return
    *   The selections ordered by the given distractor order.
    */
  override def toDisplayOrder(selection: AuthoredResponseSelection): Try[DisplayResponseSelection] =
    selection match
      case grp @ GroupingSelection(_, _, _, elems) =>
        val updatedElems: Map[ResponseIndex, Seq[ResponseIndex]] = toDisplayOrder(elems)
        Success(grp.copy(elementIndexesByGroupIndex = updatedElems).asInstanceOf[DisplayResponseSelection])
      case _                                       => Failure(InvalidSelectionOrderingException(selection, this))

  def toDisplayOrder(elementIndexesByGroupIndex: Map[Int, Seq[Int]]): Map[ResponseIndex, Seq[ResponseIndex]] =
    elementIndexesByGroupIndex.toSeq
      .map({ case (keyIdx, selected) =>
        authoredTermMap(keyIdx) -> selected.map(idx => authoredDefinitionMap(idx))
      })
      .toMap
end MatchingDistractorOrder

sealed trait ChoiceDistractorOrderType   extends DistractorOrder
sealed trait BinDropDistractorOrderType  extends DistractorOrder
sealed trait MatchingDistractorOrderType extends DistractorOrder
