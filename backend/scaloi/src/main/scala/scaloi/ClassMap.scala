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

import syntax.ClassTagOps.classTagClass

/** A map from classes to values of those classes' types.
  *
  * @tparam U
  *   the upper bound on the class types
  * @tparam L
  *   the lower bound on the class types
  *
  * @param toMap
  *   convert this $classmap into a standard scala [[scala.collection.immutable.Map Map]].
  *
  * @define classmap
  *   [[scaloi.ClassMap ClassMap]]
  * @define none
  *   [[scala.None None]]
  */
final class ClassMap[U, L <: U] private (val toMap: Map[Class[? >: L <: U], ? >: L <: U]):

  /** Test whether this map contains a value of type [[K]].
    * @tparam K
    *   the type of the desired value
    * @param k
    *   the precise runtime class of [[K]]
    */
  def contains[K >: L <: U](k: Class[K]): Boolean =
    toMap.contains(k)

  /** Get the value of type [[K]] in this map, or $none.
    * @tparam K
    *   the type of the desired value
    * @param k
    *   the precise runtime class of [[K]]
    */
  def get[K >: L <: U](k: Class[K]): Option[K] =
    toMap.get(k).asInstanceOf[Option[K]]

  /** Get the value of type [[K]] in this map, or $none.
    * @tparam K
    *   the type of the desired value
    */
  @inline
  def get[K >: L <: U: reflect.ClassTag]: Option[K] =
    this `get` classTagClass[K]

  /** Add a value to this $classmap.
    *
    * @tparam K
    *   the type of the desired value
    * @param kv
    *   the class of the desired value paired with the new value
    * @return
    *   a new $classmap with that value added.
    */
  def +[K >: L <: U](kv: (Class[K], K)): ClassMap[U, L] =
    new ClassMap[U, L](toMap + kv)

  /** Add a value to this $classmap.
    *
    * @tparam K
    *   the type of the desired value
    * @param v
    *   the value of that type
    * @return
    *   a new $classmap with that value added.
    */
  @inline
  def +[K >: L <: U: reflect.ClassTag](v: K): ClassMap[U, L] =
    this + (classTagClass[K] -> v)

  /** Remove a value from this $classmap.
    * @tparam K
    *   the type of the value to be removed
    * @param k
    *   the runtime class of the value to be removed
    * @return
    *   a $classmap without a value of that class.
    */
  def -[K >: L <: U](k: Class[K]): ClassMap[U, L] =
    if toMap contains k then new ClassMap[U, L](toMap - k) else this

  /** Remove a value from this $classmap.
    * @tparam K
    *   the type of the value to be removed
    * @return
    *   a $classmap without a value of that class.
    */
  @inline
  def -[K >: L <: U: reflect.ClassTag]: ClassMap[U, L] =
    this - classTagClass[K]

  /** A [[scala.collection.immutable.Set Set]] of all keys in this $classmap.
    */
  @inline
  def keys: Set[Class[? >: L <: U]] = toMap.keySet

  /** An [[scala.collection.Iterable Iterable]] of all values in this $classmap.
    */
  @inline
  def values: Iterable[? >: L <: U] = toMap.values

  /** Explicitly widen the upper bound on the key types of this $classmap.
    *
    * Variance in Scala is annoying, and therefore this method is provided to explicitly widen the upper bound.
    *
    * @tparam UU
    *   the new upper bound
    */
  @inline
  def widen[UU >: U]: ClassMap[UU, L] = this.asInstanceOf[ClassMap[UU, L]]

  /** Explicitly narrow the lower bound on the key types of this $classmap.
    *
    * Variance in Scala is annoying, and therefore this method is provided to explicitly narrow the lower bound.
    *
    * @tparam LL
    *   the new lower bound
    */
  @inline
  def narrow[LL <: L]: ClassMap[U, LL] = this.asInstanceOf[ClassMap[U, LL]]
end ClassMap

/** Class map companion. */
object ClassMap:

  /** Create an empty [[scaloi.ClassMap ClassMap]] with the given bounds.
    * @tparam U
    *   the upper bound on the class types
    * @tparam L
    *   the lower bound on the class types
    */
  def empty[U, L <: U]: ClassMap[U, L] = new ClassMap[U, L](Map.empty)

  /** Create an empty `ClassMap` with the given upper bound and no lower bound.
    * @tparam U
    *   the upper bound on the class types
    */
  @inline
  def empty0[U]: ClassMap0[U] = empty[U, Nothing]
end ClassMap
