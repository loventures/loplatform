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

package loi.cp.imports

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import jakarta.servlet.http.HttpServletRequest
import loi.cp.Widen
import loi.cp.admin.right.AdminRight
import loi.cp.imports.CsvImporterComponent.*
import scalaz.\/

@Controller(value = "csvImports", root = true, category = Controller.Category.CONTEXTS)
trait CsvImporterComponent extends ApiRootComponent:

  /** Sadly this is also used by remote scripts. TODO: Make the blessed public API a dedicated servlet, not an SRS API,
    * then return this to acecpting an [[ImportDTO]] as the [[RequestBody]].
    */
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "imports", method = Method.POST)
  def startImport(
    request: HttpServletRequest
  ): MatchError.type \/ ImportComponent

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "imports/validation", method = Method.POST)
  def validateImportFile(
    @RequestBody dto: ImportDTO,
  ): ErrorResponse \/ String // token for below method

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "imports/validation/status", method = Method.GET)
  def getValidationStatus(
    @QueryParam("token") token: String,
  ): ErrorResponse \/ CsvValidationStatus

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "imports/{importId}/errors/download", method = Method.GET)
  def downloadErrors(
    @PathVariable("importId") importId: Long,
    request: WebRequest
  ): ErrorResponse \/ WebResponse
end CsvImporterComponent

object CsvImporterComponent:

  /** Encapsulates an error in matching the type of an import along with a human readable message that will be useful in
    * the importer's cli interface.
    */
  case object MatchError:
    val message = "Could not determine import type."

  case class ImportDTO(uploadGuid: String, impl: String)

  sealed trait CsvValidationStatus extends Widen[CsvValidationStatus]

  object CsvValidationStatus:
    case object InProgress                                                              extends CsvValidationStatus
    case class Finished(streamStatusReport: StreamStatusReport, importType: ImportType) extends CsvValidationStatus
end CsvImporterComponent
