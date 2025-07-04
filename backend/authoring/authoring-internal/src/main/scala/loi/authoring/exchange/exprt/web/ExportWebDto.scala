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

package loi.authoring.exchange.exprt.web

import java.util.UUID

import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.exchange.exprt.ExportReceipt

case class ExportWebDto(
  name: String,
  branchId: Long,
  rootNodeNames: Seq[UUID]
)

private[web] case class ExportReceiptsResponseDto(
  receipts: Seq[ExportReceipt],
  users: Map[Long, UserDTO]
)
