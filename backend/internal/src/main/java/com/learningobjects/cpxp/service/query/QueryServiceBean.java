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

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.item.ItemWebService;
import org.apache.commons.io.IOUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class QueryServiceBean extends BasicServiceBean implements QueryService {
    private static final Logger logger = Logger.getLogger(QueryServiceBean.class.getName());

    /** The item web service. */
    private final ItemWebService _itemWebService;

    @Inject
    public QueryServiceBean(ItemService itemService, QueryCache queryCache, ItemWebService itemWebService) {
        super(itemService, queryCache);
        _itemWebService = itemWebService;
    }

    @Override
    public QueryBuilder queryAllDomains() {
        return super.queryBuilder();
    }

    @Override
    public QueryBuilder queryAllDomains(String itemType) {
        return super.querySystem(itemType);
    }

    @Override
    public QueryBuilder queryRoot(String itemType) {
        return super.queryRoot(getCurrentDomain(),itemType);
    }

    @Override
    public QueryBuilder queryRoot(Long rootId, String itemType) {
        return super.queryRoot(_itemService.get(rootId), itemType);
    }

    @Override
    public QueryBuilder queryParent(Long parentId, String itemType) {
        return super.queryParent(_itemService.get(parentId), itemType);
    }

    @Override
    public QueryBuilder queryParent(Long parentId) {
        return queryParent(parentId, null);
    }

    @Override
    public QueryBuilder queryParent(Id parent) {
        return queryParent(parent.getId());
    }

    @Override
    public QueryBuilder queryParent(Id parent, String itemType) {
        return queryParent(parent.getId(), itemType);
    }

    @Override
    public QueryBuilder queryParentIds(Iterable<Long> parentIds, String itemType) {
        return queryAllDomains(itemType).addCondition(BaseCondition.inIterable(DataTypes.META_DATA_TYPE_PARENT_ID, parentIds));
    }

    @Override
    public QueryBuilder queryParents(Iterable<? extends Id> parents, String itemType) {
        return queryAllDomains(itemType).addCondition(BaseCondition.inIterable(DataTypes.META_DATA_TYPE_PARENT_ID, parents));
    }

    @Override
    public void operateOnChunks(String tableName, int chunkSize, BiConsumer<Long, Long> consumer) {
        long min = 0L;
        Number next;
        do {
            next = (Number) createNativeQuery("SELECT MAX(id) FROM (SELECT id FROM " + tableName + " WHERE id >= " + min + " ORDER BY id ASC LIMIT " + chunkSize + ") AS id").getSingleResult();
            if (next != null) {
                consumer.accept(min, next.longValue());
                min = next.longValue() + 1L;
            }
        } while(next != null);
    }

    private static final Pattern QUERY_RE = Pattern.compile("//([.\\w]+)\\[@([.\\w]+)=(.*)]");

    @Override
    public List<Item> query(String path) {

        Matcher matcher = QUERY_RE.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid query path: " + path);
        }

        QueryBuilder qb = queryRoot(getCurrentDomain(),matcher.group(1));
        qb.addCondition(matcher.group(2), "eq", matcher.group(3));

        return qb.getItems();
    }

    @Override
    public void evict(Long id) {
        _itemWebService.evictFromCache(id);
    }

    @Override
    public void clearEntityManager() {
        getEntityManager().flush();
        getEntityManager().clear();
        Current.clearCache();
    }

    @Override
    public EntityManager getEntityManager() {
        return super.getEntityManager();
    }

    @Override
    public Query createQuery(String query) {
        logger.log(Level.FINE, "Query, {0}", query);
        return getEntityManager().createQuery(query);
    }

    @Override
    public Query createNativeQuery(String query) {
        logger.log(Level.FINE, "Native query, {0}", query);
        return getEntityManager().createNativeQuery(query);
    }

    @Override
    public Query loadNativeQuery(Object context, String name) {
        try {
            Class<?> cls = context.getClass();
            InputStream is = null;

            while (is == null && !Object.class.equals(cls)) {
                is = cls.getResourceAsStream(name + ".sql");
                cls = cls.getSuperclass();
            }
            Objects.requireNonNull(is, "No query resource named "+name+" was found!");

            return createNativeQuery(IOUtils.toString(is, StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Override
    public void invalidateQuery(String query) {
        super.invalidateQuery(query);
    }
}
