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

import loi.authoring.exchange.docx.Surround
import org.apache.commons.text.StringEscapeUtils
import org.apache.poi.xwpf.usermodel.*
import scalaz.syntax.std.boolean.*

import scala.jdk.CollectionConverters.*

private[exchange] object DocxFormat:

  def html(els: Seq[IBodyElement]): String =
    els.flatMap(elementHtml).mkString

  private def elementHtml(el: IBodyElement) =
    el match
      case p: XWPFParagraph => paragraphHtml(p)
      case _                => None

  private def paragraphHtml(p: XWPFParagraph) =
    val pHtml = p.getRuns.asScala.flatMap(runHtml).mkString.trim
    pHtml.nonEmpty.option(s"<p>\n$pHtml\n</p>\n")

  private def runHtml(r: XWPFRun) =
    Option(r.getText(0)).map { text =>
      val buf = new StringBuilder()
      val sur = Surround(r)
      sur.open.foreach(buf.append)

      buf.append(StringEscapeUtils.escapeHtml4(text))

      sur.close.foreach(buf.append)
      buf.toString()
    }
end DocxFormat
