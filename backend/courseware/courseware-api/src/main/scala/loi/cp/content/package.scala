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

import loi.cp.context.ContextId
import scaloi.data.ListTree
import scaloi.putty.Select

import java.time.Instant

object `package`:

  /** A piece of content's due date. */
  type DueDate <: Instant
  def DueDate(i: Instant): DueDate = i.asInstanceOf[DueDate]

  /** Just a tree of [[CourseContent]] s.
    */
  type ContentTree = ListTree[CourseContent]

  /** A tree of [[CourseContent]] s, each annotated with the data [[A]]
    *
    * @tparam A
    *   the data which annotate the tree. Usually a [[HList]].
    */
  type AnnotatedTree[A] = ListTree[(A, CourseContent)]

  implicit class ContentTreeOps(self: ContentTree):

    /** Annotate this tree with nothing at all. */
    def annotate: AnnotatedTree[EmptyTuple] =
      self.map(EmptyTuple -> _)

  implicit class AnnotatedTreeOps[Data <: Tuple](self: AnnotatedTree[Data]):
    def unannotated: ContentTree = self.map(_._2)

    def annotationsTree: ListTree[Data] = self.map(_._1)

    def annotationTree[Ann](using Sel: Select.Aux[Ann, Data]): ListTree[Ann] =
      annotationsTree.map(Sel)

  def courseAssetUrl(context: ContextId, content: CourseContent): String =
    s"/api/v2/lwc/${context.id}/asset/${content.edgePath}/"

  def courseAssetInstructionsUrl(context: ContextId, content: CourseContent): String =
    courseAssetUrl(context, content) + "instructions/"
end `package`
