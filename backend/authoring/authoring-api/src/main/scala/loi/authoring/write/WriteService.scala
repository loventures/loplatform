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

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.project.BigCommit
import loi.authoring.workspace.{AttachedReadWorkspace, WriteWorkspace}
import loi.authoring.write.builder.WriteOpsState

import scala.util.Try

@Service
trait WriteService:

  def commit(ws: WriteWorkspace, ops: List[WriteOp], squash: Boolean = false): Try[CommitResult[AttachedReadWorkspace]]

  final def commit[A](
    ws: WriteWorkspace,
    ops: WriteOpsState[A]
  ): ((List[WriteOp], Try[CommitResult[AttachedReadWorkspace]]), A) =
    ops
      .modify(writeOps => (writeOps, commit(ws, writeOps)))
      .runEmpty
      .value

  def reverseWriteOps(ws: WriteWorkspace, commit: BigCommit): Either[ReversingError, List[WriteOp]]
end WriteService
