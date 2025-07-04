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

/** "Free" alias types that hide the equivalence proof(s) from the compiler.
  *
  * Not "free" as in "monad", don't worry.
  *
  * It works like so:
  *
  * @example
  *   {{{ scala> val FirstName = alias.Opaque[String]; type FirstName = FirstName.T FirstName:
  *   scaloi.alias.Opaque[String] = scaloi.alias$Opaque$Impl@7b452509 defined type alias FirstName
  *
  * scala> val LastName = alias.Opaque[String]; type LastName = LastName.T LastName: scaloi.alias.Opaque[String] =
  * scaloi.alias$Opaque$Impl@1dabfc9f defined type alias LastName
  *
  * scala> val fn: FirstName = FirstName inj "Bob" fn: FirstName = Bob
  *
  * scala> val ln: LastName = LastName inj "Dobbs" ln: LastName = Dobbs
  *
  * scala> (fn : String) <console>:16: error: type mismatch; found : FirstName (which expands to) FirstName.T required:
  * String (fn : String)
  *
  * scala> (ln : FirstName) <console>:17: error: type mismatch; found : LastName (which expands to) LastName.T required:
  * FirstName (which expands to) FirstName.T (ln : FirstName)
  *
  * scala> def fullName(fn: FirstName, ln: LastName): String = s"$fn $ln" fullName: (fn: FirstName, ln: LastName)String
  *
  * scala> def userName(fn: FirstName, ln: LastName): String = s"${(FirstName prj fn take 1).toLowerCase}${(LastName prj
  * ln).toLowerCase}" userName: (fn: FirstName, ln: LastName)String
  *
  * scala> fullName(fn, ln) res6: String = Bob Dobbs
  *
  * scala> userName(fn, ln) res7: String = bdobbs
  *
  * scala> val fns = FirstName subst List("King", "Lord", "", "Ragged", "Jack") fns: List[FirstName.T] = List(King,
  * Lord, "", Ragged, Jack)
  *
  * scala> val lns = LastName subst List("Mob", "Fanny", "Boy", "Robin", "Frost") lns: List[LastName.T] = List(Mob,
  * Fanny, Boy, Robin, Frost)
  *
  * scala> (fns zip lns) map (fullName _).tupled res8: List[String] = List(King Mob, Lord Fanny, " Boy", Ragged Robin,
  * Jack Frost)
  *
  * scala> (fns zip lns) map (userName _).tupled res9: List[String] = List(kmob, lfanny, boy, rrobin, jfrost) }}}
  */
object alias:

  /** An opaque type alias instantiation for `Repr`.
    * @tparam Repr
    *   the underlying type of `T`
    */
  sealed abstract class Opaque[Repr]:
    type T

    def inj(repr: Repr): T
    def prj(t: T): Repr

    def subst[F[_]](frepr: F[Repr]): F[T]
    def unsubst[F[_]](ft: F[T]): F[Repr]

  /** [[Opaque]] companion. */
  object Opaque:

    /** Create an opaque type alias for `Repr`. */
    def apply[Repr]: Opaque[Repr] = new Impl[Repr]

    private final class Impl[Repr] extends Opaque[Repr]:
      type T = Repr

      def inj(repr: Repr) = repr
      def prj(t: Repr)    = t

      def subst[F[_]](frepr: F[Repr]) = frepr
      def unsubst[F[_]](ft: F[Repr])  = ft
  end Opaque

  /** A semi-opaque type alias for `Repr`.
    *
    * This differs from `Opaque` in that `T` is bounded above by `Repr`; therefore a `T` can be used in a context
    * requiring a `Repr` without `prj`ecting it. Therefore, `prj` and `unsubst` are rarely useful.
    *
    * @tparam Repr
    *   the underlying type of `T`.
    */
  sealed abstract class Above[Repr]:
    type T <: Repr

    def inj(repr: Repr): T
    def prj(t: T): Repr

    def subst[F[_]](frepr: F[Repr]): F[T]
    def unsubst[F[_]](ft: F[T]): F[Repr]

  /** [[Above]] companion. */
  object Above:

    /** Create a type alias for `Repr` bounded above by `Repr`.. */
    def apply[Repr]: Above[Repr] = new Impl[Repr]

    private final class Impl[Repr] extends Above[Repr]:
      type T = Repr

      def inj(repr: Repr) = repr
      def prj(t: Repr)    = t

      def subst[F[_]](frepr: F[Repr]) = frepr
      def unsubst[F[_]](ft: F[Repr])  = ft
  end Above

  /** A specialization of `Above` at `Long`.
    *
    * This is equivalent to, but plausibly more efficient than, `Above[Long]`, as `inj` and `prj` have types `(J)J`, and
    * therefore do not need boxing and unboxing..
    */
  /* specialization? what specialization... see scala/bug#8405 */
  sealed abstract class AboveLong:
    type T <: Long

    def inj(repr: Long): T
    def prj(t: T): Long

    def subst[F[_]](frepr: F[Long]): F[T]
    def unsubst[F[_]](ft: F[T]): F[Long]

  object AboveLong:
    def apply(): AboveLong = new Impl

    private final class Impl extends AboveLong:
      type T = Long

      def inj(repr: Long) = repr
      def prj(t: Long)    = t

      def subst[F[_]](frepr: F[Long]) = frepr
      def unsubst[F[_]](ft: F[Long])  = ft
  end AboveLong
end alias
