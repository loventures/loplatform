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

package scaloi.syntax

import scalaz.{Equal, Monoid}

/** Enhancements on [[Monoid]](s). */
final class MonoidOps[M](private val self: M) extends AnyVal:

  /** New Zealand map.
    *
    * Applies `f` to `self` if non-empty; otherwise, passes through the empty value.
    * @param f
    *   the function to map non-zero values
    * @param M
    *   the monoid instance for `M`
    * @param Me
    *   the equality instance for `M`
    * @return
    *   `self` if empty, otherwise `f(self)`
    */
  def mapNZ(f: M => M)(implicit M: Monoid[M], Me: Equal[M]): M =
    M.onNotEmpty(self)(f(self))
end MonoidOps

trait ToMonoidOps:
  import language.implicitConversions

  @inline implicit final def ToMonoidOps[M: Monoid](m: M): MonoidOps[M] =
    new MonoidOps[M](m)
