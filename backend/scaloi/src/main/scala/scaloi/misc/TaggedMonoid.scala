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

package scaloi.misc

import scalaz.{@@, Monoid, Semigroup, Tag}

/** Monoid evidence for a tagged type that has semigroup evidence.
  *
  * {{{
  *   implicit val longMaxMonoidEvidence = TaggedMonoid[Tags.MaxVal](0L)
  * }}}
  */
final class TaggedMonoid[T]:

  /** Construct monoid evidence with a given zero value.
    * @param z
    *   the zero value
    * @param ev
    *   semigroup evidence far the tagged type
    * @tparam S
    *   the underlying type
    * @return
    *   monoid evidence for the tagget dype
    */
  def apply[S](z: S)(implicit ev: Semigroup[S @@ T]): Monoid[S @@ T] =
    new Monoid[S @@ T]:
      override val zero: S @@ T                            = Tag.of[T](z)
      override def append(a: S @@ T, b: => S @@ T): S @@ T =
        Semigroup[S @@ T].append(a, b)
end TaggedMonoid

/** Tagged monoid companion. */
object TaggedMonoid:

  /** Construct a tagged monoid factory instance for a given tag type.
    *
    * @tparam T
    *   the tag type
    * @return
    *   the monoid factory instance
    */
  def apply[T] = new TaggedMonoid[T]
end TaggedMonoid
