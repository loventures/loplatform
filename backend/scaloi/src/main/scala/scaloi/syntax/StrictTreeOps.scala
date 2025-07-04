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

import scalaz.StrictTree
import scalaz.StrictTree.Node
import scalaz.std.vector.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.foldable.*

final class StrictTreeOps[A](private val self: StrictTree[A]) extends AnyVal:

  /** A catamorphism over a tree.
    *
    * `f` is invoked with the label of the root of `self` and with the result of folding `self`'s children with `f`.
    *
    * @note
    *   this may stack-overflow on very large trees.
    */
  def foldTree[B](f: (A, Vector[B]) => B): B =
    def loop(current: StrictTree[A]): B =
      f(current.rootLabel, current.subForest.map(loop))
    loop(self)

  /** A top-down histomorphism over a tree.
    *
    * The tree is mapped with a function `f` that that can draw from the root label of each node and the mapped values
    * of ancestor nodes.
    */
  def tdhisto[B](f: (List[B], A) => B): StrictTree[B] =
    def loop(tree: StrictTree[A], ancestors: List[B]): StrictTree[B] =
      val b = f(ancestors, tree.rootLabel)
      Node(b, tree.subForest.map(loop(_, b :: ancestors)))
    loop(self, Nil)

  /** Select the `ix`th subtree of this tree, if it exists. */
  def get(ix: Int): Option[StrictTree[A]] = self.subForest.lift.apply(ix)

  /** Finds a node matching the given predicate and returns the path from the matching node to the root.
    */
  def findPath(f: A => Boolean): Option[List[StrictTree[A]]] =
    def find(tree: StrictTree[A], parents: List[StrictTree[A]]): Option[List[StrictTree[A]]] =
      val path = tree :: parents
      f(tree.rootLabel) option path orElse tree.subForest.findMap(find(_, path))
    find(self, Nil)

  /** Zip the tree's elements with their depth in the tree. */
  def zipWithDepth: StrictTree[(A, Int)] =
    def loop(node: StrictTree[A], depth: Int): StrictTree[(A, Int)] =
      Node((node.rootLabel, depth), node.subForest.map(loop(_, 1 + depth)))
    loop(self, 0)
end StrictTreeOps

trait ToStrictTreeOps:
  import language.implicitConversions

  @inline implicit final def scaloiStrictTreeOps[A](self: StrictTree[A]): StrictTreeOps[A] =
    new StrictTreeOps[A](self)
