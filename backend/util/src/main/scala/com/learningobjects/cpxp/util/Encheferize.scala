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

package com.learningobjects.cpxp.util

import java.util.Locale

import scala.annotation.tailrec
import scala.util.matching.Regex.Match
import scalaz.std.string.stringInstance
import scalaz.std.anyVal.*
import scalaz.std.vector.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.boolean.*

class Encherefize

object Encheferize extends TransmogrifyMain:
  val SWEDISH_CHEF    = Locale.of("sv", "CH")
  val ENGLISH_REVERSE = Locale.of("en", "RV")
  val ENGLISH_KEYS    = Locale.of("en", "KY")

  val Locales = List(
    SWEDISH_CHEF    -> "Svenska (Kocken)",
    ENGLISH_REVERSE -> "English (Rovekistan)",
    ENGLISH_KEYS    -> "English (Kentucky)",
  )

  def translate(s: String, l: Locale): String = l match
    case SWEDISH_CHEF    => enchef(s)
    case ENGLISH_REVERSE => reverse(s)
    case ENGLISH_KEYS    => s"⪡$s⪢"
    case _               => s

  def translate(k: String, v: String, f: (Locale, String) => Unit): Unit =
    f(SWEDISH_CHEF, enchef(v))
    f(ENGLISH_REVERSE, reverse(v))
    f(ENGLISH_KEYS, s"⪡$k⪢")

  /** Translates a phrase.
    *
    * @param line
    *   The phrase to be translated
    * @return
    *   The translated phrase
    */
  def enchef(line: String): String =
    WordRe.replaceAllIn(line, enchefWord) + line.endsWith(".") ?? " Bork Bork Bork!"

  private[util] def transmogrify(line: String) = enchef(line)

  private def enchefWord(m: Match): String =
    if isSpecial(m.group(0)) then m.group(0) else enchefImpl(m.group(0))

  private def isSpecial(s: String): Boolean =
    s.startsWith("{") || s.startsWith("#") || (s.head == '&' && s.last == ';')

  def reverse(str: String): String =
    Atom
      .findAllIn(str)
      .foldRight(new StringBuilder) { (a, sb) =>
        sb append Parens.getOrElse(a, a)
      }
      .toString

  private val Parens = List("(" -> ")", "[" -> "]", "{" -> "}", "?" -> "⸮").flatMap(t => List(t, t.swap)).toMap

  // word or braced expression or html entity
  private val WordRe = """\{[^\}]*\}|#[^#]*#|[a-zA-Z]+|&#?([a-zA-Z0-9]+);""".r

  // char or braced expression
  private val Atom = """\{\{[^}]*\}\}|\{[^\}]*\}|#[^#]*#|.""".r

  // This is now just horrible.
  case class Chef(swedish: Vector[String], iSeen: Boolean)

  // Whut whaaat? name-based extractors for +1 efficiency.
  // http://hseeberger.github.io/blog/2013/10/04/name-based-extractors-in-scala-2-dot-11/

  class PrefixOpt(val parts: (String, String)) extends AnyVal:
    def isEmpty: Boolean      = parts == null
    def get: (String, String) = parts

  object PrefixOpt:
    def apply(name: String, length: Int): PrefixOpt =
      if name.length >= length then new PrefixOpt((name.substring(0, length), name.substring(length)))
      else new PrefixOpt(null)

  object Prefix1:
    def unapply(name: String): PrefixOpt = PrefixOpt(name, 1)

  object Prefix2:
    def unapply(name: String): PrefixOpt = PrefixOpt(name, 2)

  object Prefix3:
    def unapply(name: String): PrefixOpt = PrefixOpt(name, 3)

  object Prefix4:
    def unapply(name: String): PrefixOpt = PrefixOpt(name, 4)

  @tailrec
  private def enchefImpl(word: String, chef: Chef = Chef(Vector.empty, iSeen = false)): String =
    val start = chef.swedish.isEmpty
    word match
      case ""                                           =>
        val result = new StringBuilder(chef.swedish foldMap (_.length))
        chef.swedish.foreach(result.append)
        result.toString
      // beginning of word rules
      case Prefix1("e", rest) if start                  =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "i"))
      case Prefix1("E", rest) if start                  =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "I"))
      case Prefix1("o", rest) if start                  =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "oo"))
      case Prefix1("O", rest) if start                  =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "Oo"))
      case Prefix2("co", rest) if start                 =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "cuo"))
      case Prefix2("Co", rest) if start                 =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "Cuo"))
      // in-word rules
      case Prefix2("ew", rest) if !start                =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "oo"))
      case Prefix1("e", rest) if !start && rest.isEmpty =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "e-a"))
      case Prefix1("f", rest) if !start                 =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "ff"))
      case Prefix2("ir", rest) if !start                =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "ur"))
      case Prefix1("i", rest) if !start && !chef.iSeen  =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "ee", iSeen = true))
      case Prefix2("ow", rest) if !start                =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "oo"))
      case Prefix1("o", rest) if !start                 =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "u"))
      case Prefix4("tion", rest) if !start              =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "shun"))
      case Prefix1("u", rest) if !start                 =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "oo"))
      case Prefix1("U", rest) if !start                 =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "Oo"))
      // anywhere rules
      case Prefix2("An", rest)                          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "Un"))
      case Prefix2("Au", rest)                          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "Oo"))
      case Prefix1("A", rest) if !rest.isEmpty          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "E"))
      case Prefix2("an", rest)                          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "un"))
      case Prefix2("au", rest)                          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "oo"))
      case Prefix1("a", rest) if !rest.isEmpty          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "e"))
      case Prefix2("en", rest) if rest.isEmpty          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "ee"))
      case Prefix2("th", rest) if rest.isEmpty          =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "t"))
      case Prefix3("the", rest)                         =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "zee"))
      case Prefix3("The", rest)                         =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "Zee"))
      case Prefix1("v", rest)                           =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "f"))
      case Prefix1("V", rest)                           =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "F"))
      case Prefix1("w", rest)                           =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "v"))
      case Prefix1("W", rest)                           =>
        enchefImpl(rest, chef.copy(swedish = chef.swedish :+ "V"))
      case w                                            =>
        enchefImpl(w.substring(1), chef.copy(swedish = chef.swedish :+ w.charAt(0).toString))
    end match
  end enchefImpl
end Encheferize
