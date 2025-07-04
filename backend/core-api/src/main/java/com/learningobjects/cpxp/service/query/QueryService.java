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

import com.google.common.collect.Lists;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.service.item.Item;
import org.apache.commons.collections4.IterableUtils;

import javax.ejb.Local;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Local
public interface QueryService {

    /**
     * Generate an unbounded query builder.  This will query across domains unless
     * {@link QueryBuilder#setPath(String)}, {@link QueryBuilder#setParent(Item)},
     * {@link QueryBuilder#setRoot(Item)}, or similar are called.
     *
     * {@link #queryRoot(String)} and {@link #queryParent(Long, String)} are more
     * appropriate for most use cases.
     *
     * @return A new QueryBuilder.
     */
    public QueryBuilder queryAllDomains();

    /**
     * Generate an unbounded query builder.  This will query across domains unless
     * {@link QueryBuilder#setPath(String)}, {@link QueryBuilder#setParent(Item)},
     * {@link QueryBuilder#setRoot(Item)}, or similar are called.
     *
     * {@link #queryRoot(String)} and {@link #queryParent(Long, String)} are more
     * appropriate for most use cases.
     *
     * @param itemType Item Type
     * @return A new QueryBuilder for a given item type.
     */
    public QueryBuilder queryAllDomains(String itemType);

    /**
     * Generates a query builder that is restricted to the current domain.
     * @param itemType Item Type
     * @return A new QueryBuilder restricted to the current domain and a given item type.
     */
    public QueryBuilder queryRoot(String itemType);

    /**
     * Generates a query builder that is restricted to a domain.
     * @param rootId the domain
     * @param itemType Item Type
     * @return A new QueryBuilder restricted to the current domain and a given item type.
     */
    public QueryBuilder queryRoot(Long rootId, String itemType);

    /**
     * Generates a query builder that is restricted to children of a given parent.
     *
     * @param parentId ID of parent
     * @return A new QueryBuilder restricted to a specified parent.
     */
    public QueryBuilder queryParent(Long parentId);

    /**
     * Generates a query builder that is restricted to children of a given parent.
     *
     * @param parent Parent
     * @return A new QueryBuilder restricted to a specified parent.
     */
    public QueryBuilder queryParent(Id parent);

    /**
     * Generates a query builder that is restricted to children of a given parent, and
     * a given item type.
     *
     * @param parentId ID of parent
     * @param itemType Item Type
     * @return A new QueryBuilder restricted to a specified parent and item type.
     */
    public QueryBuilder queryParent(Long parentId, String itemType);

    /**
     * Generates a query builder that is restricted to children of a given parent, and
     * a given item type.
     *
     * @param parent Parent
     * @param itemType Item Type
     * @return A new QueryBuilder restricted to a specified parent and item type.
     */
    public QueryBuilder queryParent(Id parent, String itemType);

    /**
     * Generates a query builder that is restricted to children of a given collection
     * of parents, and given item type.
     *
     * @param parentIds IDs of parents
     * @param itemType Item Type
     * @return A new QueryBuilder restricted to a specified parent and item type.
     */
    public QueryBuilder queryParentIds(Iterable<Long> parentIds, String itemType);

    /**
     * Generates a query builder that is restricted to children of a given collection
     * of parents, and a given item type.
     *
     * @param parents Parents
     * @param itemType Item Type
     * @return A new QueryBuilder restricted to a specified parent and item type.
     */
    public QueryBuilder queryParents(Iterable<? extends Id> parents, String itemType);

    /**
     * Performs the given function over a range of ids for the given asset type.  This allows
     * for efficient batch operations to be performed over the entire table
     * NOTE: this signature can be updated to use a BiFunction if reduction is required
     *
     * @param itemType the Item Type to operate on
     * @param chunkSize how many records to use in each batch
     * @param consumer BiConsumer that performs some operation based on the start and end ids
     */
    public void operateOnChunks(String itemType, int chunkSize, BiConsumer<Long, Long> consumer);

    public List<Item> query(String path);
    public void evict(Long id);
    public void clearEntityManager();
    public EntityManager getEntityManager();
    public Query createQuery(String query);
    public Query createNativeQuery(String query);
    /** Load a SQL query from {@param context}'s resources */
    public Query loadNativeQuery(Object context, String name);
    public void invalidateQuery(String query);

    /**
     * Static function to chunk queries (or any operation) given a list of ids and a lambda that consumes them.
     * Usage example:
     * ```
     * List<Long> results;
     * chunkIdsForQuery(lotsOfIds, (chunkedIds) -> {
     *    Query q = _queryService.createNativeQuery(query);
     *    q.setParameter("ids", chunkedIds);
     *    results.putAll(q.getResultList());
     * });
     * ```
     * The operation will be split into smaller slices to avoid overloading the PostgreSQL with too many parameters and
     * thus getting a PSQL error such as: Tried to send an out-of-range integer as a 2-byte value
     *
     * @param ids the collection of ids to partition
     * @param fn consumer that accepts a list of ids, ostensibly to be passed into a query.
     */
    static <T> void chunkIdsForQuery(Iterable<T> ids, Consumer<List<T>> fn) {
        int DEFAULT_CHUNK_SIZE = 8192;
        List<List<T>> partitionedIds = Lists.partition(IterableUtils.toList(ids), DEFAULT_CHUNK_SIZE);
        partitionedIds.forEach(fn);
    }
}
