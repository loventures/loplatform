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

package loi.cp.progress

import loi.cp.reference.EdgePath

/** A request to change a user's visitation-based progress against some content. Cannot change grade-based progress.
  */
sealed abstract class ProgressChange:

  /** The path of the leaf content to modify progress for. */
  val path: EdgePath

object ProgressChange:

  /** Mark the content as visited.
    *
    * This can mean simple visitation, for resources, or completed interaction, for assessments and assignments.
    */
  final case class Visited(path: EdgePath) extends ProgressChange
  def visited(path: EdgePath): ProgressChange = Visited(path)

  /** Unmark the content as visited.
    *
    * This is used to handle invalidation of interactions with content, such as instructor invalidation of attempts.
    */
  final case class Unvisit(path: EdgePath) extends ProgressChange
  def unvisit(path: EdgePath): ProgressChange = Unvisit(path)

  /** Mark the content as having been tested out of.
    *
    * This is granted based on a user achieving proficiency on taught competencies. Since it is not based on user action
    * against the content, it is not removed by an [[Unvisit]] ation.
    */
  final case class TestOut(path: EdgePath) extends ProgressChange
  def testOut(path: EdgePath): ProgressChange = TestOut(path)

  final case class Skipped(path: EdgePath) extends ProgressChange
  def skipped(path: EdgePath): ProgressChange = Skipped(path)
end ProgressChange
