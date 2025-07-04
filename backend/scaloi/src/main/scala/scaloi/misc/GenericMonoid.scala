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

/** Provides monoid evidence for generic monoidal things. For example, a case class with just monoidal values.
  *
  * {{{
  * import scalaz.Monoid
  * import shapeless._
  * import scalaz.std.anyVal._
  * case class Foo(a: Int, b: Long)
  * implicit val fooMonoid = Monoid[Foo]
  * }}}
  */
object GenericMonoid {
  /* scala 3
  import scalaz.Monoid
  import shapeless._

  sealed trait MkMonoid[HL <: HList] { val monoid: Monoid[HL] }

  object MkMonoid                    {

    /** Monoidal evidence of a cons. */
    implicit def monoidHCons[H, T <: HList](implicit
      hMonoid: Monoid[H],
      tMonoid: Lazy[MkMonoid[T]]
    ): MkMonoid[H :: T] = new MkMonoid[H :: T] {
      val monoid =
        new Monoid[H :: T] {
          override val zero: H :: T =
            hMonoid.zero :: tMonoid.value.monoid.zero

          override def append(a: H :: T, b: => H :: T): H :: T =
            hMonoid.append(a.head, b.head) :: tMonoid.value.monoid.append(a.tail, b.tail)
        }
    }

    /** Monoidal evidence of nil. */
    implicit val monoidHNil: MkMonoid[HNil] =
      new MkMonoid[HNil] {
        val monoid = new Monoid[HNil] {
          override val zero                                = HNil
          override def append(f1: HNil, f2: => HNil): HNil = HNil
        }
      }
  }

  /** Monoidal evidence of a generic type. */
  implicit def monoidGeneric[T, R <: HList](implicit
    generic: Generic.Aux[T, R],
    rMonoid: MkMonoid[R]
  ): Monoid[T] = new Monoid[T] {
    override val zero                     = generic.from(rMonoid.monoid.zero)
    override def append(a: T, b: => T): T = generic.from(rMonoid.monoid.append(generic.to(a), generic.to(b)))
  }

  /** A kinda-curried summoning method for generically-derived monoids.
   *
    * Use thusly:
   * {{{
   *   import scalaz.std.anyVal._
   *   import scaloi.misc._
   *   case class Ints(i: Int, j: Int)
   *   implicit val intsMonoid: Monoid[Ints] = GenericMonoid[Ints]()
   * }}}
   */
  @inline def apply[T] = new applied[T]
  final class applied[T] {
    @inline
    def apply[Repr <: HList]()(implicit gen: Generic.Aux[T, Repr], mk: MkMonoid[Repr]): Monoid[T] =
      monoidGeneric[T, Repr](gen, mk)
  }

   */
}
