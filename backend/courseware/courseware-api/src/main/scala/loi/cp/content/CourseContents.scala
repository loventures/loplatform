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

import loi.asset.gradebook.GradebookCategory1
import loi.authoring.asset.Asset
import loi.cp.reference.EdgePath
import scaloi.syntax.option.*

import java.util.UUID
import scala.util.Try

/** Represents a complete tree of course contents rooted at the course node itself. */
final case class CourseContents(
  tree: ContentTree,
  categories: List[GradebookCategory] = Nil,
) /*(implicit ev: tree.rootLabel.asset.type <~< Course)*/:

  /** Return a preorder traversal of the course contents excluding the course node. */
  def nonRootElements: List[CourseContent] = tree.flatten.tail

  /** Get the content at `path`, if `path` is a valid [[EdgePath]] in this contents. */
  def get(path: EdgePath): Option[CourseContent] =
    nonRootElements.find(_.edgePath == path)

  /** Get the content with asset `name`, if `name` is a valid asset name in this contents. */
  def get(name: UUID): Option[CourseContent] =
    nonRootElements.find(_.asset.info.name == name)

  /** Get the content at `path`, if `path` is a valid [[EdgePath]] in this contents. */
  @throws[NoSuchElementException]("if the supplied path is not in this contents")
  def get_!(path: EdgePath): Try[CourseContent] = get(path).toTry(notFound(path))

  private def notFound(path: EdgePath) =
    val msg = s"Content $path is not found (in tree from ${tree.rootLabel.asset})"
    new NoSuchElementException(msg)
end CourseContents

final case class GradebookCategory(path: EdgePath, asset: Asset[GradebookCategory1])
