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

import scala.collection.IterableOnce
import scaloi.syntax.hypermonad.*

/** Extension methods for dealing with multi-valued maps.
  */
object MultiMap:

  /** A "multimap" from `K` to `V`. Just a type alias. */
  type MultiMap[K, V] = Map[K, Seq[V]]

  /** Make an empty multimap. */
  @inline
  def empty[K, V]: MultiMap[K, V] = Map.empty

  /** Operations on multimaps. */
  /* TODO Generalize Seq to M: ApplicativePlus? */
  implicit class MultiMapOps[K, V](private val map: MultiMap[K, V]) extends AnyVal:

    /** Add a new `(key, value)` pair to this multimap. */
    def add(k: K, v: V): MultiMap[K, V] =
      map + (k -> (map.getOrElse(k, Vector.empty) :+ v))

    /** Add new `(key, value)` pairs to this multimap. */
    def add(kvs: (K, V)*): MultiMap[K, V] =
      kvs.foldLeft(map) { (m, kv) =>
        m.add(kv._1, kv._2)
      }

    /** Add a set of values to this multimap, associated with a given key. */
    def addAll(k: K, v: IterableOnce[V]): MultiMap[K, V] =
      map + (k -> (map.getOrElse(k, Vector.empty) ++ v))

    /** Add all of the key-value pairs in `right` to this multimap. */
    def combine(right: MultiMap[K, V]): MultiMap[K, V] =
      right.foldLeft(map) { (res, kvs) =>
        kvs match
          case (k, vs) => res.addAll(k, vs)
      }

    /** Create a multimap in which each value is paired with all keys which map to it.
      */
    def invert: MultiMap[V, K] =
      map.toSeq
        .flatMap { case (k, vs) => vs map (_ -> k) }
        .groupBy[V](_._1)
        .map { case (v, vks) => v -> vks.map(_._2) }

    /** Create a new multimap in which the key-value mappings are exactly those pairs `(k, w)` for which there exists a
      * `v` such that `(k, v)` is in this multimap, and `(v, w)` is in `right`.
      */
    // can't call this compose or it gets shadowed by PartialFunction's compose */
    def chain[W](next: MultiMap[V, W]): MultiMap[K, W] =
      map
        .map { case (k, i) => k -> i.flatterMap(next.get) }
        .filter { case (_, w) => w.nonEmpty }

    /** All key-value pairs in this multimap.
      */
    def distributed(): Iterator[(K, V)] =
      map.iterator.flatMap { case (k, vs) => vs.iterator map (k -> _) }
  end MultiMapOps
end MultiMap
