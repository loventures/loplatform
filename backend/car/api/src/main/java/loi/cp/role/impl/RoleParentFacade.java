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
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.relationship.RelationshipConstants;
import com.learningobjects.cpxp.service.relationship.RoleFacade;

import java.util.List;

@FacadeItem("*")
public interface RoleParentFacade extends Facade {
    @FacadeChild(RelationshipConstants.ITEM_TYPE_ROLE)
    public List<RoleFacade> getRoles();
    public RoleFacade addRole();
    public void removeRole(Long id);
    public QueryBuilder queryRoles();
    public boolean lock(boolean pessimistic);
}
