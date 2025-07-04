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

import java.lang as jl
import scaloi.misc.{Boxes, JavaBuilders}
import scaloi.misc.JavaOptionalInstances

import scala.collection.Factory
import scalaz.Functor
import scalaz.std.OptionInstances

import scala.reflect.ClassTag

/** A collection of extension methods for dealing with collections of numeric types. */
object CollectionBoxOps extends ToCollectionBoxOps with OptionInstances

/** @tparam Coll
  *   the containing collection type
  * @tparam Elem
  *   the boxed object type
  */
final class BoxedCollectionOps[Coll[T], Elem <: AnyRef](private val self: Coll[Elem]) extends AnyVal:

  /** Convert the members of a collection from boxed to unboxed representation.
    *
    * Use as: Seq[jl.Long](new jl.Long(1L)).unboxInside() : Seq[Long] = Seq(1L)
    */
  def unboxInside: CollectionUnboxer[Coll, Elem, Coll] =
    new CollectionUnboxer[Coll, Elem, Coll](self)

  /** Convert the members of a collection from boxed to unboxed representation, while also changing the collection type.
    *
    * Use as: Seq[jl.Long](new jl.Long(1L)).unboxInsideTo[List]() : List[Long] = List(1L)
    */
  def unboxInsideTo[Out[_]]: CollectionUnboxer[Coll, Elem, Out] =
    new CollectionUnboxer[Coll, Elem, Out](self)
end BoxedCollectionOps

final class CollectionUnboxer[Coll[T], Elem, Out[_]](private val self: Coll[Elem]) extends AnyVal:
  def apply[UnboxedElem <: AnyVal]()(implicit
    boxes: Boxes[UnboxedElem, Elem],
    ap: CBOApplicable[Coll, Out, Elem, UnboxedElem]
  ): Out[UnboxedElem] =
    ap.map(self)(boxes.unbox)

/** @tparam Coll
  *   the containing collection type
  * @tparam Elem
  *   the unboxed primitive type
  */
final class UnboxedCollectionOps[Coll[T], Elem <: AnyVal](private val self: Coll[Elem]) extends AnyVal:

  /** Convert the members of a collection from unboxed to boxed representation.
    *
    * Use as: Seq[Long](1L).boxInside() : Seq[jl.Long] = Seq(1L)
    */
  def boxInside: CollectionBoxer[Coll, Elem, Coll] =
    new CollectionBoxer[Coll, Elem, Coll](self)

  /** Convert the members of a collection from unboxed to boxed representation, while also changing the collection type.
    *
    * Use as: Seq[Long](1L).boxInsideTo[List]() : List[jl.Long] = List(1L)
    */
  def boxInsideTo[Out[_]]: CollectionBoxer[Coll, Elem, Out] =
    new CollectionBoxer[Coll, Elem, Out](self)

  /** Boxes the members of a collection into an array. */
  def boxInsideToArray[BoxedElem: ClassTag](implicit
    boxes: Boxes[Elem, BoxedElem],
    ev: Coll[Elem] <:< IterableOnce[Elem]
  ): Array[BoxedElem] =
    self.iterator.map(boxes.box).toArray
end UnboxedCollectionOps

final class CollectionBoxer[Coll[T], Elem <: AnyVal, Out[_]](private val self: Coll[Elem]) extends AnyVal:
  def apply[BoxedElem]()(implicit
    boxes: Boxes[Elem, BoxedElem],
    ap: CBOApplicable[Coll, Out, Elem, BoxedElem]
  ): Out[BoxedElem] =
    ap.map(self)(boxes.box)

trait ToCollectionBoxOps extends JavaBuilders with JavaOptionalInstances:

  import language.implicitConversions

  implicit def toBoxedCollectionOps[Elem <: AnyRef, CollIn[T]](self: CollIn[Elem]): BoxedCollectionOps[CollIn, Elem] =
    new BoxedCollectionOps[CollIn, Elem](self)

  implicit def toUnboxedCollectionOps[Elem <: AnyVal, CollIn[T]](
    self: CollIn[Elem]
  ): UnboxedCollectionOps[CollIn, Elem] =
    new UnboxedCollectionOps[CollIn, Elem](self)
end ToCollectionBoxOps

trait CBOApplicable[CollIn[_], CollOut[_], ElemIn, ElemOut]:
  def map(ci: CollIn[ElemIn])(f: ElemIn => ElemOut): CollOut[ElemOut]

//noinspection ConvertExpressionToSAM
object CBOApplicable extends CBOApplicable0:
  implicit def apFunctor[F[_], ElemIn, ElemOut](implicit F: Functor[F]): CBOApplicable[F, F, ElemIn, ElemOut] =
    new CBOApplicable[F, F, ElemIn, ElemOut]:
      def map(ci: F[ElemIn])(f: (ElemIn) => ElemOut): F[ElemOut] =
        F.map(ci)(f)

  implicit def mkApGenTrav[CollIn[_], CollOut[_], ElemIn, ElemOut](implicit
    T: CollIn[ElemIn] <:< IterableOnce[ElemIn],
    fac: Factory[ElemOut, CollOut[ElemOut]]
  ): CBOApplicable[CollIn, CollOut, ElemIn, ElemOut] =
    new CBOApplicable[CollIn, CollOut, ElemIn, ElemOut]:
      def map(ci: CollIn[ElemIn])(f: (ElemIn) => ElemOut): CollOut[ElemOut] =
        ci.iterator.map(f).to(fac)

  implicit def mkApJIterable[CollIn[_], CollOut[_], ElemIn, ElemOut](implicit
    T: CollIn[ElemIn] <:< jl.Iterable[ElemIn],
    fac: Factory[ElemOut, CollOut[ElemOut]]
  ): CBOApplicable[CollIn, CollOut, ElemIn, ElemOut] =
    new CBOApplicable[CollIn, CollOut, ElemIn, ElemOut]:
      import scala.jdk.CollectionConverters.*
      def map(ci: CollIn[ElemIn])(f: (ElemIn) => ElemOut): CollOut[ElemOut] =
        ci.iterator.asScala.map(f).to(fac)
end CBOApplicable

sealed abstract class CBOApplicable0:
  implicit def mkApOptional2ApplicativePlus[OptIn[_], APOut[_], ElemIn, ElemOut](implicit
    Opt: scalaz.Optional[OptIn],
    AP: scalaz.ApplicativePlus[APOut]
  ): CBOApplicable[OptIn, APOut, ElemIn, ElemOut] =
    new CBOApplicable[OptIn, APOut, ElemIn, ElemOut]:
      def map(ci: OptIn[ElemIn])(f: (ElemIn) => ElemOut): APOut[ElemOut] =
        Opt.pextract(ci).fold((_: OptIn[Nothing]) => AP.empty[ElemOut], ei => AP.point(f(ei)))
