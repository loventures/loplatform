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

package com.learningobjects.cpxp.service.integration;

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.util.StringUtils;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class IntegrationServiceBean extends BasicServiceBean implements IntegrationService, IntegrationConstants {
    private static final Logger logger = Logger.getLogger(IntegrationServiceBean.class.getName());

    public Item findByUniqueId(Long system, String uniqueId, String type) {

        QueryBuilder qb = queryRoot(getCurrentDomain(), ITEM_TYPE_INTEGRATION);
        if (system != null) {
            qb.addCondition(DATA_TYPE_EXTERNAL_SYSTEM, "eq", system);
        }
        qb.addCondition(BaseCondition.getInstance(DATA_TYPE_UNIQUE_ID, "eq", StringUtils.lowerCase(uniqueId), "lower"));
        List<Item> results = qb.getItems();
        if (StringUtils.isEmpty(type)) {
            if (results.size() > 1) { // in an ambiguous case, assume a group by default..
                for (Item i : results) {
                    if (GroupConstants.ITEM_TYPE_GROUP.equals(i.getParent().getType())) {
                        return i;
                    }
                }
             }
            return com.google.common.collect.Iterables.getFirst(results, null);
        } else {
             for (Item i : results) {
                 if (type.equals(i.getParent().getType())) {
                     return i;
                 }
             }
        }
        return null;
    }

    public Item getSystemsFolder() {

        Item folder = findDomainItemById(getCurrentDomain().getId(), FOLDER_ID_SYSTEMS);

        return folder;
    }

    public Item getSystem(Long id) {

        Item system = _itemService.get(id);
        assertItemType(system, ITEM_TYPE_SYSTEM);

        return system;
    }
}
