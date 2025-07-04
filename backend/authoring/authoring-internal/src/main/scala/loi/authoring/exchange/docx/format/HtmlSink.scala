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

import java.util.regex.Pattern

import com.learningobjects.cpxp.util.HtmlUtils.toPlaintext
import org.apache.commons.lang3.StringUtils.{isBlank, *}
import org.apache.poi.xwpf.usermodel.*

import scala.jdk.CollectionConverters.*

private[docx] sealed trait HtmlSink:
  private[docx] def addNewParagraph(): Unit
  private[docx] def lastParagraph: XWPFParagraph
  private[docx] def cleanup(): Unit

private[exchange] object HtmlSink:
  def apply(doc: XWPFDocument): HtmlSink =
    new HtmlSink:
      override def addNewParagraph(): Unit      = doc.createParagraph()
      override def lastParagraph: XWPFParagraph =
        doc.getParagraphs.asScala.lastOption.getOrElse(doc.createParagraph())
      override def cleanup(): Unit =
        // MS Word always expects at least one paragraph or else you get
        // "unreadable content" erros in Word
        if doc.getParagraphs.size > 1 && isBlank(lastParagraph.getText) then
          doc.removeBodyElement(doc.getPosOfParagraph(lastParagraph))

  def apply(cell: XWPFTableCell): HtmlSink =
    new HtmlSink:
      override def addNewParagraph(): Unit      = cell.addParagraph()
      override def lastParagraph: XWPFParagraph =
        cell.getParagraphs.asScala.lastOption.getOrElse(cell.addParagraph())
      override def cleanup(): Unit =
        // MS Word always expects at least one paragraph or else you get
        // "unreadable content" erros in Word
        if cell.getParagraphs.size > 1 && isBlank(lastParagraph.getText) then
          cell.removeParagraph(cell.getParagraphs.size() - 1)

  implicit class HtmlSinkOps(private val doc: HtmlSink) extends AnyVal:
    def addHtml(html: String): Unit =
      if isNotBlank(html) then
        val buf                                = new StringBuilder()
        def append(oldTags: Set[RunTag]): Unit =
          val txt = buf.toString().replaceAll("<span.*>", "")
          if isNotBlank(txt) then
            val lastRun = doc.lastParagraph.getRuns.asScala.lastOption.getOrElse(
              doc.lastParagraph.createRun()
            )
            oldTags.foreach(_.applyTo(lastRun))
            lastRun.setText(txt)
            buf.clear()

        var bufEndsInEscape = false
        var tagFound        = false
        val tags            = new net.htmlparser.jericho.Source(html)
          .iterator()
          .asScala
          .foldLeft(DocxSegmentState()) { case (s0, seg) =>
            val s1 = DocxSegmentState.nextState(s0, seg)
            if buf.nonEmpty then                                             // already in paragraph and text queued
              if s0.pLevel != s1.pLevel then                                 // p tag encountered
                append(s0.tags)
                doc.addNewParagraph()
              else if s0.tags != s1.tags then // recognized run tag
                append(s0.tags)
                doc.lastParagraph.createRun()
                tagFound = true
            else if s0.pLevel < s1.pLevel && !doc.lastParagraph.isEmpty then // root level
              doc.addNewParagraph()
              tagFound = false
            end if
            s1.txt.filter(isNotBlank).foreach { t =>
              val isEscape = EscapePat.matcher(t).matches()
              if buf.nonEmpty && buf.last != ' ' && !t.startsWith(" ")
                && !(isEscape || bufEndsInEscape)
              then buf.append(" ")
              val text     =
                if buf.nonEmpty || tagFound || !t.startsWith(" ") then t
                else stripStart(t, null)
              buf.append(if isEscape then toPlaintext(text) else text)
              bufEndsInEscape = isEscape
              tagFound = false
            }
            s1
          }
          .tags
        append(tags)
        doc.cleanup()
  end HtmlSinkOps

  // these are necessary because Jericho considers escape sequences their
  // own segments
  private val EscapePat = Pattern.compile("&(#\\d+|[a-z]+);$", Pattern.CASE_INSENSITIVE)
end HtmlSink
