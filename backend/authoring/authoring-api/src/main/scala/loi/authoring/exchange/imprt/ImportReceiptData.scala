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

package loi.authoring.exchange.imprt

/** This dto has data from the web request and gets saved in the ImportReceiptEntity data column.
  */

private[imprt] case class ImportReceiptData(
  description: String,
  targetBranchId: Long
) extends ReceiptData

private[imprt] case class QtiImportReceiptData(
  description: String,
  targetBranchId: Long,
  assessmentType: String,
  assessmentTitle: Option[String]
) extends ReceiptData

private[imprt] sealed trait ReceiptData:
  val description: String // used in the UI receipt table
  val targetBranchId: Long // I guess it's good to have
