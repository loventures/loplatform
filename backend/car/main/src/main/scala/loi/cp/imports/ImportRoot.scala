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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentService,
  ComponentSupport
}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Service
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.imports.BatchImporterRootComponent

@Component
class ImportRoot(
  val componentInstance: ComponentInstance,
)(implicit cs: ComponentService, fs: FacadeService)
    extends ImportRootComponent
    with ComponentImplementation:

  private val importCoordinator = Service.service[ImportCoordinator]

  override def imports(q: ApiQuery): ApiQueryResults[ImportComponent] =
    val importQuery =
      ComponentSupport.get(classOf[BatchImporterRootComponent]).queryImports
    ApiQuerySupport.query(importQuery, q, classOf[ImportComponent])

  override def importers(): Seq[ImportType] =
    importCoordinator.getImportTypes.keys.toSeq

  override def importResource(q: ApiQuery): ImportResource =
    val importQuery =
      ComponentSupport.get(classOf[BatchImporterRootComponent]).queryImports
    ImportResource(
      importCoordinator.getImportTypes.keys.toSeq,
      ApiQuerySupport.query(importQuery, q, classOf[ImportComponent])
    )

  override def getImport(importId: Long): ImportComponent =
    importId
      .tryComponent[ImportComponent]
      .getOrElse(throw new ResourceNotFoundException(s"No import with id: $importId"))
end ImportRoot
