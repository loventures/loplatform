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

package loi.cp.path

import scala.jdk.CollectionConverters.*

object PathUtils:
  def pathOf(elements: Long*): Path = fromElements(elements.map(_.toString)*)

  def fromElements(elements: String*): Path =
    new Path(elements.map(_.toString).asJava)

  implicit class PathOps(val path: Path) extends AnyVal:
    def ++(element: String): Path =
      path.append(element)

    def ++(element: Number): Path =
      path.append(element.toString)
end PathUtils
