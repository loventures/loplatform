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

package loi.authoring.index

import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationConstants
import loi.cp.integration.{AbstractSystem, SystemComponent}

trait EsSystem extends SystemComponent[EsSystem]

@Component(name = "Elastic Search")
class EsSystemImpl extends AbstractSystem[EsSystem] with EsSystem:
  @PostCreate
  def initEsSystem(): Unit =
    facade.setSystemId("es")
    facade.setName("ElasticSearch")
    facade.setDisabled(false)

object EsSystemImpl:
  def getOrCreate()(implicit fs: FacadeService): EsSystem =
    IntegrationConstants.FOLDER_ID_SYSTEMS
      .facade[SystemParentFacade]
      .getOrCreateEsSystem[EsSystemImpl](classOf[EsSystemImpl].getName)
