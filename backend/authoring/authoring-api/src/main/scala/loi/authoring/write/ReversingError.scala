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

package loi.authoring.write

import java.util.UUID

sealed abstract class ReversingError(val msg: String)

object ReversingError:

  case object IrreversibleCommit                     extends ReversingError(s"ops pattern is irreversible")
  case class IrreversibleOpType(opClassName: String) extends ReversingError(s"$opClassName is irreversible")
  case class NodeConflict(name: UUID)                extends ReversingError(s"node $name has more recent changes, undo them first")
  case class EdgeConflict(name: UUID)                extends ReversingError(s"edge $name has more recent changes, undo them first")

  // these are bs errors, cannot happen
  case object NoParentCommit                        extends ReversingError("no parent commit")
  case class NoSuchNode(name: UUID, commitId: Long) extends ReversingError(s"no such node $name in commit $commitId")
  case class NoSuchEdge(name: UUID, commitId: Long) extends ReversingError(s"no such edge $name in commit $commitId")
end ReversingError
