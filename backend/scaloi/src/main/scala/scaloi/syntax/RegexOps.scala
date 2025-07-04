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

package scaloi
package syntax

import scala.util.matching.Regex

import scala.language.implicitConversions

/** Enhancements on regex. */
final class RegexOps(private val self: Regex) extends AnyVal:

  /** Test whether this regex partially matches `s`. */
  def test(s: String): Boolean = self.pattern.matcher(s).find

  /** Test whether this regex matches the start of `s`. */
  def lookingAt(s: String): Boolean = self.pattern.matcher(s).lookingAt

  /** Return the first matching group, if any. */
  def match1(s: String): Option[String] = PartialFunction.condOpt(s) { case self(e) =>
    e
  }
end RegexOps

/** Implicit conversion for Regex operations.
  */
trait ToRegexOps extends Any:

  /** Implicit conversion from [[Regex]] to its enhancements.
    *
    * @param r
    *   the regex
    */
  implicit def toRegexOps(r: Regex): RegexOps = new RegexOps(r)
