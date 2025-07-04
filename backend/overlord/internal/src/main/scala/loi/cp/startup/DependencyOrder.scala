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

package loi.cp.startup

import scala.annotation.tailrec
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*

/** Typeclass describing a type that has both a primary partial order expressed as dependencies, and a secondary total
  * order.
  *
  * @tparam A
  *   the type in question
  */
trait DependencyOrder[A]:

  /** A predicate that selects values that this should precede.
    * @param a
    *   the value
    * @return
    *   the dependency predicate
    */
  def dependsOn(a: A): A => Boolean

  /** Provide the secondary total ordering of a sequence of values without regard for dependencies.
    * @param as
    *   the values
    * @return
    *   the sorted values
    */
  def sorted(as: Seq[A]): Seq[A]
end DependencyOrder

/** Dependency order typeclass companion.
  */
object DependencyOrder:

  /** A map from a value to the values that should precede it in dependency order. This in effect captures the edges of
    * the multi-tree that will be traversed depth first.
    * @tparam A
    *   the value type
    */
  type DependencyMap[A] = Map[A, Set[A]]

  /** A tuple of a value and the values that should precede it in dependency order.
    * @tparam A
    *   the value type
    */
  type DependencyEntry[A] = (A, Set[A])

  /** Support for computing the dependency ordering of a sequence of a dependency-ordered type.
    * @param as
    *   the values
    * @tparam A
    *   the value type
    */
  implicit class SeqDependencyOrderOps[A: DependencyOrder](val as: Seq[A]):

    /** Compute the total order of a sequence of values of a dependency-ordered type. This is a topological sort of the
      * values based primarily on their expressed dependencies and secondarily on their natural ordering. For example,
      * given the ordered values A, B, C and the dependency A -> B (i.e. A depends upon B), the total order would be B,
      * C, A. Note that although C is naturally ordered before A and both are non-dependent once B is resolved, the
      * natural ordering applies to all non-dependent elements at each iteration.
      *
      * @return
      *   the total ordering, if defined, or else the unsatisfiable dependency map
      */
    def dependencyOrder: DependencyMap[A] \/ List[A] =
      val dependencies = dependencyMap(as)
      val sorted       = implicitly[DependencyOrder[A]].sorted(as)
      dependencyOrdering(List.empty, sorted, dependencies)
  end SeqDependencyOrderOps

  /** Recursively determine the dependency ordering.
    *
    * @param order
    *   the ordering so far
    * @param as
    *   the remaining naturally ordered values
    * @param dependencies
    *   the remaining dependencies
    * @tparam A
    *   the type
    * @return
    *   the dependency ordering, if computable
    */
  @tailrec private def dependencyOrdering[A: DependencyOrder](
    order: List[A],
    as: Seq[A],
    dependencies: DependencyMap[A]
  ): DependencyMap[A] \/ List[A] =
    // partition into those that depend on other tasks and those that do not
    val (dependent, free) = as.partition(dependencies.contains)
    if dependent.isEmpty then // if there remain no dependent tasks we're done
      (order ++ free).right
    else if free.isEmpty then // if there are no free tasks, we have a loop
      dependencies.left
    else                      // clear the released dependencies and recurse
      dependencyOrdering(order ++ free, dependent, dependencies.flatMap(filterOut(free)))
  end dependencyOrdering

  /** Filter out a sequence of values from the dependencies of a dependency entry.
    * @param as
    *   the values to remove from the dependency entry.
    * @param entry
    *   a value and the values upon which it depends.
    * @tparam A
    *   the value type
    * @return
    *   an updated dependency entry, if there are any remaining dependencies
    */
  private def filterOut[A: DependencyOrder](as: Seq[A])(entry: DependencyEntry[A]): Option[DependencyEntry[A]] =
    val filtered = entry._2 -- as
    filtered.nonEmpty.option(entry._1 -> filtered)

  /** Convert a list of dependency-ordered values into a map from values to their dependencies.
    * @param as
    *   the values
    * @tparam A
    *   the value type
    * @return
    *   the dependency map
    */
  private def dependencyMap[A: DependencyOrder](as: Seq[A]): DependencyMap[A] =
    as.map(a => a -> as.filter(implicitly[DependencyOrder[A]].dependsOn(a)).toSet)
      .filterNot(_._2.isEmpty)
      .toMap
end DependencyOrder
