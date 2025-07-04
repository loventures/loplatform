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

package com.learningobjects.cpxp.service.relationship;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.query.QueryBuilder;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The relationship web service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class RelationshipWebServiceBean extends BasicServiceBean implements RelationshipWebService, RelationshipConstants {
    private static final Logger logger = Logger.getLogger(RelationshipWebServiceBean.class.getName());

    /** the relationship service */
    @Inject
    private RelationshipService _relationshipService;

    @Inject
    private FacadeService _facadeService;

    ////
    // Role
    ////

    public RoleFacade getRole(Long id) {

        Item role = _relationshipService.getRole(id);

        return _facadeService.getFacade(role, RoleFacade.class);
    }

    public RoleFacade getRoleByRoleId(Long parentId, String roleId) {

        Item parent = _itemService.get(parentId);
        QueryBuilder qb = queryParent(parent,RelationshipConstants.ITEM_TYPE_ROLE);
        qb.addCondition(BaseCondition.getInstance(RelationshipConstants.DATA_TYPE_ROLE_ID, "eq", roleId.toLowerCase(), "lower"));
        Item role = com.google.common.collect.Iterables.getFirst(qb.getItems(), null);

        return _facadeService.getFacade(role, RoleFacade.class);
    }


    public RoleFacade addRole(Long parentId) {
        return _facadeService.addFacade(parentId, RoleFacade.class);
    }

    public void removeRole(Long id) {

        Item role = _relationshipService.getRole(id);
        _relationshipService.removeRole(role);

    }

    public Set<RoleFacade> getSystemRoles() {

        Set<RoleFacade> roles = new HashSet<RoleFacade>();
        for (Item role: _relationshipService.getSystemRoles()) {
            RoleFacade facade = _facadeService.getFacade(role, RoleFacade.class);
            if ((facade != null)) {
                roles.add(facade);
            }
        }

        return roles;
    }

    public List<RoleFacade> getLocalRoles(Long itemId) {

        Item item = _itemService.get(itemId);

        List<RoleFacade> roles = new ArrayList<RoleFacade>();
        for (Item role: _relationshipService.getLocalRoles(item)) {
            RoleFacade facade = _facadeService.getFacade(role, RoleFacade.class);
            if ((facade != null)) {
                roles.add(facade);
            }
        }

        return roles;
    }

    public void addSupportedRole(Long itemId, Long id) {

        Item item = _itemService.get(itemId);
        Item role = _relationshipService.getRole(id);
        _relationshipService.addSupportedRole(item, role);

    }

    public List<RoleFacade> getSupportedRoles(Long itemId) {

        Item item = _itemService.get(itemId);

        List<RoleFacade> roles = new ArrayList<RoleFacade>();
        for (Item roleItem: _relationshipService.getSupportedRoles(item)) {
            RoleFacade role = _facadeService.getFacade(roleItem, RoleFacade.class);
            if (role != null) {
                roles.add(role);
            }
        }

        return roles;
    }

    public Long getRoleFolder() {

        Item folder = getDomainItemById(ID_FOLDER_ROLES);

        return getId(folder);
    }
}
