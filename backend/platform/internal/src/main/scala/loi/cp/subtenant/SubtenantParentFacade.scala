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

import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.dto.{Facade, FacadeComponent, FacadeCondition, FacadeItem}
import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.cpxp.service.query.{QueryBuilder, Function as QBFunction}
import com.learningobjects.cpxp.service.subtenant.SubtenantConstants
import loi.cp.subtenant.Subtenant.SubtenantDTO
import scaloi.GetOrCreate

@FacadeItem(DomainConstants.ITEM_TYPE_DOMAIN)
trait SubtenantParentFacade extends Facade:

  @FacadeComponent
  def getSubtenant(id: Long)(implicit dm: DataModel[Subtenant]): Option[Subtenant]

  def querySubtenants(implicit dm: DataModel[Subtenant]): QueryBuilder

  def getOrCreateSubtenantByTenantId(
    @FacadeCondition(value = SubtenantConstants.DATA_TYPE_TENANT_ID, function = QBFunction.LOWER)
    tenantId: String,
    init: SubtenantDTO,
  )(implicit dm: DataModel[Subtenant]): GetOrCreate[Subtenant]

  def findSubtenantByTenantId(
    @FacadeCondition(value = SubtenantConstants.DATA_TYPE_TENANT_ID, function = QBFunction.LOWER)
    tenantId: String
  )(implicit dm: DataModel[Subtenant]): Option[Subtenant]

  def lock(pessimistic: Boolean): Boolean
end SubtenantParentFacade
