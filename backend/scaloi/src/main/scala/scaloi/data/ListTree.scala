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
package data

import scalaz.*
import scalaz.std.list.listInstance
import scalaz.syntax.foldable.*
import scalaz.syntax.std.boolean.*

import scala.annotation.nowarn
import scala.collection.{BuildFrom, IterableOnce, mutable}
import scala.language.implicitConversions
import scala.util.hashing.MurmurHash3

/** A tree backed by a [[List]].
  *
  * Isomorphic to `Cofree[List[T]]`, but contains extra tree-based methods.
  *
  * @param rootLabel
  *   The label at the root of this tree.
  * @param subForest
  *   The child nodes of this tree.
  * @see
  *   [[Cofree]]
  */
case class ListTree[A](
  rootLabel: A,
  subForest: List[ListTree[A]]
):
  import ListTree.*

  /** Run a bottom-up algorithm.
    *
    * This is the framework for several stackless methods, such as map.
    *
    * @param reduce
    *   is a function from a label and its mapped children to the new result.
    */
  private[data] def runBottomUp[B](reduce: A => mutable.ListBuffer[B] => B): B =
    val root  = BottomUpStackElem[A, B](None, this)
    var stack = root :: Nil

    while stack.nonEmpty do
      val here = stack.head
      if here.hasNext then
        val child         = here.next()
        val nextStackElem = BottomUpStackElem[A, B](Some(here), child)
        stack = nextStackElem :: stack
      else
        // The "here" node is completed, so add its result to its parents completed children.
        val result = reduce(here.rootLabel)(here.mappedSubForest)
        here.parent.foreach(_.mappedSubForest += result)
        stack = stack.tail
    end while

    reduce(root.rootLabel)(root.mappedSubForest)
  end runBottomUp

  /** Maps the elements of the ListTree into a Monoid and folds the resulting ListTree. */
  def foldMap[B: Monoid](f: A => B): B =
    foldLeft(Monoid[B].zero)((a, b) => Monoid[B].append(b, f(a)))

  def foldLeft[B](z: B)(f: (A, B) => B): B =
    foldLeftN(z)((label: A, _, acc: B) => f(label, acc))

  /** N for "node variant" as f is given the label and the children, i.e. the "node"
    */
  def foldLeftN[B](z: B)(f: (A, List[ListTree[A]], B) => B): B =
    var stack = List(this) :: Nil
    var acc   = z
    while stack.nonEmpty do
      val head :: tail = stack: @unchecked
      if head.isEmpty then stack = tail
      else
        val h2 :: t2 = head: @unchecked
        acc = f(h2.rootLabel, h2.subForest, acc)
        stack = h2.subForest :: t2 :: tail
    acc
  end foldLeftN

  def foldRight[B](z: B)(f: (A, => B) => B): B =
    rflatten.foldLeft(z)((a, b) => f(b, a))

  /** A 2D String representation of this ListTree. */
  def drawTree(implicit sh: Show[A]): String =
    toTree.drawTree

  /** A histomorphic transform. Each element in the resulting tree is a function of the corresponding element in this
    * tree and the histomorphic transform of its children.
    */
  def scanr[B](g: (A, List[ListTree[B]]) => B): ListTree[B] =
    runBottomUp(scanrReducer(g))

  /** Pre-order traversal. */
  def flatten: List[A] = rflatten.reverse

  /** Reverse pre-order traversal. */
  def rflatten: List[A] = foldLeft(List.empty[A])(_ :: _)

  def size: Int = foldLeft(0)((_, b) => b + 1)

  /** Breadth-first traversal. */
  def levels: List[List[A]] =
    var level = List(this)

    val result = mutable.ListBuffer.empty[List[A]]

    while level.nonEmpty do
      result += level.map(_.rootLabel)
      level = level.flatMap(_.subForest)

    result.toList
  end levels

  def toTree: Tree[A] =
    Tree.Node[A](rootLabel, subForest.toEphemeralStream.map(_.toTree))

  def foldNode[Z](f: A => List[ListTree[A]] => Z): Z =
    f(rootLabel)(subForest)

  def map[B](f: A => B): ListTree[B] =
    runBottomUp(mapReducer(f))

  def flatMap[B](f: A => ListTree[B]): ListTree[B] =
    runBottomUp(flatMapReducer(f))

  def flatCollect[B](pf: PartialFunction[A, B]): List[B] =
    flatten.collect(pf)

  def traverse1[G[_]: Apply, B](f: A => G[B]): G[ListTree[B]] =
    val G = Apply[G]

    subForest match
      case Nil     => G.map(f(rootLabel))(Leaf(_))
      case x :: xs =>
        G.apply2(f(rootLabel), NonEmptyList.nel(x, IList.fromList(xs)).traverse1(_.traverse1(f))) { case (h, t) =>
          Node(h, t.list.toList)
        }

  def zip[B](b: ListTree[B]): ListTree[(A, B)] =
    val root  = ZipStackElem[A, B](None, this, b)
    var stack = root :: Nil

    while stack.nonEmpty do
      val here = stack.head
      if here.hasNext then
        val (childA, childB) = here.next()
        val nextStackElem    = ZipStackElem[A, B](Some(here), childA, childB)
        stack = nextStackElem :: stack
      else
        // The "here" node is completed, so add its result to its parents completed children.
        val result = ListTree((here.a.rootLabel, here.b.rootLabel), here.mappedSubForest.toList)
        here.parent.foreach(_.mappedSubForest += result)
        stack = stack.tail
    end while

    ListTree((rootLabel, b.rootLabel), root.mappedSubForest.toList)
  end zip

  /** This implementation is 24x faster than the trampolined implementation for ListTreeTestJVM's hashCode test.
    *
    * @return
    */
  override def hashCode(): Int =
    MurmurHash3.listHash(rflatten, "ListTree".hashCode)

  override def equals(obj: scala.Any): Boolean =
    obj match
      case other: ListTree[?] =>
        ListTree.badEqInstance[A].equal(this, other.asInstanceOf[ListTree[A]])
      case _                  =>
        false

  // from scaloi syntax for other trees

  /** A catamorphism over a tree.
    *
    * `f` is invoked with the label of the root of `self` and with the result of folding `self`'s children with `f`.
    *
    * @note
    *   this may stack-overflow on very large trees.
    */
  def foldTree[B](f: (A, List[B]) => B): B =
    def loop(current: ListTree[A]): B =
      f(current.rootLabel, current.subForest.map(loop))
    loop(this)

  /** A top-down histomorphism over a tree.
    *
    * The tree is mapped with a function `f` that that can draw from the root label of each node and the mapped values
    * of ancestor nodes.
    */
  def tdhisto[B](f: (List[B], A) => B): ListTree[B] =
    tdhistoWithIndex((ancestors, label, _) => f(ancestors, label))

  def tdhistoWithIndex[B](f: (List[B], A, Int) => B): ListTree[B] =
    var i = -1

    def loop(tree: ListTree[A], ancestors: List[B]): ListTree[B] =
      i = i + 1
      val b = f(ancestors, tree.rootLabel, i)
      Node(b, tree.subForest.map(loop(_, b :: ancestors)))

    loop(this, Nil)

  /** [[tdhisto]], where the resulting element type is [[A]].
    *
    * This is used to work around Scala's local type inference algorithm.
    *
    * @see
    *   [[tdhisto]]
    */
  def tdhistendo(f: (List[A], A) => A): ListTree[A] = tdhisto(f)

  /** Left-biased tree filter. Errs on the side of exclusivity: If an ancestor is excluded then so too will be its
    * descendants.
    *
    * @param f
    *   the predicate
    * @return
    *   the resulting filtered tree, if any
    */
  def filtl(f: A => Boolean): Option[ListTree[A]] =
    def loop(tree: ListTree[A]): Option[ListTree[A]] = tree match
      case Node(content, subForest) =>
        f(content) option Node(content, subForest.flatMap(loop))
    loop(this)

  /** Right-biased tree filter. Errs on the side of inclusivity: If a descendant is included then so too will be its
    * ancestors.
    *
    * @param f
    *   the predicate
    * @return
    *   the resulting filtered tree, if any
    */
  def filtr(f: A => Boolean): Option[ListTree[A]] =
    def loop(tree: ListTree[A]): Option[ListTree[A]] = tree match
      case Node(content, subForest) =>
        val filteredForest = subForest.flatMap(loop)
        (f(content) || filteredForest.nonEmpty) option Node(content, filteredForest)
    loop(this)

  /** Finds matching subtrees within this tree. Will return multiple matches, but will not search descendants once it
    * collects a match. I.e. if the root matches, then the return will be a list of only the root.
    *
    * @param f
    *   the predicate
    * @return
    *   the resulting found subtrees, if any
    */
  def findSubtrees(f: A => Boolean): List[ListTree[A]] =
    def loop(tree: ListTree[A]): List[ListTree[A]] = tree match
      case node @ Node(content, subForest) =>
        if f(content) then List(node)
        else subForest.flatMap(loop)

    loop(this)

  /** Rebuild this tree, at each level mapping over the label and the to-be-mapped subforest.
    */
  def rebuild[B](f: (A, List[ListTree[B]]) => ListTree[B]): ListTree[B] =
    def loop(tree: ListTree[A]): ListTree[B] =
      f(tree.rootLabel, tree.subForest.map(loop))
    loop(this)

  /** Rebuild this tree without changing the element type.
    *
    * Can help with inference.
    */
  @inline
  def endoRebuild(f: (A, List[ListTree[A]]) => ListTree[A]): ListTree[A] = rebuild(f)

  /** Flatten this tree and then flatmap the resulting list with `f` */
  def flatterMap[B](f: A => IterableOnce[B])(implicit bf: BuildFrom[List[A], B, List[B]]): List[B] =
    flatten.flatMap(f)

  /** Flatten this tree and then flatten the result. */
  def flattern[B](implicit asTraversable: A => IterableOnce[B]): List[B] =
    flatterMap(asTraversable)

  /** Select the `ix`th subtree of this tree, if it exists. */
  def get(ix: Int): Option[ListTree[A]] = subForest.lift.apply(ix)

  /** Finds a node matching the given predicate and returns the path from the matching node to the root.
    */
  def findPath(f: A => Boolean): Option[List[ListTree[A]]] =
    import scalaz.syntax.std.boolean.*
    import scaloi.syntax.foldable.*
    def find(tree: ListTree[A], parents: List[ListTree[A]]): Option[List[ListTree[A]]] =
      val path = tree :: parents
      f(tree.rootLabel) option path orElse tree.subForest.findMap(find(_, path))
    find(this, Nil)

  /** Zip the tree's elements with their depth in the tree. */
  def zipWithDepth: ListTree[(A, Int)] =
    def loop(node: ListTree[A], depth: Int): ListTree[(A, Int)] = node match
      case Node(content, children) =>
        Node((content, depth), children.map(loop(_, 1 + depth)))
    loop(this, 0)

  def at(ixen: Int*): Option[A] =
    ixen
      .foldLeft(Option(this)) {
        case (Some(rest), ix) => rest.subForest.lift.apply(ix)
        case (None, _)        => None
      }
      .map(_.rootLabel)
end ListTree

sealed abstract class ListTreeInstances:
  implicit val listTreeInstance: Traverse1[ListTree] & Monad[ListTree] & Align[ListTree] & Zip[ListTree] =
    new Traverse1[ListTree] with Monad[ListTree] with Align[ListTree] with Zip[ListTree]:
      def point[A](a: => A): ListTree[A]                                                  = ListTree.Leaf(a)
      override def map[A, B](fa: ListTree[A])(f: A => B)                                  = fa `map` f
      def bind[A, B](fa: ListTree[A])(f: A => ListTree[B]): ListTree[B]                   = fa `flatMap` f
      def traverse1Impl[G[_]: Apply, A, B](fa: ListTree[A])(f: A => G[B]): G[ListTree[B]] = fa `traverse1` f
      override def foldRight[A, B](fa: ListTree[A], z: => B)(f: (A, => B) => B): B        = fa.foldRight(z)(f)
      override def foldMapRight1[A, B](fa: ListTree[A])(z: A => B)(f: (A, => B) => B)     =
        (fa.flatten.reverse: @unchecked) match
          case h +: t => t.foldLeft(z(h))((b, a) => f(a, b))
      override def foldLeft[A, B](fa: ListTree[A], z: B)(f: (B, A) => B): B               =
        fa.flatten.foldLeft(z)(f)
      override def foldMapLeft1[A, B](fa: ListTree[A])(z: A => B)(f: (B, A) => B): B      = fa.flatten match
        case h +: t => t.foldLeft(z(h))(f)
        case _      => z(fa.rootLabel) // can't happen but the compiler doesn't know
      override def foldMap[A, B](fa: ListTree[A])(f: A => B)(implicit F: Monoid[B]): B    = fa `foldMap` f

      // This implementation is 14x faster than the trampolined implementation for ListTreeTestJVM's align test.
      override def alignWith[A, B, C](f: A \&/ B => C): (ListTree[A], ListTree[B]) => ListTree[C] = (a, b) =>
        import ListTree.AlignStackElem
        val root  = AlignStackElem[A, B, C](None, \&/(a, b))
        var stack = root :: Nil

        while stack.nonEmpty do
          val here = stack.head
          if here.hasNext then
            val nextChildren  = here.next()
            val nextStackElem = AlignStackElem[A, B, C](Some(here), nextChildren)
            stack = nextStackElem :: stack
          else
            // The "here" node is completed, so add its result to its parents completed children.
            val result = ListTree[C](f(here.trees.bimap(_.rootLabel, _.rootLabel)), here.mappedSubForest.toList)
            here.parent.foreach(_.mappedSubForest += result)
            stack = stack.tail
        end while

        ListTree(f(root.trees.bimap(_.rootLabel, _.rootLabel)), root.mappedSubForest.toList)

      override def zip[A, B](a: => ListTree[A], b: => ListTree[B]): ListTree[(A, B)] =
        a.zip(b)

  implicit def treeEqual[A](implicit A0: Equal[A]): Equal[ListTree[A]] =
    new ListTreeEqual[A]:
      def A = A0

  implicit def treeOrder[A](implicit A0: Order[A]): Order[ListTree[A]] =
    new Order[ListTree[A]] with ListTreeEqual[A]:
      def A                                              = A0
      override def order(x: ListTree[A], y: ListTree[A]) =
        A0.order(x.rootLabel, y.rootLabel) match
          case Ordering.EQ =>
            std.list.listOrder[ListTree[A]].order(x.subForest, y.subForest)
          case x           => x
end ListTreeInstances

object ListTree extends ListTreeInstances:

  /** Node represents a tree node that may have children.
    *
    * You can use Node for tree construction or pattern matching.
    */
  object Node:
    def apply[A](root: A, forest: List[ListTree[A]]): ListTree[A] =
      ListTree[A](root, forest)

    def unapply[A](t: ListTree[A]): Some[(A, List[ListTree[A]])] = Some((t.rootLabel, t.subForest))

  /** Leaf represents a tree node with no children.
    *
    * You can use Leaf for tree construction or pattern matching.
    */
  object Leaf:
    def apply[A](root: A): ListTree[A] =
      Node(root, Nil)

    def unapply[A](t: ListTree[A]): Option[A] =
      t match
        case Node(root, List()) =>
          Some(root)
        case _                  =>
          None
  end Leaf

  def unfoldForest[A, B](s: List[A])(f: A => (B, List[A])): List[ListTree[B]] =
    s.map(unfoldTree(_)(f))

  def unfoldTree[A, B](v: A)(f: A => (B, List[A])): ListTree[B] =
    f(v) match
      case (a, bs) => Node(a, unfoldForest(bs)(f))

  // Only used for .equals.
  @nowarn private def badEqInstance[A] = new ListTreeEqual[A]:
    override def A: Equal[A] = _ `equals` _

  /** This implementation is 16x faster than the trampolined implementation for ListTreeTestJVM's scanr test.
    */
  private def scanrReducer[A, B](
    f: (A, List[ListTree[B]]) => B
  )(rootLabel: A)(subForest: mutable.ListBuffer[ListTree[B]]): ListTree[B] =
    val subForestList = subForest.toList
    ListTree[B](f(rootLabel, subForestList), subForestList)

  /** This implementation is 10x faster than mapTrampoline for ListTreeTestJVM's map test.
    */
  private def mapReducer[A, B](
    f: A => B
  )(rootLabel: A)(subForest: mutable.ListBuffer[ListTree[B]]): ListTree[B] =
    ListTree[B](f(rootLabel), subForest.toList)

  /** This implementation is 9x faster than flatMapTrampoline for ListTreeTestJVM's flatMap test.
    */
  private def flatMapReducer[A, B](
    f: A => ListTree[B]
  )(root: A)(subForest: mutable.ListBuffer[ListTree[B]]): ListTree[B] =
    val ListTree(rootLabel0, subForest0) = f(root)
    ListTree(rootLabel0, subForest0 ++ subForest)

  private final case class BottomUpStackElem[A, B](
    parent: Option[BottomUpStackElem[A, B]],
    tree: ListTree[A]
  ) extends Iterator[ListTree[A]]:
    private var subPosition = tree.subForest

    private[data] def rootLabel = tree.rootLabel

    private[data] val mappedSubForest = mutable.ListBuffer.empty[B]

    override def hasNext: Boolean = subPosition.nonEmpty

    override def next(): ListTree[A] =
      val head = subPosition.head
      subPosition = subPosition.tail
      head
  end BottomUpStackElem

  private final case class ZipStackElem[A, B](
    parent: Option[ZipStackElem[A, B]],
    a: ListTree[A],
    b: ListTree[B]
  ) extends Iterator[(ListTree[A], ListTree[B])]:
    private var subPosition = STreeZip(a.subForest, b.subForest)

    private[data] val mappedSubForest = mutable.ListBuffer.empty[ListTree[(A, B)]]

    override def hasNext: Boolean = subPosition.as.nonEmpty && subPosition.bs.nonEmpty

    override def next(): (ListTree[A], ListTree[B]) =
      val head = (subPosition.as.head, subPosition.bs.head)
      subPosition = STreeZip(subPosition.as.tail, subPosition.bs.tail)
      head
  end ZipStackElem

  private[data] final case class AlignStackElem[A, B, C](
    parent: Option[AlignStackElem[A, B, C]],
    trees: \&/[ListTree[A], ListTree[B]]
  ) extends Iterator[\&/[ListTree[A], ListTree[B]]]:
    private var subPosition = STreeZip(
      trees.a.map(_.subForest).getOrElse(List.empty),
      trees.b.map(_.subForest).getOrElse(List.empty)
    )

    private[data] val mappedSubForest = mutable.ListBuffer.empty[ListTree[C]]

    override def hasNext: Boolean = subPosition.as.nonEmpty || subPosition.bs.nonEmpty

    override def next(): \&/[ListTree[A], ListTree[B]] =
      subPosition match
        case STreeZip(a :: aTail, b :: bTail) =>
          subPosition = STreeZip(aTail, bTail)
          \&/.Both(a, b)
        case STreeZip(a :: aTail, Nil)        =>
          subPosition = STreeZip(aTail, Nil)
          \&/.This(a)
        case STreeZip(Nil, b :: bTail)        =>
          subPosition = STreeZip(Nil, bTail)
          \&/.That(b)
        case STreeZip(Nil, Nil)               =>
          throw new NoSuchElementException("reached iterator end")
  end AlignStackElem

  implicit def ToListTreeUnzip[A1, A2](root: ListTree[(A1, A2)]): ListTreeUnzip[A1, A2] =
    new ListTreeUnzip[A1, A2](root)
end ListTree

private trait ListTreeEqual[A] extends Equal[ListTree[A]]:
  def A: Equal[A]

  // This implementation is 4.5x faster than the trampolined implementation for ListTreeTestJVM's equal test.
  override final def equal(a1: ListTree[A], a2: ListTree[A]): Boolean =
    import ListTree.Node

    if !A.equal(a1.rootLabel, a2.rootLabel) then return false

    var stack = STreeZip(a1.subForest, a2.subForest) :: Nil

    while stack.nonEmpty do
      stack match
        case STreeZip(Node(childA1, childrenA1) :: a1Tail, Node(childA2, childrenA2) :: a2Tail) :: tail =>
          if !A.equal(childA1, childA2) then return false
          stack = STreeZip(a1Tail, a2Tail) :: STreeZip(childrenA1, childrenA2) :: tail
        case STreeZip(Nil, Nil) :: tail                                                                 =>
          stack = tail
        case _                                                                                          =>
          return false

    true
  end equal
end ListTreeEqual

final class ListTreeUnzip[A1, A2](private val root: ListTree[(A1, A2)]) extends AnyVal:
  private def unzipCombiner(
    rootLabel: (A1, A2)
  )(accumulator: mutable.ListBuffer[(ListTree[A1], ListTree[A2])]): (ListTree[A1], ListTree[A2]) =
    (ListTree(rootLabel._1, accumulator.map(_._1).toList), ListTree(rootLabel._2, accumulator.map(_._2).toList))

  /** Turns a tree of pairs into a pair of trees. */
  def unzip: (ListTree[A1], ListTree[A2]) =
    root.runBottomUp[(ListTree[A1], ListTree[A2])](unzipCombiner)

private[data] final case class STreeZip[A, B](
  as: List[ListTree[A]],
  bs: List[ListTree[B]],
)
