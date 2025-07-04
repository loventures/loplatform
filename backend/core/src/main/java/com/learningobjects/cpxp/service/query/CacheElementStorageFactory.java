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

import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.item.Item;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory responsible for choosing a storage strategy for query results that
 * need to be cached.
 */
class CacheElementStorageFactory {

    static CacheElementStorage store(QueryDescription description,
            List<?> results) {
        // DATA and AGGREGATE_ITEM_WITH_DATA only return ids...
        // But ITEM_WITH_AGGREGATE (and other multiples?) seem like it is
        // rewritten into Object[] { item, ... } in AbstractQB which is bad...
        if (description.isFinderRewriteNeeded() && !description._projection.multiple()) {
            String itemType = description._itemType;
            return new FinderElementStorage(itemType, results);
        } else if (description.isItemProjection()) {
            return new ItemElementStorage(results);
        } else {
            return new DataElementStorage(results);
        }
    }
}

/**
 * Store the primary key identities of Item instances than load them when
 * pulling from the cache.
 */
class ItemElementStorage implements CacheElementStorage {
    private static final long serialVersionUID = 2004705291350224862L;

    private final List<Long> _results;

    ItemElementStorage(List<?> results) {
        List<Long> temp = new ArrayList<Long>();
        for (Object o : results) {
            assert o instanceof Item : "Cannot store anything other than Item instances.";

            Long itemId = ((Item) o).getId();
            temp.add(itemId);
        }
        _results = Collections.unmodifiableList(temp);
    }

    public List<?> resurrect(EntityManager entityManager) {
        ServiceContext serviceContext = ServiceContext.getContext();
        return serviceContext.getItemService().get(_results);
    }
}

/**
 * Strategy for storing plain Data values.
 */
class DataElementStorage implements CacheElementStorage {
    private static final long serialVersionUID = 4378296447379059944L;

    private final List<?> _results;

    public DataElementStorage(List<?> results) {
        _results = Collections.unmodifiableList(results);
    }

    public List<?> resurrect(EntityManager entityManager) {
        return new ArrayList<Object>(_results);
    }
}

/**
 * Store the primary key identities of Finder instances than load them when
 * pulling from the cache and tarely on the eager item loading.
 */
class FinderElementStorage implements CacheElementStorage {
    private static final long serialVersionUID = 1700131748799967960L;

    private final List<Long> _results;
    private final String _itemType;

    public FinderElementStorage(String itemType, List<?> results) {
        _itemType = itemType;
        List<Long> temp = new ArrayList<Long>(results.size());
        for (Object o : results) {
            Item item = (Item) o;
            temp.add(item.getId());
        }
        _results = Collections.unmodifiableList(temp);
    }

    public List<?> resurrect(EntityManager entityManager) {
        ServiceContext serviceContext = ServiceContext.getContext();
        return serviceContext.getItemService().get(_results, _itemType);
    }

}
