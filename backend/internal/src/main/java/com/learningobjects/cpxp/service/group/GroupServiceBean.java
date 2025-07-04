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

package com.learningobjects.cpxp.service.group;

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.query.Direction;

import static com.learningobjects.cpxp.service.group.GroupConstants.*;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class GroupServiceBean extends BasicServiceBean implements GroupService {
    public Item getGroup(Long id) {

        Item group = _itemService.get(id);
        assertItemType(group, ITEM_TYPE_GROUP);

        return group;
    }

    public Item getGroupByGroupId(Item parent, String groupId) {

        QueryBuilder qb = queryParent(parent, ITEM_TYPE_GROUP);
        qb.addCondition(BaseCondition.getInstance(DATA_TYPE_GROUP_ID, "eq", groupId.toLowerCase(), "lower"));
        Item group = com.google.common.collect.Iterables.getFirst(qb.getItems(), null);

        return group;
    }

    public List<Item> getAllGroups(Item parent, int start, int limit, String dataType, boolean ascending) {

        QueryBuilder qb = queryParent(parent, ITEM_TYPE_GROUP);
        qb.setOrder(dataType, ascending ? Direction.ASC : Direction.DESC);
        qb.setFirstResult(start);
        qb.setLimit(limit);
        List<Item> groups = qb.getItems();

        return groups;
    }
}
