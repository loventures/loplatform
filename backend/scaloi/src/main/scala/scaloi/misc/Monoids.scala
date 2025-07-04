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

import scalaz.syntax.tag.*
import scalaz.{@@, Monoid, Tag, \/}

/** Miscellaneous monoids.
  */
object Monoids:

  /** A tag for disjunctions desirous of a monoid instance which selects the first left value, if any exist.
    */
  sealed trait FailFast

  /** @see [[FailFast]] */
  final val FailFast = Tag.of[FailFast]

  /** Monoid instance for disjunctions that fails fast on the first left. Contrast with the standard DisjunctionMonoid
    * which requires a Semigroup left type that accumulates results.
    *
    * @tparam A
    *   the left type
    * @tparam B
    *   the right type
    * @return
    *   a fail-fast monoid instance
    */
  implicit def failFastDisjunctionMonoid[A, B: Monoid]: Monoid[(A \/ B) @@ FailFast] =
    new Monoid[(A \/ B) @@ FailFast]:
      type ABFF = (A \/ B) @@ FailFast

      override def zero: ABFF = Monoid[B].zero.rightFF[A]

      override def append(l: ABFF, r: => ABFF): ABFF =
        FailFast {
          for (lv <- l.unwrap; rv <- r.unwrap) yield Monoid[B].append(lv, rv)
        }

  implicit class FailFastMonoidSyntaxOps[T](private val self: T) extends AnyVal:
    import scalaz.syntax.either.*

    /** Wrap a value in the left side of a fail-fast disjunction.
      * @tparam B
      *   the right-hand type
      * @return
      *   the value, on the left
      */
    @inline def leftFF[B]: (T \/ B) @@ FailFast = FailFast(self.left)

    /** Wrap a value in the right side of a fail-fast disjunction.
      * @tparam A
      *   the left-hand type
      * @return
      *   the value, on the right
      */
    @inline def rightFF[A]: (A \/ T) @@ FailFast = FailFast(self.right)
  end FailFastMonoidSyntaxOps

  /** A [[Monoid]] for [[Map]]s which relies on [[scala.collection.MapLike.++]] to append maps. This means that it
    * prefers the key-value pairs from the right-side map over those from the left-side map.
    *
    * This is morally equivalent to tagging the values with [[scalaz.Tags.LastVal]].
    */
  implicit def rightBiasMapMonoid[K, V]: Monoid[Map[K, V]] = new Monoid[Map[K, V]]:
    override def zero: Map[K, V]                                    = Map.empty
    override def append(f1: Map[K, V], f2: => Map[K, V]): Map[K, V] = f1 ++ f2
end Monoids
