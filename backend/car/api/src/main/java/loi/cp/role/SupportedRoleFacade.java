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

package loi.cp.role;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.relationship.RelationshipConstants;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import loi.cp.right.Right;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A SupportedRole is a join of a {@link RoleFacade role} and some {@link Right rights}.
 */
@FacadeItem(RelationshipConstants.ITEM_TYPE_SUPPORTED_ROLE)
public interface SupportedRoleFacade extends Facade, Id {
    @FacadeData(RelationshipConstants.DATA_TYPE_SUPPORTED_ROLE_ROLE)
    RoleFacade getRole();
    void setRole(Id role);
    void setRole(String role); // temporary for bootstrapping

    @FacadeData(RelationshipConstants.DATA_TYPE_SUPPORTED_ROLE_ROLE)
    Long getRoleId();

    @FacadeData(RelationshipConstants.DATA_TYPE_SUPPORTED_ROLE_RIGHTS)
    RightsList getRights();

    /**
     * The list of {@link Right} this role has as a string.  If a right is specified, it's children in the type hierarchy will be applied to the role.
     * If you want to exclude one of the children in the hierarchy, prepend "-" in front of the right name.
     * @param rights
     */
    void setRights(RightsList rights);

    class RightsList extends ArrayList<String> {

        public RightsList() {
            super();
        }

        public RightsList(Collection<? extends String> c) {
            super(c);
        }
    }
}
