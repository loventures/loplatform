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

import javax.ejb.Local;

/**
 * The item Web service.
 */
@Local
public interface ItemWebService {
    /**
     * Get an item.
     *
     * @param id
     *            the item id
     *
     * @return the item
     */
    public Item getItem(Long id);

    /**
     * Get an item type.
     *
     * @param id
     *            the item id
     *
     * @return the type
     */
    public String getItemType(Long id);

    /**
     * Get the parent id.
     *
     * @param id
     *            the item id
     *
     * @return the id of the parent, or null
     */
    public Long getParentId(Long id);

    /**
     * @param id
     *            the id value
     */
    public Long getById(String id);

    public Long findById(String id);

    public Long create(Long parentId, String itemType);

    /**
     * Allow the presentation layer to request cache invalidation, locally and
     * across the cluster; useful when an update/edit wouldn't affect a parent
     * so that inconsistent caches of descendants would lead to surprising
     * results.
     *
     * @param id primary key of an item
     */
    public void evictFromCache(Long id);
}
