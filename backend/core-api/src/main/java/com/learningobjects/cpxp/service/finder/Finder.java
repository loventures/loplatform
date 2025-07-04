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

package com.learningobjects.cpxp.service.finder;

import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.entity.EntityUtils;
import com.learningobjects.cpxp.service.item.Item;

/**
 * The contract of an entity that operates within the item hierarchy.
 */
public interface Finder extends IdType {
    Long getId();

    void setId(final Long id);

    default Long id() {
        return getId();
    }

    Item getRoot();

    void setRoot(final Item root);

    default Item root() {
        return getRoot();
    }

    Item getOwner();

    void setOwner(final Item owner);

    default Item owner() {
        return getOwner();
    }

    Item getParent();

    void setParent(final Item parent);

    default Item parent() {
        return getParent();
    }

    String getPath();

    void setPath(final String path);

    default String path() {
        return getPath();
    }

    String getDel();

    void setDel(final String del);

    default String del() {
        return getDel();
    }

    default String getItemType() {
        return EntityUtils.getItemType(getClass());
    }

    ItemRelation getItemRelation();

    boolean isNew();

    void setNew(final boolean isNew);
}
