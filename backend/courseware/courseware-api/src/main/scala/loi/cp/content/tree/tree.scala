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

package loi.cp.content
package tree

import loi.cp.reference.EdgePath
import scaloi.putty.*
import scala.deriving.Mirror

/** Utilities for manipulating [[AnnotatedTree]] s.
  */
object `package`:

  /** Create a tree processor that adds the result of invoking `f` on each content node in the tree.
    */
  def mapped[Result](f: CourseContent => Result) =
    new MapTree[Result]:
      protected def run(content: CourseContent) = f(content)

  /** Create a tree processor that adds the mapped value of each content node edge path in the tree.
    */
  def mappedByEdgePath[Result](map: Map[EdgePath, Result]) =
    new MapTree[Option[Result]]:
      protected def run(content: CourseContent) = map.get(content.edgePath)

  /** Create a tree processor that adds the result of invoking `f` on each content node in the tree, and the result of
    * processing its parents.
    *
    * Probably some kind of histomorphism.
    */
  def mappedWithParents[Result](f: (CourseContent, List[Result]) => Result) =
    new MapTreeWithParents[Result]:
      protected def run(cc: CourseContent, parents: List[Result]) =
        f(cc, parents)

  /** Create a tree processor that extracts a value of type `Param` from the annotations on the tree's content, and adds
    * the result of invoking `f` with that parameter and the content node to the annotations.
    *
    * @note
    *   this does not remove any `Param` from the annotations.
    */
  def mappedGiven[Param, Result](f: (CourseContent, Param) => Result) =
    new MapTreeGiven[Param, Result]:
      protected def run(cc: CourseContent, param: Param) = f(cc, param)

  /** Select a value of type `Params` from the annotations on the tree's content, and add a value of type `Result` to
    * the remainder.
    *
    * @tparam Params
    *   a structure of types which should be found in the tree's annotations. This can be a tuple or a case class.
    */
  def combine[Params <: Product, Result](f: (CourseContent, Params) => Result) =
    new CombineInTree[Params, Result]:
      protected def run(cc: CourseContent, params: Params) = f(cc, params)
end `package`

// implementations of the above functions, to keep their signatures pretty.

sealed abstract class MapTree[Result]:
  protected def run(cc: CourseContent): Result

  final def apply[Data <: Tuple](
    tree: AnnotatedTree[Data],
  ): AnnotatedTree[Result *: Data] = tree.map { case (data, content) =>
    (run(content) *: data, content)
  }

sealed abstract class MapTreeWithParents[Result]:
  protected def run(cc: CourseContent, parents: List[Result]): Result

  final def apply[Data <: Tuple](
    tree: AnnotatedTree[Data],
  ): AnnotatedTree[Result *: Data] =
    tree.tdhisto[(Result *: Data, CourseContent)] { (parents, here) =>
      val (data, content) = here
      val result          = run(content, parents.map(_._1.head))
      (result *: data, content)
    }
end MapTreeWithParents

sealed abstract class MapTreeGiven[Param, Result]:
  protected def run(cc: CourseContent, param: Param): Result

  final def apply[Data <: Tuple](tree: AnnotatedTree[Data])(implicit
    Sel: Select.Aux[Param, Data]
  ): AnnotatedTree[Result *: Data] = tree.map { case (data, content) =>
    (run(content, Sel(data)) *: data, content)
  }

sealed abstract class CombineInTree[Params <: Product, Result]:
  protected def run(cc: CourseContent, params: Params): Result

  final def apply[Data <: Tuple](tree: AnnotatedTree[Data])(using
    m: Mirror.ProductOf[Params],
    Rm: RemoveAll.Aux[m.MirroredElemTypes, Data]
  ): AnnotatedTree[Result *: RemoveAll[m.MirroredElemTypes, Data]] = tree.map { case (data, content) =>
    val (params, remaining) = Rm(data)
    (run(content, m.fromTuple(params)) *: remaining, content)
  }
end CombineInTree
