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

import java.util.ResourceBundle.getBundle

import scala.util.Random

/** A̓͟n͞ a͡nc͖̑i͠e̋n̠͡tͯ̕ i̡nc̦an͍̓tā̵̦tͪ͝i̠o̞n͆ to s͜u̷̼m̶̈́m̸̰o̙ṉ̚ Z̲Á͖L̯̓G̾͜O̬̍.
  *
  * U̗͝s̛̬͑e͢ wǐ̡t̘̋h gͤre̛̺a̞t̷̩̔ c̟aút͗̀i̟o͠n̽ͅ, fͩo͞r͒͘ y̤͟o̵̩u̘͂r̓ m̫or̳ͤ͠ta̳l̄͟ s̔͢o̧͕͐ǔ͇͝l m̮̊ay̡̬
  * a͇͡l̘̆r̢e̡͈͌a̷̻̿d͞ỳ̤͜ b́ẽ i̕ṉ d̺́ȧ́n̖̍ģ̄ȩͅrͪ͢.
  */
object Zalgo extends ToStringZalginator with TransmogrifyMain:

  /** H̢̗͛E͐ C̭̎͜O͓̎ME͖͗͡S̐ */
  def apply(prophecy: String)(implicit mood: Mood = defaultMood, rnd: Random = Random): String =
    def mkSuffix(): String =
      val (u, m, d) = mood.sample()
      val up        = lazyShuffle(soul `getString` "zalgo.up") take u mkString ""
      val mid       = lazyShuffle(soul `getString` "zalgo.mid") take m mkString ""
      val down      = lazyShuffle(soul `getString` "zalgo.down") take d mkString ""
      up ++ mid ++ down

    def mungeChar(ch: Char): String = ch match
      case alphanum if Character.isAlphabetic(alphanum) || Character.isDigit(alphanum) =>
        ch +: mkSuffix()
      case other                                                                       => other.toString

    prophecy.map(mungeChar).mkString("")
  end apply

  final case class Mood(up: Int, mid: Int, down: Int):
    private[Zalgo] def sample()(implicit rnd: Random): (Int, Int, Int) =
      (rnd nextInt (up + 1), rnd nextInt (mid + 1), rnd nextInt (down + 1))

  val defaultMood = Mood(8, 2, 8)

  val soul = getBundle("com.learningobjects.cpxp.util.Zalgo")

  private def lazyShuffle(str: String)(implicit rnd: Random): LazyList[Char] =
    rnd.shuffle[Char, LazyList[Char]](str.to(LazyList))

  private[util] def transmogrify(line: String): String = this(line)
end Zalgo

final class StringZalginator(val sc: StringContext) extends AnyVal:
  def zalgo(args: Any*)(implicit mood: Zalgo.Mood = Zalgo.defaultMood, rnd: Random = Random): String =
    StringContext.checkLengths(args, sc.parts)
    sc.parts
      .zip(args :+ "")
      .map {
        case (p, a) if p.endsWith("\\") => s"${Zalgo(p dropRight 1)}$a"
        case (p, a)                     => Zalgo(s"$p$a")
      }
      .mkString("")
end StringZalginator

trait ToStringZalginator:
  import language.implicitConversions

  @inline
  implicit def ToStringZalginator(sc: StringContext): StringZalginator =
    new StringZalginator(sc)
