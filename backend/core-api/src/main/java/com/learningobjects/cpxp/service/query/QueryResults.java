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

package com.learningobjects.cpxp.service.query;

import com.learningobjects.cpxp.service.item.Item;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;

public class QueryResults {
    private final Long _parent;
    private final String _itemType;
    private final Set<String> _invalidationKeys;
    private final CacheElementStorage _storage;

    public QueryResults(Item parent, String itemType, Set<String> invalidationKeys,
            CacheElementStorage storage) {
        _parent = (null == parent) ? null : parent.getId();
        _itemType = itemType;
        _invalidationKeys = invalidationKeys;
        _storage = storage;
    }

    public Long getParent() {
        return _parent;
    }

    public String getItemType() {
        return _itemType;
    }

    public Set<String> getInvalidationKeys() {
        return _invalidationKeys;
    }

    public List getResultList(EntityManager entityManager) {
        return _storage.resurrect(entityManager);
    }
}
