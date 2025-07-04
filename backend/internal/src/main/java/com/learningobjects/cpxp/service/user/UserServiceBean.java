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

package com.learningobjects.cpxp.service.user;

import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.QueryBuilder;

import javax.annotation.Nullable;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Logger;

import static com.learningobjects.cpxp.service.user.UserConstants.*;

/**
 * User service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class UserServiceBean extends BasicServiceBean implements UserService {
    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getName());

    public Item get(Long id) {

        Item item = _itemService.get(id);
        assertItemType(item, ITEM_TYPE_USER);

        return item;
    }

    @Nullable
    public Item getByUserName(Item parent, String userName) {

        QueryBuilder qb = queryParent(parent, ITEM_TYPE_USER);
        qb.addCondition(BaseCondition.getInstance(DATA_TYPE_USER_NAME, "eq", userName.toLowerCase(), "lower"));

        List<Item> results = qb.getItems();
        if(results.size() > 1){
            logger.severe(String.format("Multiple users found matching name %s in %d", userName, parent.getId()));
        }

        // TODO: getOnly and throw.  getFirst is ridiculously dangerous in this context.
        Item user = com.google.common.collect.Iterables.getFirst(results, null);

        return user;
    }

    public Item getUserFolder() {
        return getUserFolder(Current.getDomain());
    }

    public Item getUserFolder(Long domainId) {

        Item usersFolder = getDomainItemById(domainId, UserConstants.ID_FOLDER_USERS);

        return usersFolder;
    }
}
