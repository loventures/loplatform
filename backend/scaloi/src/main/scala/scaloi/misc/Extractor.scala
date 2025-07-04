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
package misc

/** An extractor template for extractors expressible as a predicate and a transformation function.
  * @param pred
  *   the predicate that decides if the extractor matches
  * @param trans
  *   a function that transforms a value for which `pred` is true
  */
class Extractor[I, O](pred: I => Boolean, trans: I => O):
  final def unapply(arg: I): Option[O] =
    if pred(arg) then Some(trans(arg)) else None

/** Sundry purportedly-useful extractor templates. */
object Extractor:

  /** An extractor for strings starting with a given prefix. Matches iff the string starts with `pre`. Returns the part
    * of the string after `pre`.
    */
  def dropPrefix(pre: String): Extractor[String, String] =
    new Extractor(_ `startsWith` pre, _ drop pre.length)

  /** An extractor for strings ending with a given suffix. Matches iff the string ends with `suf`. Returns the part of
    * the string before `suf`.
    */
  def dropSuffix(pre: String): Extractor[String, String] =
    new Extractor(_ `endsWith` pre, _ dropRight pre.length)
end Extractor
