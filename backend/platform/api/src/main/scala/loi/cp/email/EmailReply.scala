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

package loi.cp.email

import scalaz.syntax.std.boolean.*
import scaloi.syntax.regex.*

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

/** Representation of an email reply. Attempts to parse the reply into original text, quoted text and any signature
  * part(s).
  *
  * A port of https://github.com/github/email_reply_parser translated to be more tail recursive and less side-effected.
  *
  * @param sections
  *   the sections of the reply
  */
case class EmailReply(sections: List[EmailReply.Section]):

  /** Get the visible text from this reply. This strips all trailing quotes and signatures.
    * @return
    *   the visible text
    */
  def visibleText: String = sections.filterNot(_.hidden).map(_.lines.mkString("\n")).mkString("\n\n")

object EmailReply:

  /** Parse an email into its representation.
    * @param email
    *   the email body
    * @return
    *   the parsed reply
    */
  def apply(email: String): EmailReply = EmailReply(scan(fixOnWroteHeader(email.linesIterator).reverse))

  /** Fixes 'On...wrote' quotation headers (a standard email reply block indicator) which can have two problems.
    *
    * Case #1 - inline quote "Instructor reply message. On DATE, NAME <EMAIL> wrote:"
    *
    *   - Normally, we expect the 'On...wrote:' to be on a new line. If not, we will add one.
    *
    * Case #2 - multiline quote "On DATE, LONG NAME <LONG EMAIL> wrote:"
    *
    *   - In this case, the email client has added lines in between we weren't expecting. Remove the newlines.
    *
    * @param input
    *   the input lines
    * @param quoteHeader
    *   the multiline quotation header, if any
    * @param output
    *   the output accumulator
    * @return
    *   the input lines, with any fixes as needed
    */
  @tailrec
  private final def fixOnWroteHeader(
    input: Iterator[String],
    quoteHeader: ListBuffer[String] = ListBuffer.empty,
    output: ListBuffer[String] = ListBuffer.empty
  ): List[String] =
    if input.isEmpty then (output ++ quoteHeader).toList
    else
      val line = input.next()
      if OnRe.lookingAt(line) then      // 'On' is at the beginning of this line, what to do?
        if WroteRe.test(line) then
          // 'On...wrote' was only one line, no fixes needed
          fixOnWroteHeader(input, ListBuffer.empty, output ++ quoteHeader :+ line)
        else
          // 'On...wrote' spans multiple lines - start a new multiline quote header
          fixOnWroteHeader(input, ListBuffer(line), output ++ quoteHeader)
      else if quoteHeader.nonEmpty then // we encountered an 'On' on a recent line
        if WroteRe.test(line) then
          // 'wrote:' found, end of multi-line quote header
          val qh = (quoteHeader :+ line).mkString(" ")
          fixOnWroteHeader(input, ListBuffer.empty, output :+ qh)
        else if quoteHeader.size >= 4 then
          // too many lines, abandon multi-line quote header
          fixOnWroteHeader(input, ListBuffer.empty, output ++ quoteHeader :+ line)
        else
          // nothing yet, append this line to potential multi-line quote header
          fixOnWroteHeader(input, quoteHeader :+ line, output)
      else if !QuoteRe.lookingAt(line) && OnWroteRe.test(line) then
        // 'On..wrote' started midline, and was not a quote '>', split onto newline
        val (line1, line2) = line.splitAt(OnWroteRe.findFirstMatchIn(line).get.start)
        fixOnWroteHeader(input, ListBuffer.empty, output :+ line1 :+ line2)
      else
        // append the line to the output list
        fixOnWroteHeader(input, ListBuffer.empty, output :+ line)
      end if

  /** Builds the section string and reverses it, after all lines have been added. It also checks to see if this Section
    * is hidden. The hidden Section check reads from the bottom to the top.
    *
    * Any quoted Sections or signature Sections are marked hidden if they are below any visible Sections. Visible
    * Section are expected to contain original content by the author. If they are below a quoted Section, then the
    * Section should be visible to give context to the reply.
    *
    * some original text (visible)
    *
    * > do you have any two's? (quoted, visible)
    *
    * Go fish! (visible)
    *
    * > -- > Player 1 (quoted, hidden)
    *
    * -- Player 2 (signature, hidden)
    */
  @tailrec
  private final def scan(
    input: List[String],
    section: List[String] = Nil,
    output: List[Section] = Nil
  ): List[Section] = input match
    case Nil          =>
      section.isEmpty.fold(output, finish(section, output) :: output)
    case line :: rest =>
      val isBlank = line.trim.isEmpty
      // Mark the current Section as a signature if the current line is empty
      // and the Section starts with a common signature indicator.
      if isBlank && section.headOption.exists(SignatureRe.lookingAt) then
        scan(rest, Nil, finish(section, output, isSignature = true) :: output)
      else
        val isQuoteHeader = OnWroteRe.lookingAt(line)
        val isQuote       = QuoteRe.lookingAt(line)
        val wasQuote      = section.exists(s => QuoteRe.lookingAt(s) || OnWroteRe.lookingAt(s))
        // If this line matches the section - either neither is a quote, or the
        // section is a quote and the line is a quote, blank or quote header,
        // or the line is a quote header on top of a non quoted quote
        if section.isEmpty || wasQuote.fold(isQuote || isQuoteHeader || isBlank, isQuoteHeader || !isQuote) then
          // Then bang the line onto the section
          scan(rest, line :: section, output)
        else
          // Else finish this section and start a new one
          scan(rest, List(line), finish(section, output) :: output)
      end if

  /** Finish processing a section.
    *
    * This takes a list of lines (clustered by the scan function) and determines whether they are A) a quote
    *   - if the last line begins with a '>'
    *   - OR if the email has no '>', but starts with an 'On...wrote' string B) a signature (passed in) C) hidden
    *   - if this is the last section AND is a quote, blank, or signature
    *   - if the previous section was hidden AND this is blank, or a signature
    *
    * An example section looks like [ "", "On 9 Jan 2014, at 2:47, George Plymale wrote:", "", "> quoted text line 1" ">
    * quoted text line 2", ]
    *
    * @param lines
    *   the lines of the section
    * @param output
    *   the preceding (succeeding) output
    * @param isSignature
    *   whether this is a signature section
    * @return
    *   the new section
    */
  private def finish(lines: List[String], output: List[Section], isSignature: Boolean = false): Section =
    val isBlank     = lines.forall(_.trim.isEmpty)
    val isQuote     = lines.lastOption.exists(QuoteRe.lookingAt) ||
      lines.dropWhile(_.trim.isEmpty).headOption.exists(OnWroteRe.lookingAt)
    val isLastQuote = isQuote && !output.exists(_.quoted)
    val wasHidden   = output.headOption.forall(_.hidden) // also true if output is empty
    Section(lines, isQuote, signature = isSignature, hidden = wasHidden && (isLastQuote || isBlank || isSignature))

  /* The regular expressions. */

  private final val OnRe        = """\s*On\s""".r
  private final val WroteRe     = """wrote:\s*$""".r
  private final val OnWroteRe   = """\s*On\s.*wrote:\s*$""".r
  private final val QuoteRe     = ">+".r
  private final val SignatureRe = """(?:\s*—|\s*--|\s*__|-\w|Sent from my (\w+\s*){1,3})""".r

  /** A section of a reply.
    * @param lines
    *   the lines of the section
    * @param quoted
    *   whether the section is quoted
    * @param signature
    *   whether the section is a signature
    * @param hidden
    *   whether the section should remain hidden
    */
  case class Section(lines: List[String], quoted: Boolean, signature: Boolean, hidden: Boolean)
end EmailReply
