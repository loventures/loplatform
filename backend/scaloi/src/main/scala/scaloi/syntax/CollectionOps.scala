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

import scalaz.{Liskov, Semigroup, \/}

import java.util
import scala.annotation.tailrec
import scala.collection.{BuildFrom, mutable}
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

final class CollectionOps[CC[X] <: IterableOnce[X], T](private val self: CC[T]) extends AnyVal:
  import Liskov.*

  /** Calculate the cross product of `self` and `other`.
    *
    * The cross product of two collections is a collection containing all possible pairs of elements from each
    * collection.
    *
    * @param other
    *   the other collection to cross with `self`
    * @return
    *   a (lazy) iterable of all possible pairs of elements from each collection
    */
  def cross[U](other: IterableOnce[U]): Iterable[(T, U)] = for
    t <- self.iterator.to(LazyList)
    u <- other.iterator.to(LazyList)
  yield (t, u)

  /** An alias for `cross`. */
  @inline def ×[U](other: IterableOnce[U]): Iterable[(T, U)] = cross(other)

  /** An alias for `cross`. */
  @inline def ⟗[U](other: IterableOnce[U]): Iterable[(T, U)] = cross(other)

  @inline def squared: Iterable[(T, T)] = this ⟗ self

  /** Group a seq to a map of values grouped by the specified value function.
    *
    * @param keyFn
    *   The function transforming the entries to map keys.
    * @param valueFn
    *   The function transforming the entries to values in the map.
    * @tparam K
    *   The key type.
    * @tparam V
    *   The grouped value type.
    * @return
    *   Map of values grouped by the given key function
    */
  private def groupMap[K, V, That](keyFn: T => K)(valueFn: T => V)(implicit
    bf: BuildFrom[CC[T], V, That],
  ): Map[K, That] =
    val result = mutable.Map
      .empty[K, mutable.Builder[V, That]]
      .withDefault(_ => bf.newBuilder(self))
    self.iterator.foreach { t =>
      val k = keyFn(t)
      result(k) = result(k) += valueFn(t)
    }
    result.view.mapValues(_.result()).toMap
  end groupMap

  /** Group this collection of pairs into a multimap.
    *
    * Similar to [[scala.collection.IterableOnceOps.toMap toMap]], but keys are aggregated into the same kind of
    * collection as this one.
    */
  def groupToMap[K, V, That](implicit
    kv: T <~< (K, V),
    bf: BuildFrom[CC[T], V, That],
  ): Map[K, That] =
    groupMap(t => kv(t)._1)(t => kv(t)._2)

  /** Apply a partial function to this collection and combine the resulting tuples into a map.
    */
  def collectToMap[U, V](pf: PartialFunction[T, (U, V)]): Map[U, V] =
    val b = Map.newBuilder[U, V]
    self.iterator.foreach(pf.runWith(b += _))
    b.result()

  def collectType[U: ClassTag](implicit bf: BuildFrom[CC[T], U, CC[U]]): CC[U] =
    val b = bf.newBuilder(self)
    self.iterator.flatMap(implicitly[ClassTag[U]].unapply).foreach(b.+=)
    b.result()

  /** Group the elements of this collection by `kf`, map them by `vf`, and fold them as elements of the monoid `V`.
    */
  def groupMapFold[K, V](kf: T => K)(vf: T => V)(implicit V: Semigroup[V]): Map[K, V] =
    val result = mutable.Map.empty[K, V]
    self.iterator.foreach { t =>
      val k = kf(t)
      result(k) = result.get(k).fold(vf(t))(V.append(_, vf(t)))
    }
    result.toMap

  /** Group the elements of this collection by `kf`, taking the first element on a `kf` collision.
    *
    * Sugar for self.groupBy(kf).mapValues(_.head)
    */
  def groupUniqBy[K](kf: T => K): Map[K, T] =
    groupMapFold(kf)(identity)(using Semigroup.firstSemigroup)

  /** Group the elements of this collection to `vf`, taking the first element on a key collision.
    */
  def groupUniqTo[V](vf: T => V): Map[T, V] =
    groupMapFold(identity)(vf)(using Semigroup.firstSemigroup)

  /** Group the elements of this collection by `kf`, taking the first element on a `kf` collision, map the values with
    * `vf`.
    *
    * Sugar for self.groupBy(kf).mapValues(_.head).mapValues(vf)
    */
  def groupMapUniq[K, V](kf: T => K)(vf: T => V): Map[K, V] =
    groupMapFold(kf)(vf)(using Semigroup.firstSemigroup)

  /** Collect elements of this collection into one of two result collections, possibly of different types.
    */
  def partitionCollect[A, B, CCA, CCB](f: PartialFunction[T, A \/ B])(implicit
    bfA: BuildFrom[CC[T], A, CCA],
    bfB: BuildFrom[CC[T], B, CCB],
  ): (CCA, CCB) =
    val (as, bs) = (bfA.newBuilder(self), bfB.newBuilder(self))
    self.iterator.foreach(f.runWith(_.fold(as.+=, bs.+=)))
    (as.result(), bs.result())

  def collectWhile[B, CCB](f: PartialFunction[T, B])(implicit
    bfA: BuildFrom[CC[T], B, CCB]
  ): CCB =
    val bs = bfA.newBuilder(self)
    self.iterator.takeWhile(f.runWith(bs.+=)).foreach(_ => ())
    bs.result()

  /** Apply a map to an optional value to the elements of this traversable, returning the first defined result.
    *
    * @param f
    *   the map function
    * @tparam B
    *   the target type
    * @return
    *   the optional value
    */
  @inline def findMap[B](f: T => Option[B]): Option[B] =
    val i                        = self.iterator
    @tailrec def loop: Option[B] =
      if i.hasNext then
        val ob = f(i.next())
        if ob.isDefined then ob
        else loop
      else None
    loop

  /** Apply a partial function to the elements of this traversable, returning the first defined result.
    *
    * @param f
    *   the partial function
    * @tparam B
    *   the target type
    * @return
    *   the optional value
    */
  @inline def findMapf[B](f: PartialFunction[T, B]): Option[B] =
    findMap(f.lift)

  /** Given a function from [[T]] to a tuple of [[B]] and [[C]], fold this traversable into a [[Map]].
    * @param f
    *   the map function
    * @tparam B
    *   the key type
    * @tparam C
    *   the value type
    * @return
    *   the resulting [[Map]]
    */
  @inline def foldToMap[B, C](f: T => (B, C)): Map[B, C] =
    self.iterator.map(f).toMap

  /** Short circuit a fold to the monoidal zero if this collection is empty.
    * @param f
    *   the fold function
    * @tparam U
    *   the result type with [[Zero]] evidence
    * @return
    *   the result type
    */
  @inline def foldSC[U: Zero](f: CC[T] => U): U = if self.iterator.isEmpty then Zero.zero[U] else f(self)

  /** Short circuit a fold to the monoidal zero if this collection is empty. An alias for foldSC.
    * @param f
    *   the fold function
    * @tparam U
    *   the result type with [[Zero]] evidence
    * @return
    *   the result type
    */
  @inline def ??>[U: Zero](f: CC[T] => U): U = if self.iterator.isEmpty then Zero.zero[U] else f(self)

  /** Short circuit a function to the monoidal zero if this collection is empty.
    * @param f
    *   the function
    * @tparam U
    *   the result type with [[Zero]] evidence
    * @return
    *   the result type
    */
  @inline def ??[U: Zero](f: => U): U = if self.iterator.isEmpty then Zero.zero[U] else f

  /** Return this, if non-empty, else that.
    * @param cc
    *   the alternate collection
    * @return
    *   this or that
    */
  @inline def ||(cc: => CC[T]): CC[T] = if self.iterator.isEmpty then cc else self

  /** Convert this collection of tuples to a map, summing the values.
    * @param ev
    *   evidence that the elements are tuples
    * @param Semigroup
    *   semigroup evidence of the values
    * @tparam A
    *   the key type
    * @tparam B
    *   the value type
    * @return
    *   a map from keys to values
    */
  def sumToMap[A, B](implicit ev: T <:< (A, B), Semigroup: Semigroup[B]): Map[A, B] =
    import mutableMap.*
    val acc = mutable.Map.empty[A, B]
    self.iterator foreach { t =>
      acc.append(ev(t)._1, ev(t)._2)
    }
    acc.toMap
end CollectionOps

trait ToCollectionOps:
  import language.implicitConversions

  @inline implicit final def toCollectionOps[CC[X] <: IterableOnce[X], T](self: CC[T]): CollectionOps[CC, T] =
    new CollectionOps[CC, T](self)

  @inline implicit final def toJavaCollectionOps[CC[X] <: util.Collection[X], T](
    self: CC[T]
  ): CollectionOps[Iterable, T] =
    new CollectionOps[Iterable, T](self.asScala)
end ToCollectionOps

object CollectionOps extends ToCollectionOps
