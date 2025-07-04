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

package loi.authoring.exchange.docx

import enumeratum.*
import loi.authoring.exchange.docx.writeops.*

import scala.collection.immutable.IndexedSeq

private[docx] sealed abstract class DocxQuestionType(
  override val entryName: String
) extends EnumEntry
    with TableToWriteOps

private[docx] object DocxQuestionType extends Enum[DocxQuestionType]:

  override def values: IndexedSeq[DocxQuestionType] = findValues

//  case object Classification extends DocxQuestionType("Classification")
//  case object Essay          extends DocxQuestionType("Essay")
  case object FillInTheBlank extends DocxQuestionType("FB") with FillInTheBlankWriteOps
//  case object Matching       extends DocxQuestionType("Matching")
  case object MultipleChoice extends DocxQuestionType("MC") with MultipleChoiceToWriteOps
  case object MultipleSelect extends DocxQuestionType("MS") with MultipleSelectToWriteOps
//  case object Ordering       extends DocxQuestionType("Ordering")
  case object TrueFalse      extends DocxQuestionType("TF") with TrueFalseToWriteOps
end DocxQuestionType
