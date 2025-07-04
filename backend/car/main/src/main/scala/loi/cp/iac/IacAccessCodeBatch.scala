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

package loi.cp.iac

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import loi.cp.accesscode.{AbstractAccessCodeBatch, AccessCodeComponent, RedemptionComponent, RedemptionSuccess}
import scalaz.syntax.std.option.*

@Component
class IacAccessCodeBatch extends AbstractAccessCodeBatch with IacAccessCodeBatchComponent:
  @PostCreate
  def initEntitlement(init: IacAccessCodeBatchComponent): Unit =
    _instance.setAttribute("isbn", init.getISBN)

  override def getISBN: String =
    _instance.getAttribute("isbn", classOf[String])

  override def getDescription: String =
    s"ISBN: $getISBN. Import: $getFilename."

  private def getFilename: String =
    Option(_instance.getImport).cata(_.getFileName, "none")

  override def getUse: String = getISBN

  override def getUseName: String = "ISBN"

  override def importBatch(skipHeader: Boolean, uploadInfo: UploadInfo): Unit =
    val rows = loadCsv(uploadInfo, true)
    _instance.addImport(uploadInfo)
    super.importAccessCodes(rows)

  override def validateContext(context: JsonNode): Boolean =
    Option(context.get("isbn")).map(_.asText).contains(getISBN)

  override def redeemAccessCode(accessCode: AccessCodeComponent, redemption: RedemptionComponent): RedemptionSuccess =
    IacRedemptionSuccess(accessCode, getISBN)

  override def generateBatch(prefix: String, quantity: Long): Unit =
    _instance.setQuantity(quantity)
    generateAccessCodes(quantity, prefix)
end IacAccessCodeBatch
