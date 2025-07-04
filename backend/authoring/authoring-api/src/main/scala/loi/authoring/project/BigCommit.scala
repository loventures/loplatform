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

package loi.authoring.project

import com.fasterxml.jackson.databind.JsonNode

import java.time.LocalDateTime
import java.util.UUID

final case class BigCommit(
  commit: Commit2,
  comboDoc: Commit2.ComboDoc,
  ops: JsonNode /* List[DbWriteOp] */,
):
  val id: Long                 = commit.id
  val rootName: UUID           = commit.rootName
  val homeName: UUID           = commit.homeName
  val created: LocalDateTime   = commit.created
  val createdBy: Option[Long]  = commit.createdBy
  val parentId: Option[Long]   = commit.parentId
  val kfDocId: Long            = commit.kfDocId
  val driftDocId: Option[Long] = commit.driftDocId
  val rootId: Long             = commit.rootId
end BigCommit
