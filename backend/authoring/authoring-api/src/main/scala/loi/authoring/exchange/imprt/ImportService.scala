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

import com.learningobjects.cpxp.component.annotation.Service

@Service
trait ImportService:

  def loadImportReceipts(limit: Int, offset: Int): Seq[ImportReceipt]

  def loadImportReceipt(id: Long): Option[ImportReceipt]

  /** @param existingReceipt
    *   With non-loaf imports, we create a receipt on 1st (validation) step: `recordValidation`. We pass this receipt in
    *   on the 2nd (import) step to mutate the same receipt.
    */
  def deferImport(dto: ConvertedImportDto, existingReceipt: Option[ImportReceipt]): ImportReceipt

  def doImport(dto: ConvertedImportDto): ImportReceipt

  def deleteImportReceipt(receipt: ImportReceipt): Unit

  /** Creates a new ImportReceiptEntity with status of VALIDATED, the unconverted source, and the converted source (if
    * done in the validation step).
    */
  def recordValidation(dto: ValidatedImportDto): ImportReceipt
end ImportService
