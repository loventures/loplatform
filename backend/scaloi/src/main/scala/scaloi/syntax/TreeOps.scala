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

import scalaz.Tree.Node
import scalaz.syntax.comonad.*
import scalaz.syntax.foldable.*
import scalaz.{EphemeralStream, Need, Tree}
import scaloi.syntax.boolean.*
import scaloi.syntax.eStream.*

final class TreeOps[A](private val self: Tree[A]) extends AnyVal:

  /** A catamorphism over a tree.
    *
    * `f` is invoked with the label of the root of `self` and with the result of folding `self`'s children with `f`.
    *
    * @note
    *   this may stack-overflow on very large trees.
    */
  def foldTree[B](f: (A, => EphemeralStream[B]) => B): B =
    def loop(current: Tree[A]): B =
      lazy val folded = current.subForest.map(loop) // `Stream` not lazy enough
      f(current.rootLabel, folded)

    loop(self)

  /** A top-down histomorphism over a tree.
    *
    * The tree is mapped with a function `f` that that can draw from the root label of each node and the mapped values
    * of ancestor nodes.
    */
  def tdhisto[B](f: (=> LazyList[B], A) => B): Tree[B] =
    def loop(tree: Tree[A], ancestors: => LazyList[B]): Tree[B] =
      val bs     = Need(ancestors)
      lazy val b = f(bs.value, tree.rootLabel)
      Node(b, tree.subForest.map(loop(_, b #:: bs.value)))
    loop(self, LazyList.empty)

  /** Left-biased tree filter. Errs on the side of exclusivity: If an ancestor is excluded then so too will be its
    * descendants.
    *
    * @param f
    *   the predicate
    * @return
    *   the resulting filtered tree, if any
    */
  def filtl(f: A => Boolean): Option[Tree[A]] =
    def loop(tree: Tree[A]): EphemeralStream[Tree[A]] =
      f(tree.rootLabel) `optionES` Node(tree.rootLabel, tree.subForest.flatMap(loop))
    loop(self).headOption

  /** Right-biased tree filter. Errs on the side of inclusivity: If a descendant is included then so too will be its
    * ancestors.
    *
    * @param f
    *   the predicate
    * @return
    *   the resulting filtered tree, if any
    */
  def filtr(f: A => Boolean): Option[Tree[A]] =
    def loop(tree: Tree[A]): EphemeralStream[Tree[A]] =
      lazy val filteredForest = tree.subForest.flatMap(loop)
      (f(tree.rootLabel) || filteredForest.nonEmpty) `optionES` Node(tree.rootLabel, filteredForest)
    loop(self).headOption

  /** Rebuild this tree, at each level mapping over the label and the to-be-mapped subforest.
    */
  def rebuild[B](f: (A, => EphemeralStream[Tree[B]]) => Tree[B]): Tree[B] =
    def loop(tree: Tree[A]): Tree[B] =
      f(tree.rootLabel, tree.subForest.map(loop))
    loop(self)

  /** Rebuild this tree without changing the element type.
    *
    * Can help with inference.
    */
  @inline
  def endoRebuild(f: (A, => EphemeralStream[Tree[A]]) => Tree[A]): Tree[A] = rebuild(f)

  /** Select the `ix`th subtree of this tree, if it exists. */
  def get(ix: Int): Option[Tree[A]] = self.subForest.index(ix)

  /** Map the values in this tree along with their position relative to their parent's sub-forest.
    */
  def mapWithIndices[B](f: (Int, A) => B): Tree[B] =
    self.loc.coflatMap { here =>
      val ix = here.lefts.length
      f(ix, here.tree.rootLabel)
    }.tree

  /** Zip the tree's elements with their depth in the tree. */
  def zipWithDepth: Tree[(A, Int)] =
    def loop(node: Tree[A], depth: Int): Tree[(A, Int)] =
      Node((node.rootLabel, depth), node.subForest.map(loop(_, 1 + depth)))
    loop(self, 0)
end TreeOps

trait ToTreeOps:
  import language.implicitConversions

  @inline implicit final def ToTreeOps[A](self: Tree[A]): TreeOps[A] =
    new TreeOps[A](self)

  // help avoid name clashes
  @inline implicit final def ToZTreeOps[A](self: A): scalaz.syntax.TreeOps[A] =
    new scalaz.syntax.TreeOps[A](self)
