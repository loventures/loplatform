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
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

@Controller(value = "imports", root = true, category = Controller.Category.CONTEXTS)
trait ImportRootComponent extends ApiRootComponent:

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "imports", method = Method.GET)
  def imports(q: ApiQuery): ApiQueryResults[ImportComponent]

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "importers", method = Method.GET)
  def importers(): Seq[ImportType]

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "importerResource", method = Method.GET)
  def importResource(q: ApiQuery): ImportResource

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "imports/{importId}", method = Method.GET)
  def getImport(@PathVariable(value = "importId") importId: Long): ImportComponent

  /*
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "imports/{importId}/file", method = Method.GET)
  def getImportFile(@PathVariable(value = "importId") importId: Long): AttachmentComponent
   */
end ImportRootComponent
