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

import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.util.StringUtils;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static com.learningobjects.cpxp.service.relationship.RelationshipConstants.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class RelationshipServiceBean extends BasicServiceBean implements RelationshipService {
    private static final Logger logger = Logger.getLogger(RelationshipServiceBean.class.getName());

    /** The data service. */
    @Inject
    private DataService _dataService;

    ////
    // Role
    ////

    public Item getRole(Long id) {

        Item role = _itemService.get(id);
        assertItemType(role, ITEM_TYPE_ROLE);

        return role;
    }

    public void removeRole(Item role) {

        _itemService.destroy(role);

    }

    public Collection<Item> getSystemRoles() {

        Item folder = getDomainItemById(ID_FOLDER_ROLES);
        List<Item> roles = findByParentAndType(folder, ITEM_TYPE_ROLE);

        return roles;
    }

    public Collection<Item> getLocalRoles(Item item) {

        List<Item> roles = findByParentAndType(item, ITEM_TYPE_ROLE);

        return roles;
    }

    public Collection<Item> getLocalRoles(Item item, String query) {

        QueryBuilder qb = queryParent(item, ITEM_TYPE_ROLE);
        if (StringUtils.isNotEmpty(query)) {
            qb.addCondition(BaseCondition.getInstance(DataTypes.DATA_TYPE_NAME, "like", query.toLowerCase() + "%", "lower"));
        }

        return qb.getItems();
    }

    public void addSupportedRole(Item item, Item role) {

        Collection<Item> currentRoles = DataTransfer.findItemData(item, DATA_TYPE_SUPPORTED_ROLE);

        if (!currentRoles.contains(role)) {
            _dataService.createItem(item, DATA_TYPE_SUPPORTED_ROLE, role);
        }

    }

    public Collection<Item> getSupportedRoles(Item item) {

        Collection<Item> supportedRoles = DataTransfer.findItemData(item, DATA_TYPE_SUPPORTED_ROLE);

        return supportedRoles;
    }
}
