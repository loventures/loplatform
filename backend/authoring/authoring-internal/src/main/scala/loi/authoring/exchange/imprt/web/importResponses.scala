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

package loi.authoring.exchange.imprt.web

import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.task.TaskReport
import loi.authoring.exchange.imprt.ImportReceipt
import loi.authoring.exchange.model.ExchangeManifest

private[imprt] case class ImportReceiptsResponse(
  receipts: Seq[ImportReceipt],
  users: Map[Long, UserDTO]
)

private[imprt] object ImportReceiptsResponse:
  def apply(
    receipt: ImportReceipt,
    userDto: UserDTO
  ): ImportReceiptsResponse =
    ImportReceiptsResponse(Seq(receipt), Map(userDto.id -> userDto))

// To pass back after finishing step 1 validation
// (in which an initial receiptId is made)
// of 2-step import process for non-LO zips.
private[imprt] case class ImportPreviewResponse(
  manifest: Option[ExchangeManifest],
  receiptId: Option[Long],
  taskReport: TaskReport
)
