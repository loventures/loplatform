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

package loi.cp.role.impl;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.dto.FacadeQuery;
import com.learningobjects.cpxp.service.relationship.RelationshipConstants;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import loi.cp.role.SupportedRoleFacade;

import java.util.List;

@FacadeItem("*")
public interface SupportedRoleParentFacade extends Facade {
    @FacadeChild(RelationshipConstants.ITEM_TYPE_SUPPORTED_ROLE)
    public List<SupportedRoleFacade> getSupportedRoles();
    public SupportedRoleFacade addSupportedRole();
    void removeSupportedRole(Long id);
    @FacadeQuery(group = "SupportedRoles", projection = RelationshipConstants.DATA_TYPE_SUPPORTED_ROLE_ROLE)
    public List<RoleFacade> findRoles();
}
