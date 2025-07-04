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

/* package _root_ */

import scalaz.*

package object scaloi:
  type Attempt[A] = Throwable \/ A

  /** A (Scala) partial function.
    *
    * Basically a [[Kleisli]] arrow over [[Option]], but using exceptions.
    */
  type =∂>[-A, +R] = PartialFunction[A, R]

  /** A [[scaloi.ClassMap]] with no lower bound.
    * @tparam U
    *   the upper bound of the types of values in this [[scaloi.ClassMap]]
    */
  type ClassMap0[U] = ClassMap[U, Nothing]

  import MultiMap.*

  /** For a fixed `K` and `V`, `MultiMap[K, V]` is a monoid. */
  implicit def MultiMapMonoid[K, V]: Monoid[MultiMap[K, V]] =
    new Monoid[MultiMap[K, V]]:
      def zero: MultiMap[K, V] = Map.empty

      def append(f1: MultiMap[K, V], f2: => MultiMap[K, V]) = f1 `combine` f2
end scaloi
