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

package loi.authoring.exchange.docx.format

import enumeratum.EnumEntry.*
import enumeratum.*
import org.apache.poi.xwpf.usermodel.{XWPFRun, UnderlinePatterns as UPat}
import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STVerticalAlignRun as VAl

import scala.collection.immutable.IndexedSeq

private[exchange] sealed abstract class RunTag(
  protected[docx] val inRun: XWPFRun => Boolean,
  protected[docx] val applyTo: XWPFRun => Unit
) extends EnumEntry
    with Lowercase

private[exchange] object RunTag extends Enum[RunTag]:
  override def values: IndexedSeq[RunTag] = findValues
  lazy val all: Set[RunTag]               = values.toSet

  case object Strong extends RunTag(_.isBold, _.setBold(true))
  case object Em     extends RunTag(_.isItalic, _.setItalic(true))
  case object Ins    extends RunTag(_.getUnderline != UPat.NONE, _.setUnderline(UPat.SINGLE))
  case object Del    extends RunTag(_.isStrikeThrough, _.setStrikeThrough(true))
  case object Sub    extends RunTag(_.getVerticalAlignment == VAl.SUBSCRIPT, _.setVerticalAlignment("subscript"))
  case object Sup    extends RunTag(_.getVerticalAlignment == VAl.SUPERSCRIPT, _.setVerticalAlignment("superscript"))
end RunTag
