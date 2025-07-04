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

package loi.authoring.exchange.imprt.math

import java.io.{ByteArrayOutputStream, StringReader}
import javax.xml.transform.{Source, TransformerFactory, URIResolver}
import javax.xml.transform.stream.{StreamResult, StreamSource}

import org.apache.commons.text.StringEscapeUtils

import scala.xml.Node

class ClasspathURIResolver extends URIResolver:
  override def resolve(href: String, base: String): Source =
    new StreamSource(getClass.getResourceAsStream(href))

object MathMLToLatexConverter:
  private val transformerFactory = TransformerFactory.newInstance()
  transformerFactory.setURIResolver(new ClasspathURIResolver())

  private val xslSource = getClass.getResourceAsStream("mmltex.xsl")

  private val templates = transformerFactory.newTemplates(new StreamSource(xslSource))

  def convert(node: Node): String =
    val transformer  = templates.newTransformer()
    val xmlSource    = new StreamSource(new StringReader(node.toString))
    val outputStream = new ByteArrayOutputStream()
    val result       = new StreamResult(outputStream)
    transformer.transform(xmlSource, result)

    /* Replace unicode characters not supported by KaTeX. */
    val latex = replaceUnicodeCharsWithTokens(replaceFigureSpace(replaceNbsp(outputStream.toString)))
      .replaceAll("\u200b", "")         /* zero width space */
      .replaceAll("\u2010", "-")        /* hyphen */
      .replaceAll("\u2013", "-")        /* en dash used for minus sign by mistake */
      .replaceAll("\u2019", "'")        /* right single quotation mark */
      .replaceAll("\u201c", "\"")       /* left double quotation mark */
      .replaceAll("\u201d", "\"")       /* right double quotation mark */
      .replaceAll("\u2212", "-")        /* minus sign */
      .replaceAll("\u2223", "|")        /* divides */
      .replaceAll("\u2502", "|")        /* box drawings light vertical */
      .replaceAll("\\\\kern\\{1\\}", "\\\\kern\\{1em\\}")
      .replaceAll("\\\\underset\\{\u00af\\}", "\\\\underline")
      .replaceAll("\\\\text\\{\\}", "") /* clean up empty text */

    /* In all cases, we are inserting LaTeX inside of HTML so we need to escape characters like '<' */
    val escaped = StringEscapeUtils.escapeXml11(latex)
    s"""<span class="math-tex">$escaped</span>"""
  end convert

  /* Replace \u00a0 (no-break whitespace) with ' ' inside of \text{} and with '\ ' outside of \text{}. */
  private def replaceNbsp(input: String): String        = replaceUnicodeChar(input, '\u00a0', " ", "\\ ")
  private def replaceFigureSpace(input: String): String = replaceUnicodeChar(input, '\u2007', " ", "\\ ")

  private val charTokenMap = Map(
    '\u00b0' -> "^{\\circ}",
    '\u00b2' -> "^2",
    '\u00b3' -> "^3",
    '\u00b7' -> "\\cdot",         /* middle dot */
    '\u00d7' -> "\\times",
    '\u00f7' -> "\\div",
    '\u01a9' -> "\\Sigma",        /* esh */
    '\u0394' -> "\\Delta",
    '\u03a3' -> "\\Sigma",
    '\u03b1' -> "\\alpha",
    '\u03b2' -> "\\beta",
    '\u03b3' -> "\\gamma",
    '\u03bc' -> "\\mu",
    '\u03bd' -> "\\nu",
    '\u03c0' -> "\\pi",
    '\u03c3' -> "\\sigma",
    '\u2003' -> "\\quad",         /* em space */
    '\u2009' -> "\\kern{0.17em}", /* thin space */
    '\u2032' -> "\\prime",
    '\u2133' -> "\\mathscr{M}",
    '\u21cc' -> "\\rightleftharpoons",
    '\u221a' -> "\\sqrt",
    '\u2248' -> "\\approx",
    '\u2261' -> "\\equiv",
    '\u22c5' -> "\\cdot",         /* dot operator */
    '\u2551' -> "\\Vert",
    '\u22ef' -> "\\cdots",
    '\u2609' -> "\\odot",         /* sun */
    '\u27f6' -> "\\longrightarrow"
  )

  private def replaceUnicodeCharsWithTokens(input: String): String =
    charTokenMap.foldLeft(input) { case (in, (c, token)) => replaceUnicodeCharWithToken(in, c, token) }

  private def replaceUnicodeCharWithToken(input: String, char: Char, token: String): String =
    replaceUnicodeChar(input, char, s"} $token \\text{", s"$token ")

  private def replaceUnicodeChar(input: String, char: Char, insideText: String, outsideText: String): String =
    val builder      = new StringBuilder()
    var i            = 0
    var isInsideText = false
    while i < input.length do
      if input.charAt(i) == char then
        val replace = if isInsideText then insideText else outsideText
        builder.append(replace)
      else
        if !isInsideText && (i + 6 < input.length) && input.slice(i, i + 5) == "\\text" then isInsideText = true
        else if isInsideText && input.charAt(i) == '}' then isInsideText = false
        builder.append(input.charAt(i))
      i += 1
    builder.toString
  end replaceUnicodeChar
end MathMLToLatexConverter
