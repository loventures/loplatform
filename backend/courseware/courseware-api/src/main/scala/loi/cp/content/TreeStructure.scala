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

import loi.cp.reference.EdgePath
import scaloi.data.ListTree.Node

/** Annotates a tree with tree structure. */
object TreeStructure:
  private type StructuredTree = AnnotatedTree[List[EdgePath] *: Int *: EmptyTuple]

  /** Annotates a content tree with each node's index and parent path. */
  def addStructure(tree: ContentTree): StructuredTree =
    def loop(tree: ContentTree, index: Int, parents: List[EdgePath]): StructuredTree = tree match
      case Node(content, subForest) =>
        val path = content.edgePath :: parents
        Node((parents *: index *: EmptyTuple, content), subForest.zipWithIndex.map(t => loop(t._1, t._2, path)))
    loop(tree, 0, Nil)

  /** Returns a map linking each edgepath to its index (under the immediate parent) and its full parent paths. */
  def getParentStructure(tree: ContentTree): Map[EdgePath, (Int, List[EdgePath])] =
    def loop(tree: ContentTree, index: Int, parents: List[EdgePath]): List[(EdgePath, (Int, List[EdgePath]))] =
      tree match
        case Node(content, subForest) =>
          val head             = content.edgePath -> ((index, parents))
          val accumulatedPaths = content.edgePath :: parents
          head :: subForest.zipWithIndex.flatMap { case (subTree, i) => loop(subTree, i, accumulatedPaths) }
    loop(tree, 0, Nil).toMap
end TreeStructure
