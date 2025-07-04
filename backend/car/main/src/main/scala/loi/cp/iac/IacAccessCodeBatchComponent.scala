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

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{QueryParam, RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.cp.accesscode.{AccessCodeAdminRight, AccessCodeBatchComponent, CsvValidationResponse}

@Schema(IacAccessCodeBatchComponent.Schema)
trait IacAccessCodeBatchComponent extends AccessCodeBatchComponent:
  @JsonProperty
  def getISBN: String

  @RequestMapping(path = "generate", method = Method.POST, async = true)
  @Secured(Array(classOf[AccessCodeAdminRight]))
  def generateBatch(@QueryParam prefix: String, @QueryParam quantity: Long): Unit

  @RequestMapping(path = "import", method = Method.POST, async = true)
  @Secured(Array(classOf[AccessCodeAdminRight]))
  def importBatch(@QueryParam skipHeader: Boolean, @QueryParam("upload") uploadInfo: UploadInfo): Unit

  @RequestMapping(path = "validateUpload", method = Method.GET)
  def validateUpload(@QueryParam("upload") uploadInfo: UploadInfo): CsvValidationResponse
end IacAccessCodeBatchComponent

object IacAccessCodeBatchComponent:
  final val Schema = "iacAccessCodeBatch"
