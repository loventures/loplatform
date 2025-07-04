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

package loi.cp.subtenant

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.subtenant.Subtenant.SubtenantDTO

@Service
class SubtenantServiceImpl(implicit domain: () => DomainDTO, fs: FacadeService) extends SubtenantService:
  override def findSubtenantByTenantId(tenantId: String): Option[Subtenant] =
    val subtenantFolder: SubtenantParentFacade = domain.facade[SubtenantParentFacade]
    subtenantFolder.findSubtenantByTenantId(tenantId)

  override def getOrCreateSubtenant(tenantId: String, name: String): Subtenant =
    val subtenantFolder: SubtenantParentFacade = domain.facade[SubtenantParentFacade]
    subtenantFolder
      .getOrCreateSubtenantByTenantId(tenantId, SubtenantDTO(tenantId, name, name, None))
      .update(_.setName(name))
      .result
end SubtenantServiceImpl
