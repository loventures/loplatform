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

import RunTag.*
import net.htmlparser.jericho.*
import net.htmlparser.jericho.HTMLElementName.*
import org.apache.commons.lang3.StringUtils.isNotEmpty
import scalaz.syntax.std.boolean.*
import scalaz.{-\/, \/, \/-}

private[docx] case class DocxSegmentState(
  txt: Option[String] = None,
  tags: Set[RunTag] = Set.empty,
  pLevel: Int = 0
)

private[docx] object DocxSegmentState:
  type SegmentData = Tag \/ String

  def nextState(pState: DocxSegmentState, seg: Segment): DocxSegmentState =
    segmentData(seg).fold(
      nextTagState(_, pState),
      t => pState.copy(txt = isNotEmpty(t).option(t))
    )

  private def segmentData(s: Segment): SegmentData =
    s match
      case t: Tag => -\/(t)
      case _      => \/-(s.toString.replaceAll("\\s+", " "))

  private def nextTagState(tag: Tag, pState: DocxSegmentState) =
    val state0 = pState.copy(txt = None)
    if tag.getName == P then
      val fn: Int => Int =
        if tag.isInstanceOf[StartTag] then _ + 1
        else if pState.pLevel > 0 then _ - 1
        else i => i
      state0.copy(pLevel = fn(pState.pLevel))
    else
      RunTag
        .withNameOption(tagName(tag))
        .map { rt =>
          val fn: Set[RunTag] => Set[RunTag] =
            if tag.isInstanceOf[StartTag] then _ + rt
            else _ - rt
          state0.copy(tags = fn(pState.tags))
        }
        .getOrElse(state0)
    end if
  end nextTagState

  private def tagName(tag: Tag) =
    tag.getName match
      case B         => Strong.entryName
      case I         => Em.entryName
      case U         => Ins.entryName
      case STRIKE    => Del.entryName
      case S         => Del.entryName
      case s: String => s
end DocxSegmentState
