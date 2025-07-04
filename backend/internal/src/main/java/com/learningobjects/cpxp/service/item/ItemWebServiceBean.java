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

package com.learningobjects.cpxp.service.item;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import com.learningobjects.cpxp.coherence.DeferredCoherence;
import com.learningobjects.cpxp.locache.AppCacheSupport;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.query.QueryCache;

/**
 * The item Web service implementation.
 */
@Stateless // TODO: declare in superclass? probably not..
@TransactionAttribute(TransactionAttributeType.REQUIRED) // TODO: declare in superclass? maybe.
public class ItemWebServiceBean extends BasicServiceBean implements ItemWebService {

    @Inject
    public ItemWebServiceBean(ItemService itemService, QueryCache queryCache) {
        super(itemService, queryCache);
    }

    /**
     * Get an item DTO.
     */
    public Item getItem(Long id) {
        return _itemService.get(id);
    }

    /**
     * Get an item type.
     */
    public String getItemType(Long id) {

        Item item = _itemService.get(id);
        String type = (item != null) ? item.getType() : null;

        return type;
    }

    /**
     * Get the parent id.
     */
    public Long getParentId(Long id) { // TODO: This is bogus

        // TODO: ACL

        Item item = _itemService.get(id);
        Long parentId = ((item == null) || (item.getParent() == null)) ? null : item.getParent().getId();

        return parentId;
    }

    public Long getById(String id) {

        // TODO: ACL

        Item item = getDomainItemById(id);

        return getId(item);
    }

    public Long findById(String id) {

        Item item = findDomainItemById(id);

        return getId(item);
    }

    public void evictFromCache(Long id) {
        // remove locally so it will not be visible in this transaction
        AppCacheSupport.removeItem(id);
        // queue to be removed across the cluster on commit
        DeferredCoherence.invalidateItem(id);
    }

    public Long create(Long parentId, String itemType) {
        return getId(_itemService.create(_itemService.get(parentId), itemType));
    }
}
