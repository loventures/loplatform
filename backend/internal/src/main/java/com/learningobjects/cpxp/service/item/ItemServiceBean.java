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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.dto.BaseEntityDescriptor;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.entity.EntityUtils;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.tx.TransactionCompletion;
import org.hibernate.internal.SessionImpl;
import org.hibernate.query.NativeQuery;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Provider;
import jakarta.persistence.*;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The item service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ItemServiceBean implements ItemService {
    private static final Logger logger = Logger.getLogger(ItemServiceBean.class.getName());

    private final Ontology _ontology;
    private final Provider<EntityContext> _entityContext;

    public ItemServiceBean(
      Ontology ontology,
      Provider<EntityContext> entityContext
    ) {
        _ontology = ontology;
        _entityContext = entityContext;
    }

    /**
     * Create a new root item.
     */
    public Item create(String type) {

        EntityDescriptor descriptor = _ontology.getEntityDescriptor(type);

        Item item = newItem(null, type, descriptor);
        persist(item, descriptor);

        return item;
    }

    /**
     * Create a new child item.
     */
    public Item create(Item parent, String type) {
        return create(parent, type, null);
    }

    @Override
    public Item create(Item parent, String type, Consumer<Item> init) {

        EntityDescriptor descriptor = _ontology.getEntityDescriptor(type);

        Item item = newItem(parent, type, descriptor);
        if (init != null) {
            init.accept(item);
        }
        persist(item, descriptor);
        // If the entity itself is not cacheable, assume it does not participate in cached queries
        // and so no invalidation is needed. This is primarily in order that rapid-append tables,
        // like analytics events and such, do not cause tremendous invalidation traffic across the
        // cluster.
        if ((descriptor == null) || ((BaseEntityDescriptor) descriptor).isCacheable()) {
            Current.setPolluted(item);
        }

        return item;
    }

    /**
     * Move an item.
     *
     * *WARNING* This purges the item and all of its descendants from the L1
     * cache, so no entity references held to any of these items will be valid
     * after this call.
     *
     * @param item
     *            the item to move
     * @param parent
     *            the new parent
     */
    public void move(Item item, Item parent) {

        String oldPath = calculatePath(item);

        if (loadFinder(item)) {
            item.getFinder().setParent(parent);
        }
        item.setParent(parent);
        // addChild also sets the parent on the item argument
        String newPath = calculatePath(item);

        EntityDescriptor entityDescriptor = _ontology.getEntityDescriptor(item.getItemType());
        List<Item> affectedItems = null;
        if(entityDescriptor.getItemRelation().isPeered()) {
            affectedItems = fetchSubtreeItems(item);
            int n = updateSubtreePaths(oldPath, newPath);
            assert affectedItems.size() == n : "Affected items list should match the number actually affected by update.";
        }

        item.setPath(newPath);
        if (item.getFinder() != null) {
            item.getFinder().setPath(newPath); // meh
        }

        getEntityManager().flush();

        if(entityDescriptor.getItemRelation().isPeered()) {
            // the use of native queries definitely required eviction from the
            // L1/session cache and the L2/shared cache
            bulkEvictL1(affectedItems, true);
        }

    }

    private int updateSubtreePaths(String oldPath, String newPath) {
        // update path prefix on children
        String sqlSet = "SET path = regexp_replace(path, :old_path, :new_path) where path like :match_path";
        Query query = createNativeQuery(
                        "update item " + sqlSet);
        // regexp_replace with ^ so that moving from "1/" to "11/21/"
        // doesn't go haywire
        query.setParameter("old_path", "^".concat(oldPath));
        query.setParameter("new_path", newPath);
        query.setParameter("match_path", oldPath.concat("%"));
        int n = query.executeUpdate();

        for (EntityDescriptor descriptor : _ontology.getEntityDescriptors().values()) {
            query = createNativeQuery(
                "UPDATE " + descriptor.getTableName() + " " + sqlSet);
            query.setParameter("old_path", "^".concat(oldPath));
            query.setParameter("new_path", newPath);
            query.setParameter("match_path", oldPath.concat("%"));
            query.executeUpdate();
        }

        return n;
    }

    /**
     * Destroy an subtree of items.
     */
    public void destroy(Item item) {

        destroy(item, true);

    }

    /**
     * Destroy a subtree of items, optionally leaving the apex node intact.
     */
    public void destroy(Item item, boolean inclusive) {

        // dummy items for orphan finders don't require cleaning up any
        // references or descendants since they by definition don't participate
        // in the item hierarchy and cannot be referenced in other data/finder
        // properties
        if (removeOrphan(item)) {
            Current.setPolluted(item);
            return;
        } else if (inclusive) {
            delete(item);
            return;
        }

        clearFinder(item);

        long start = System.currentTimeMillis();
        if (inclusive) {
            clearItemRefs(item, null, ReferenceClearance.PATH_INCLUSIVE); // UNREACHABLE
        } else {
            clearItemRefs(item, null, ReferenceClearance.PATH_EXCLUSIVE);
        }
        logger.log(Level.FINE, "Took {0} MS to clear item and descendants references.", (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        destroyWithDescendants(item, !inclusive);
        logger.log(Level.FINE, "Took {0} MS to remove item and descendants.", (System
          .currentTimeMillis() - start));

    }

    public void deleteDomain(Item domain) {

        String deleted = "DomainDelete";
        int deletedEntities = 0;
        Query query = createQuery("UPDATE Item SET deleted = :deleted WHERE root = :domain AND deleted IS NULL");

        query.setParameter("domain", domain);
        query.setParameter("deleted", deleted);
        deletedEntities = query.executeUpdate();
        logger.log(Level.FINE, "Deleted {0} on Item", deletedEntities);
        //updating finders
        for (EntityDescriptor descriptor : _ontology.getEntityDescriptors().values()) {
            if(ItemRelation.DOMAIN != descriptor.getItemRelation()) {
                query = createQuery(
                  "UPDATE " + descriptor.getEntityName() + " SET del = :deleted WHERE root = :domain AND del IS NULL");
                query.setParameter("domain", domain);
                query.setParameter("deleted", deleted);
                int finderDeleted = query.executeUpdate();
                logger.log(Level.FINE, "Deleted {0} on {1}", new Object[]{finderDeleted, descriptor.getEntityName()});
                deletedEntities += finderDeleted;
            }
        }
        logger.log(Level.FINE, "Deleted total {0} for domain: {1}", new Object[]{deletedEntities, domain.getId()});

    }

    public void delete(Item item) {
        setDeleted(item, Current.deleteGuid());
    }

    @Override
    public void delete(Finder finder) {
        setDeleted(finder.getOwner(), Current.deleteGuid());
    }

    public void setDeleted(Item item, String deleted) {

        if (deleted == null) {
            throw new RuntimeException("Null deleted");
        } else if (item.getDeleted() != null) {
            throw new RuntimeException("Already deleted: " + item);
        }

        Current.setPolluted(item);

        item.setDeleted(deleted);
        if (item.getFinder() != null) {
            item.getFinder().setDel(deleted);
        }

        if (isOrphan(item)) {
            // the actual entity columns are read-only so an explicit update is needed for persistence
            final EntityDescriptor ed = _ontology.getEntityDescriptor(item.getType());
            // this is not actually a read-only query, but the appropriate invalidation will be triggered
            // by the entity updates.
            final Query query = createNativeQueryWithNoCacheInvalidation(
              "UPDATE " + ed.getTableName() + " SET del = :deleted WHERE id = :item_id");
            query.setParameter("item_id", item.getId());
            query.setParameter("deleted", deleted);
            query.executeUpdate();
            logger.log(Level.FINE, "Deleted orphan Item ID: {0}", item.getId());
            return;
        }

        // flush all entities to the database before doing bulk updates
        getEntityManager().flush();

        int deletedEntities = 0;
        Query query = createQuery("UPDATE Item SET deleted = :deleted WHERE path LIKE :item_path AND deleted IS NULL");

        query.setParameter("item_path", item.getPath().concat("%"));
        query.setParameter("deleted", deleted);
        deletedEntities = query.executeUpdate();

        logger.log(Level.FINE, "Deleted {0} on Item", deletedEntities);

        if (deletedEntities == 0) {
            // if item  had no item children, we only have to look at standalone tables
            for (EntityDescriptor ed : _ontology.getEntityDescriptors().values()) {
                if (ed.getItemRelation() == ItemRelation.LEAF) {
                    query = createQuery(
                      "UPDATE " + ed.getEntityName() + " SET del = :deleted WHERE parent_id = :item_id AND del IS NULL");
                    query.setParameter("item_id", item.getId());
                    query.setParameter("deleted", deleted);
                    int finderDeleted = query.executeUpdate();
                    logger.log(Level.FINE, "Deleted {0} on {1}", new Object[]{finderDeleted, ed.getEntityName()});
                    deletedEntities += finderDeleted;
                }
            }
        } else {
            // otherwise update all finders
            for (EntityDescriptor ed : _ontology.getEntityDescriptors().values()) {
                if (ed.getItemRelation() != ItemRelation.DOMAIN) {
                    query = createQuery(
                      "UPDATE " + ed.getEntityName() + " SET del = :deleted WHERE path LIKE :item_path AND del IS NULL");
                    query.setParameter("item_path", item.getPath().concat("%"));
                    query.setParameter("deleted", deleted);
                    int finderDeleted = query.executeUpdate();
                    logger.log(Level.FINE, "Deleted {0} on {1}", new Object[]{finderDeleted, ed.getEntityName()});
                    deletedEntities += finderDeleted;
                }
            }
        }
        logger.log(Level.FINE, "Deleted total {0} for Item ID: {1}", new Object[]{deletedEntities, item.getId()});

    }

    public void clearDeleted(String deleted) {

        int deletedEntities = 0;
        Query query = createQuery("UPDATE Item SET deleted = null WHERE deleted = :deleted");
        query.setParameter("deleted", deleted);
        deletedEntities = query.executeUpdate();
        logger.log(Level.FINE, "Undeleted {0} on Item", deletedEntities);
        //updating finders
        for (EntityDescriptor descriptor : _ontology.getEntityDescriptors().values()) {
            if (descriptor.getItemRelation() != ItemRelation.DOMAIN) {
                query = createQuery(
                  "UPDATE " + descriptor.getEntityName() + " SET del = NULL WHERE del = :deleted");
                query.setParameter("deleted", deleted);
                int finderDeleted = query.executeUpdate();
                logger.log(Level.FINE, "Deleted {0} on {1}", new Object[]{finderDeleted, descriptor.getEntityName()});
                deletedEntities += finderDeleted;
            }
        }
        logger.log(Level.FINE, "Undeleted total {0} for trash Id: {1}", new Object[]{deletedEntities, deleted});

    }

    private boolean removeOrphan(Item item) {
        if (!isOrphan(item)) {
            return false;
        }

        if (item.getFinder() != null) {
            getEntityManager().remove(item.getFinder());
        }

        return true;
    }

    private boolean isOrphan(Item item) {

        EntityDescriptor entityDescriptor = _ontology.getEntityDescriptor(item.getType());

        return null != entityDescriptor && !entityDescriptor.getItemRelation().isPeered();
    }

    public long countDescendants(Item item) {

        long count = 0;
        Query query = createQuery("SELECT COUNT(*) FROM Item i WHERE i.path LIKE :item_path");
        query.setParameter("item_path", item.getPath().concat("%"));
        count += NumberUtils.longValue((Number) query.getSingleResult());

        for (Entry<String, EntityDescriptor> entry : _ontology
                .getEntityDescriptors().entrySet()) {
            if (entry.getValue().getItemRelation() != ItemRelation.LEAF) {
                continue;
            }
            query = createQuery(String
                    .format(
                            "SELECT COUNT(*) FROM %1$s f WHERE f.path LIKE :item_path",
                            entry.getValue().getEntityName()));
            query.setParameter("item_path", item.getPath().concat("%"));
            count += NumberUtils.longValue((Number) query.getSingleResult());
        }

        return count;
    }

    private void clearFinder(Item item) {
        EntityDescriptor entityDescriptor =
          _ontology.getEntityDescriptor(item.getType());
        if (entityDescriptor != null) { // == DataMappingSupport
            for (String dataType : entityDescriptor.getDataTypes()) {
                entityDescriptor.set(item, dataType, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Multimap<Long, String> findLeafParents(Item parent) {
        Multimap<Long, String> leafParents = ArrayListMultimap.create();
        for (Entry<String, EntityDescriptor> entry : _ontology
                .getEntityDescriptors().entrySet()) {
            if (entry.getValue().getItemRelation() != ItemRelation.LEAF) {
                continue;
            }
            Query query = createQuery(String
                    .format(
                            "SELECT DISTINCT f.parent.id FROM %1$s f WHERE f.path LIKE :item_path",
                            entry.getValue().getEntityName()));
            query.setParameter("item_path", parent.getPath().concat("%"));
            for (Long id : (List<Long>) query.getResultList()) {
                leafParents.put(id, entry.getKey());
            }
        }

        return leafParents;
    }

    @Override
    public boolean pessimisticLock(Item item) {
        return lock(item, true, false, Optional.empty());
    }

    /**
     * Refresh an entity from the database and lock its backing rows for
     * the duration of the transaction.
     *
     * @param item    The item to refresh
     * @param timeout A duration in milliseconds to wait, or None to wait forever.
     * @return whether the lock was successfully obtained
     */
    @Override
    public boolean lock(Item item, boolean lock, boolean refresh, Optional<Long> timeout) {
        final LockModeType mode = lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE;
        final Map<String, Object> props = // set timeout if locking and specified
          timeout.filter(to -> lock).map(to -> Collections.<String, Object>singletonMap("jakarta.persistence.lock.timeout", to)).orElse(Collections.emptyMap());
        try {
            if (refresh) {
                // if it has a finder
                if (loadFinder(item)) {
                    // if the finder is in the entity manager (not new or detached)
                    if (getEntityManager().contains(item.getFinder()) && !item.getFinder().isNew()) {
                        // refresh/lock the finder
                        getEntityManager().refresh(item.getFinder(), mode, props);
                        // and if there is an item, refresh it
                        if (!_ontology.isStandalone(item.getType())) {
                            getEntityManager().refresh(item);
                        }
                    }
                } else {
                    // else if the item is in the entity manager, refresh/lock it
                    if (getEntityManager().contains(item) && !item.isNew()) {
                        getEntityManager().refresh(item, mode, props);
                    }
                }
            } else {
                getEntityManager().flush();
                if (loadFinder(item)) {
                    if (getEntityManager().contains(item.getFinder()) && !item.getFinder().isNew()) {
                        getEntityManager().lock(item.getFinder(), mode, props);
                    }
                } else {
                    if (getEntityManager().contains(item) && !item.isNew()) {
                        getEntityManager().lock(item, mode, props);
                    }
                }
            }
        } catch (LockTimeoutException ex) {
            return false;
        }

        Set<Item> locks = Current.get(LOCKED_ITEMS);
        if (locks == null) {
            Current.put(LOCKED_ITEMS, new HashSet<>(Arrays.asList(item)));
            EntityContext.onCompletion(new TransactionCompletion() {
                @Override
                protected void reset() {
                    Current.remove(LOCKED_ITEMS);
                }
            });
        } else {
            locks.add(item);
        }

        return true;
    }

    private static final Object LOCKED_ITEMS = new Object();

    @Override
    public boolean isLocked(Item item) {
        Set<Item> locks = Current.get(LOCKED_ITEMS);
        return locks != null && locks.contains(item);
    }

    /**
     * Clear all data references to an item.
     *
     * @param item
     *            the item to clear
     *
     * @return the number of references
     */
    public int clearItemRefs(Item item) {

        return clearItemRefs(item, null, ReferenceClearance.ITEM);
    }

    /**
     * Replace all data references to an item.
     *
     * @param item
     *            the item to change from
     * @param value
     *            the item to change to
     *
     * @return the number of references
     */
    public int replaceItemRefs(Item item, Item value) {

        return clearItemRefs(item, value, ReferenceClearance.ITEM);
    }

    /**
     * Get an item.
     *
     * @param id
     *            The id.
     *
     * @return The item.
     *
     * @throws EntityNotFoundException
     *             If the item is not found.
     */
    public Item get(Long id) {

        Item item = findImpl(id);
        if (item != null) {
            // assert item.getPath() != null : "Item should have a materialized path.";
            if (item.getDeleted() != null) {
                logger.log(Level.FINE, "Item deleted, {0}", id);
                item = null;
            } else {
                loadFinder(item);
            }
        } else if (id != null) {
            logger.log(Level.FINE, "Item not found, {0}", id);
        }

        return item;
    }

    public <T extends Finder> T get(Long id, Class<T> clas) {
        return Optional.ofNullable(get(id, EntityUtils.getItemType(clas)))
          .map(item -> clas.cast(item.getFinder()))
          .filter(finder -> finder.getDel() == null)
          .orElse(null);
    }

    private Item getImpl(Long id) {
        Item item = findImpl(id);
        if ((item == null) || (item.getDeleted() != null)) {
            return null;
        }
        loadFinder(item);
        return item;
    }

    private Item findImpl(Long id) {
        if (id == null) {
            return null;
        } else {
            return getEntityManager().find(Item.class, id);
        }
    }

    @Override
    public List<Item> get(Iterable<Long> ids) {
        Map<Long, Item> map = map(ids);
        List<Item> items = new ArrayList<>();
        for (Long id : ids) {
            Item item = map.get(id);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public Map<Long, Item> map(Iterable<Long> idsIn) {
        Set<Long> ids = StreamSupport.stream(idsIn.spliterator(), false).filter(Objects::nonNull).collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Collections.emptyMap();
        } else if (ids.size() == 1) {
            Item item = getImpl(ids.iterator().next());
            if (item == null) {
                return Collections.emptyMap();
            } else {
                return Collections.singletonMap(item.getId(), item);
            }
        } else {
            Map<Long, Item> items = new HashMap<>();
            Multimap<String, Long> itemsByType = HashMultimap.create();

            // Load any entities that exist in the L1 or L2 cache directly from Hibernate
            SessionImpl session = getEntityManager().unwrap(SessionImpl.class);

            List<Item> loadResult =
              HibernateSessionOps$.MODULE$.bulkLoadFromCachesJava(session, ids, Item.class);

            for (Item item : loadResult) {
                if (item != null && item.getDeleted() == null) {
                    items.put(item.getId(), item);
                    itemsByType.put(item.getType(), item.getId());
                }
            }

            /* Optimize for the case where most items have the same item type.
             * Group items by type and load finders in bulk.
             * In the worst case we have a db hit for each item, which is what we had before.
             * The main reason we get here is from having built a native query from which
             * we want items; in that case it's overwhelmingly likely we got all of the
             * PKs from inspecting the same table, and this will hit the table only once,
             * for Hibernate's sake. */
            for (String itemType : itemsByType.keySet()) {
                EntityDescriptor table = _ontology.getEntityDescriptor(itemType);
                if (table == null) {
                    continue;
                }
                //noinspection unchecked
                Class<? extends Finder> clasz =
                  (Class<? extends Finder>) table.getEntityType();

                Set<Long> finderIds = new HashSet<>(itemsByType.get(itemType));

                List<? extends Finder> finderLoadResult =
                  HibernateSessionOps$.MODULE$.bulkLoadFromCachesJava(session, finderIds, clasz);

                for (Finder finder : finderLoadResult) {
                    if (finder != null && finder.getDel() == null) {
                        Item item = items.get(finder.getId());
                        // TODO: Establish whether this causes a needless entity update
                        finder.setOwner(item);
                        item.setFinder(finder);
                    }
                }
            }

            return items;
        }
    }

    @Override
    public Item get(Long id, String itemType) {
        return map(Collections.singleton(id), itemType).get(id);
    }

    @Override
    public List<Item> get(Iterable<Long> ids, String itemType) {
        Map<Long, Item> map = map(ids, itemType);
        List<Item> items = new ArrayList<>();
        for (Long id : ids) {
            Item item = map.get(id);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public Map<Long, Item> map(Iterable<Long> idsIn, String itemType) {
        // This code goes to some lengths to only hit the database for entities that are not in the L1 and L2
        // caches. Cached entries are looked up by PK because that should involve no database access. The
        // principal flaw with this is that cache.containsEntity lies and will return true for expired entries.
        // This can lead us to degenerate to 1-by-1 loading entities from the database. Perhaps if we cracked
        // open ehcache we could directly issue get requests as a more accurate test for cached availability
        // of entities.

        Set<Long> ids = StreamSupport.stream(idsIn.spliterator(), false).filter(Objects::nonNull).collect(Collectors.toSet());
        EntityDescriptor entityDescriptor = _ontology.getEntityDescriptor(itemType);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        } else if (null == entityDescriptor) {
            return map(ids);
        } else {
            Map<Long, Item> items = new HashMap<>();
            Map<Long, Finder> finders = new HashMap<>();

            // Load any entities that exist in the L1 or L2 cache directly from Hibernate
            SessionImpl session = (SessionImpl) getEntityManager().getDelegate();

            Class<? extends Finder> clasz = ((Class<? extends Finder>) entityDescriptor.getEntityType());
            List<? extends Finder> loadResult =
              HibernateSessionOps$.MODULE$.bulkLoadFromCachesJava(session, ids, clasz);

            for (Finder f : loadResult) {
                if (f != null && f.getDel() == null) {
                    finders.put(f.getId(), f);
                }
            }

            if (entityDescriptor.getItemRelation().isPeered()) {
                List<Item> itemLoadResult =
                  HibernateSessionOps$.MODULE$.bulkLoadFromCachesJava(session, ids, Item.class);
                for (Item i : itemLoadResult) {
                    if (i != null && i.getDeleted() == null) {
                        Finder f = finders.get(i.getId());
                        if (f != null) {
                            i.setFinder(f);
                            f.setOwner(i);
                            items.put(i.getId(), i);
                        }
                    }
                }
            } else {
                for (Finder f : loadResult) {
                    if (f != null && f.getDel() == null) {
                        items.put(f.getId(), f.getOwner());
                    }
                }
            }

            return items;
        }
    }

    @Override
    public void preloadItemsInPath(String path, String itemType) {
        // If the data are already loaded into cache, preload is actually
        // counterproductive...
        EntityDescriptor entityDescriptor = _ontology.getEntityDescriptor(itemType);
        if ((null != entityDescriptor) && (entityDescriptor.getItemRelation() != ItemRelation.DOMAIN)) {
            String table = entityDescriptor.getEntityName();
            Query query;
            if (entityDescriptor.getItemRelation() == ItemRelation.LEAF) {
                query = createQuery("SELECT f FROM " + table  + " f WHERE f.path LIKE :path AND f.del IS NULL");
            } else {
                query = createQuery("SELECT f FROM " + table  + " f INNER JOIN FETCH f.owner i WHERE f.path LIKE :path AND f.del IS NULL");
            }
            query.setParameter("path", path + "%");
            List<Finder> finders = (List<Finder>) query.getResultList();
            logger.log(Level.FINE, "Preload items in path, {0}, {1}", new Object[]{itemType, finders.size()});
        }
    }

    @Deprecated //just make a QueryBuilder for your aggregate query and call qb.preloadPartitionedByParent(parentIds)
    @Override
    public Multimap<Long, Item> preloadChildren(Iterable<Long> parentIds, String itemType) {
        return preloadChildMap(parentIds, itemType, "", "");
    }

    @Deprecated //just make a QueryBuilder for your aggregate query and call qb.preloadPartitionedByParent(parentIds)
    @Override
    public Multimap<Long, Item> preloadSingleChildren(Iterable<Long> parentIds, String itemType) {
        return preloadChildMap(parentIds, itemType, "", "1/");
    }

    @Deprecated //just make a QueryBuilder for your aggregate query and call qb.preloadPartitionedByParent(parentIds)
    @Override
    public Multimap<Long, Item> preloadOrderedChildren(Iterable<Long> parentIds, String itemType, String dataType) {
        return preloadChildMap(parentIds, itemType, " ORDER BY " + dataType + " ASC", dataType + " ASC/");
    }

    @Override
    public void preloadData(Iterable<Item> items) {
        QueryService.chunkIdsForQuery(items, chunk -> {
            Query dquery = createQuery("SELECT d FROM Data d WHERE d.owner IN :items");
            dquery.setParameter("items", chunk);
            List<Data> datas = (List<Data>) dquery.getResultList();
            Multimap<Long, Data> dmap = ArrayListMultimap.create();
            for (Data data : datas) {
                dmap.put(data.getOwner().getId(), data);
            }
            for (Item item : chunk) {
                item.cacheData(dmap.get(item.getId()));
            }
        });
    }

    @Deprecated //just make a QueryBuilder for your aggregate query and call qb.preloadPartitionedByParent(parentIds)
    private Multimap<Long, Item> preloadChildMap(Iterable<Long> parentIdIterator, String itemType, String orderSql, String infixKey) {
        Set<Long> parentIds = StreamSupport.stream(parentIdIterator.spliterator(), false).collect(Collectors.toSet());
        EntityDescriptor entityDescriptor = _ontology.getEntityDescriptor(itemType);
        if (parentIds.isEmpty()) {
            return ArrayListMultimap.<Long, Item>create();
        }
        Multimap<Long, Item> map = ArrayListMultimap.create();
        if (null == entityDescriptor) {
            if (StringUtils.isNotEmpty(orderSql)) {
                throw new IllegalArgumentException("Invalid order on non-finder preload: " + itemType + " / " + orderSql);
            }
            List<Item> items = new ArrayList<>();
            QueryService.chunkIdsForQuery(parentIds, (chunkedIds) -> {
                Query query = createQuery("SELECT i FROM Item i WHERE i.parent.id IN :ids AND i.deleted IS NULL");
                query.setParameter("ids", chunkedIds);
                items.addAll((List<Item>) query.getResultList());
            });
            for (Item item : items) {
                map.put(item.getParent().getId(), item);
            }
            preloadData(items);
        } else if (entityDescriptor.getItemRelation() != ItemRelation.DOMAIN) {
            List<Finder> finders = new ArrayList<>();
            QueryService.chunkIdsForQuery(parentIds, (chunkedIds) -> {
                Query query;
                String table = entityDescriptor.getEntityName();
                if (entityDescriptor.getItemRelation() == ItemRelation.LEAF) {
                    query = createQuery("SELECT f FROM " + table + " f WHERE f.parent.id IN :ids AND f.del IS NULL" + orderSql);
                } else {
                    query = createQuery("SELECT f FROM " + table + " f INNER JOIN FETCH f.owner i WHERE f.parent.id IN :ids AND f.del IS NULL" + orderSql);
                }
                query.setParameter("ids", chunkedIds);
                finders.addAll((List<Finder>) query.getResultList());
            });
            for (Finder finder : finders) {
                map.put(finder.getParent().getId(), finder.getOwner());
            }
        }
        for (Long id : parentIds) {
            String key = id + "/" + itemType + "/" + infixKey + "ITEM"; //TODO Map<parentid, string> QueryCacheKey.partitionByParent
            Current.put(key, map.get(id)); //http transaction level cache of partitioned query to item for QB
            //TODO TECH-70 encapsulate usage of Current in QueryCurrentCache
        }
        logger.log(Level.WARNING, "Preload children of items, {0}, {1}, {2}", new Object[]{itemType, parentIds.size(), map.size()});
        return map;
    }

    private Item newItem(Item parent, String type, EntityDescriptor descriptor) {
        Item item = new Item();
        final Long id = EntityContext.generateId();
        final String path = ((parent == null) ? "" : parent.getPath()) + id + "/";
        item.setId(id);
        item.setType(type);
        item.setPath(path);
        item.setNew(true);
        if (parent != null) {
            item.setRoot(parent.getRoot());
            item.setParent(parent);
        }
        if (null != descriptor) {
            Finder finder = descriptor.newInstance();
            // TODO: HACK: Kill nonnullability of DomainFinder's parent and make the root setting a core item service thing
            finder.setId(id);
            finder.setRoot((parent != null) ? parent.getRoot() : item);
            finder.setParent((parent != null) ? parent : item);
            finder.setPath(path);
            finder.setOwner(item);
            finder.setNew(true);
            item.setFinder(finder);
        }
        return item;
    }

    private void bulkEvictL1(List<Item> items, boolean evictL2) {
        List<Long> itemIds = new LinkedList<Long>();
        long start = System.currentTimeMillis();
        // N.B. this is Hibernate specific but should be reasonably safe
        SessionImpl session = (SessionImpl) getEntityManager()
                .getDelegate();
        for (Item item : items) {
            if (evictL2) {
                itemIds.add(item.getId());
            }
            // according to Hibernate JavaDocs, this call just evicts the
            // identified entry from the L1 cache
            session.evict(getEntityManager().find(Item.class, item.getId()));
        }
        logger.log(Level.FINE, "Took {0} MS to evict {1} items from L1 cache.", new Object[]{(System.currentTimeMillis() - start), items.size()});

        if (itemIds.isEmpty()) {
            return;
        }

        bulkEvictL2(Item.class.getName(), itemIds);
    }

    private void bulkNativeEvictL2(String cacheName, List<Number> nativeIds) {
        if (null == nativeIds || nativeIds.isEmpty()) {
            return;
        }
        List<Long> toEvict = new LinkedList<Long>();
        for (Number result : nativeIds) {
            toEvict.add(result.longValue());
        }

        bulkEvictL2(cacheName, toEvict);
    }

    private void bulkEvictL2(String cacheName, List<Long> entityIds) {
        BulkCacheDispatcher.enqueueItemEviction(getEntityManager(), cacheName,
          entityIds);
    }

    private int clearItemRefs(Item item, Item value,
            ReferenceClearance clearance) {

        EntityDescriptor descriptor = _ontology.getEntityDescriptor(item.getType());

        // the item is bogus, it doesn't participate in the item hierarchy, so
        // there will be no data elements or references from other item finders
        if ((descriptor != null) && !descriptor.getItemRelation().isPeered()) {
            return 0;
        }

        int n = executeClearanceQuery(item, value, clearance.getStatement(
          value, "Data", "item"), clearance, Data.class.getName(), false);

        logger.log(Level.FINE, "Cleared {0} data elements.", n);

        for (EntityDescriptor entityDescriptor : _ontology
                .getEntityDescriptors().values()) {
            for (String dataType : entityDescriptor.getDataTypes()) {
                DataTypedef typedef = _ontology.getDataType(dataType);
                if (typedef.value() != DataFormat.item) {
                    continue;
                }
                PropertyDescriptor pd = entityDescriptor.getPropertyDescriptors().get(dataType);
                boolean isFinderRef = Finder.class.isAssignableFrom(pd.getPropertyType());
                if (isFinderRef && !item.getType().equals(typedef.itemType())) {
                    continue;
                }

                String propertyName = entityDescriptor
                        .getPropertyName(dataType);

                String ejbQl = clearance.getStatement(value, entityDescriptor
                        .getEntityName(), propertyName);
                int m = executeClearanceQuery(item, value, ejbQl, clearance,
                        entityDescriptor.getEntityName(), isFinderRef);
                logger.log(Level.FINE, "Cleared {0} finder references.", m);
                n += m;
            }
        }

        return n;
    }

    @SuppressWarnings("unchecked")
    private int executeClearanceQuery(Item item, Item value, String statement,
            ReferenceClearance clearance, String cacheName, boolean isFinderRef) {
        if (ReferenceClearance.ITEM == clearance) {
            Query query = createQuery(statement);
            clearance.setParameters(query, item, value, isFinderRef);
            return query.executeUpdate();
        }

        // Hibernate doesn't coordinate the cache changes for native queries so
        // this needs to execute a bit differently to capture the affected IDs
        // and flush them from the cache
        Query query = createNativeQuery(statement);
        clearance.setParameters(query, item, value, isFinderRef);
        List<Number> resultList = query.getResultList();

        bulkNativeEvictL2(cacheName, resultList);

        return (null == resultList) ? 0 : resultList.size();
    }

    /**
     * This relies on up-to-date materialized paths in the database so any code
     * that affects parent or child relations needs to re-calculate paths for
     * the affected sub-tree.
     *
     * @see Item#setParent(Item)
     * @see #move(Item, Item)
     */
    @SuppressWarnings("unchecked")
    private int destroyWithDescendants(Item item, boolean exclusive) {

        // fetch ids for items that will be removed to manually evict from L2
        // cache
        List<Long> affected = fetchSubtreeIds(item, exclusive);

        Query query = null;
        int totalAffected = 0;

        // flush changes to the database
        getEntityManager().flush();
        // erase finder references to avoid FK constraint violations
        for (EntityDescriptor descriptor : _ontology.getEntityDescriptors()
                .values()) {
            String tableName = descriptor.getTableName();
            for (PropertyDescriptor pd : descriptor.getPropertyDescriptors().values()) {
                if (!Finder.class.isAssignableFrom(pd.getPropertyType())) {
                    continue;
                }
                String propertyName = pd.getName().concat("_id");
                String targetTable = pd.getPropertyType().getSimpleName();
                if (exclusive) {
                    query = createNativeQuery(
                        "UPDATE " + tableName + " SET " + propertyName + " = NULL" +
                        " WHERE " + propertyName + " IN (SELECT id FROM " + targetTable +
                        " WHERE path LIKE :item_path AND id <> :item_id)");
                    query.setParameter("item_id", item.getId());
                } else {
                    query = createNativeQuery(
                        "UPDATE " + tableName + " SET " + propertyName + " = NULL" +
                        " WHERE " + propertyName + " IN (SELECT id FROM " + targetTable +
                        " WHERE path LIKE :item_path)");
                }
                query.setParameter("item_path", item.getPath().concat("%"));
                int count = query.executeUpdate();
                logger.log(Level.FINE, "Cleared finder references, {0}, {1}, {2}, {3}", new Object[]{tableName, propertyName, targetTable, count});
            }
        }
        // delete finders of descendants first to clear the parent FK out
        for (EntityDescriptor descriptor : _ontology.getEntityDescriptors()
                .values()) {
            // Orphans don't have an owner but do have a parent.
            if (descriptor.getItemRelation() == ItemRelation.DOMAIN) {
                continue;
            } else if (descriptor.getItemRelation() == ItemRelation.LEAF) {
                query = createNativeQuery(
                    String.format(
                        "DELETE FROM %1$s f USING Item i WHERE f.parent_id = i.id AND i.path LIKE :item_path RETURNING f.id",
                        descriptor.getTableName()));
            } else if (exclusive) {
                query = createNativeQuery(
                                String
                                        .format(
                                                "DELETE FROM %1$s f USING Item i WHERE f.id = i.id and i.id <> :item_id AND i.path LIKE :item_path RETURNING f.id",
                                                descriptor.getTableName()));
                query.setParameter("item_id", item.getId());
            } else {
                query = createNativeQuery(
                                String
                                        .format(
                                                "DELETE FROM %1$s f USING Item i WHERE f.id = i.id AND i.path LIKE :item_path RETURNING f.id",
                                                descriptor.getTableName()));
            }
            query.setParameter("item_path", item.getPath().concat("%"));
            List<Number> results = query.getResultList();
            bulkNativeEvictL2(descriptor.getEntityName(), results);
            totalAffected += (null == results) ? 0 : results.size();
        }
        logger.log(Level.FINE, "Removed {0} finder instances.", totalAffected);

        // delete data elements before items
        if (exclusive) {
            query = createNativeQuery(
                            "DELETE FROM Data d USING Item i WHERE d.owner_id = i.id and i.id != :item_id AND i.path LIKE :item_path RETURNING d.id");
            query.setParameter("item_id", item.getId());
        } else {
            query = createNativeQuery(
                            "DELETE FROM Data d USING Item i WHERE d.owner_id = i.id and i.path LIKE :item_path RETURNING d.id");
        }
        query.setParameter("item_path", item.getPath().concat("%"));
        List<Number> results = query.getResultList();
        bulkNativeEvictL2(Data.class.getName(), results);
        int n = (null == results) ? 0 : results.size();
        logger.log(Level.FINE, "Removed {0} data instances.", totalAffected);
        totalAffected += n;

        // delete out the disconnected, un-referenced items
        if (exclusive) {
            query = createQuery("DELETE Item i WHERE i.id <> :item_id AND i.path LIKE :item_path");
            query.setParameter("item_id", item.getId());
        } else {
            query = createQuery("DELETE Item i WHERE i.path LIKE :item_path");
        }
        query.setParameter("item_path", item.getPath().concat("%"));
        n = query.executeUpdate();
        logger.log(Level.FINE, "Removed {0} item instances.", totalAffected);
        assert affected.size() == n : "Affected ids list should match the number actually affected by delete.";
        totalAffected += n;

        // Hibernate will evict bulk deletes from the session cache, not sure
        // about L2 cache so this call ensures L2 entries are evicted
        bulkEvictL2(Item.class.getName(), affected);

        return n;
    }

    @SuppressWarnings("unchecked")
    private List<Item> fetchSubtreeItems(Item item) {
        Query query = createQuery("SELECT i FROM Item i WHERE i.path LIKE :item_path");
        query.setParameter("item_path", item.getPath().concat("%"));
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> fetchSubtreeIds(Item item, boolean exclusive) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT i.id FROM Item i WHERE i.path LIKE :item_path");
        if (exclusive) {
            sb.append(" AND i.id <> :item_id");
        }
        Query query = createQuery(sb.toString());
        query.setParameter("item_path", item.getPath().concat("%"));
        if (exclusive) {
            query.setParameter("item_id", item.getId());
        }
        return query.getResultList();
    }

    private void persist(Item item, EntityDescriptor descriptor) {
        ItemRelation itemRelation = (null == descriptor) ? ItemRelation.PEER
                : descriptor.getItemRelation();
        if (itemRelation.isPeered()) {
            getEntityManager().persist(item);
        }
        if (null != descriptor && null != item.getFinder()) {
            getEntityManager().persist(item.getFinder());
        }
    }

    public boolean loadFinder(Item item) {
        if (null == item) {
            return false;
        } else if (item.getFinder() != null) {
            return true;
        }

        EntityDescriptor descriptor = _ontology.getEntityDescriptor(item.getType());
        if (null == descriptor) {
            return false;
        }

        assert item.getId() > 0L : "Cannot load a finder for a dummy item.";

        final Finder finder = (Finder) getEntityManager().find(
            descriptor.getEntityType(), item.getId());
        assert finder != null : "Null finder for item: " + item;
        assert finder.getPath() != null : "Finder should have a materialized path.";
        assert ObjectUtils.equals(item.getDeleted(), finder.getDel()) : "Item/finder deletion should match: " + item.getDeleted() + " / " + finder.getDel();

        finder.setOwner(item);
        item.setFinder(finder);
        return true;
    }

    private String calculatePath(Item item) {
        // special case, this item is a root
        if (item.getParent() == null) {
            return Long.toString(item.getId()).concat("/");
        }
        // try to re-use the path of the immediate parent as an efficiency,
        // should cut down the overhead of either traversal or computation both
        // of which have a perceptible impact on item creation during bulk
        // operations
        String parentPath = item.getParent().getPath();
        if (null == parentPath) {
            parentPath = calculateParentPath(item.getParent());
        }
        return parentPath.concat(Long.toString(item.getId())).concat("/");
    }

    // based on empirical testing, mostly with a 250 item batch, this traversal
    // code adds the least cost over using the connectby function from
    // PostgreSQL's tablefunc contributed module
    private String calculateParentPath(Item parent) {
        StringBuilder pathBuffer = new StringBuilder(Long.toString(parent
                .getId()));
        pathBuffer.append('/');
        Item ancestor = parent.getParent();
        while (ancestor != null) {
            if (ancestor.getPath() != null) {
                // if any ancestor has a path, use that as a short circuit
                pathBuffer.insert(0, ancestor.getPath());
                break;
            }

            pathBuffer.insert(0, '/');
            pathBuffer.insert(0, ancestor.getId());
            ancestor = ancestor.getParent();
        }
        return pathBuffer.toString();
    }

    /**
     * What started as a hack has evolved into a reasonable way of collecting
     * implementation strategies for clearing FK references efficiently for a
     * couple of different uses.
     */
    private static enum ReferenceClearance {
        /**
         * For dealing with a single item, a simple EJBQL statement, the JPA
         * provider can handle cache coordination.
         */
        ITEM("UPDATE %1$s e SET e.%2$s = :value WHERE e.%2$s = :item", false) {
            void setParameters(Query query, Item item, Item value, boolean isFinderRef) {
                query.setParameter("item", isFinderRef ? item.getFinder() : item);
                query.setParameter("value", (isFinderRef && (value != null)) ? value.getFinder() : value);
            }
        },

        /**
         * For clearing references for a sub-tree, uses native queries for
         * speed, hence needs to manual manage cache coordination as the JPA
         * provider won't.
         */
        PATH_INCLUSIVE(
                "UPDATE %1$s SET %2$s_id = :value FROM item i WHERE %1$s.%2$s_id = i.id AND i.path LIKE :item_path RETURNING %1$s.id",
                true) {
            void setParameters(Query query, Item item, Item value, boolean isFinderRef) {
                query.setParameter("item_path", item.getPath().concat("%"));
                // the :value parameter will be absent if the value is null, see
                // the note in the constructor
                if (value != null) {
                    query.setParameter("value", value.getId());
                }
            }
        },

        /**
         * For clearing references for a sub-tree, uses native queries for
         * speed, hence needs to manual manage cache coordination as the JPA
         * provider won't.
         */
        PATH_EXCLUSIVE(
                "UPDATE %1$s SET %2$s_id = :value FROM Item i WHERE %1$s.%2$s_id = i.id AND %1$s.%2$s_id != :item_id AND i.path LIKE :item_path RETURNING %1$s.id",
                true) {

            void setParameters(Query query, Item item, Item value, boolean isFinderRef) {
                query.setParameter("item_path", item.getPath().concat("%"));
                query.setParameter("item_id", item.getId());
                // the :value parameter will be absent if the value is null, see
                // the note in the constructor
                if (value != null) {
                    query.setParameter("value", value.getId());
                }
            }
        };

        private final String _statement;

        private final boolean _nativeStatement;

        private final String _nullStatement;

        private ReferenceClearance(String statement, boolean nativeStatement) {
            _statement = statement;
            _nativeStatement = nativeStatement;
            // there is a problem with postgres' setNull on the underlying
            // prepared statement that shows up as a complaint about being
            // unable to set a value of type bytea on a column of type bigint;
            // Hibernate's EJB-QL handling code seems to work around this but
            // not its native query handling
            _nullStatement = statement.replaceFirst("= :value", "= null");
        }

        String getStatement(Item value, String entityName, String propertyName) {
            // only use the native null hack if the statement actually is a
            // native statement
            String statement = _nativeStatement ? ((null == value) ? _nullStatement
                    : _statement)
                    : _statement;
            if (_nativeStatement) {
                int lastDot = entityName.lastIndexOf('.');
                entityName = entityName.substring(lastDot + 1);
            }
            return String.format(statement, entityName, propertyName);
        }

        abstract void setParameters(Query query, Item item, Item value, boolean isFinderRef);
    };

    private EntityManager getEntityManager() {
        return _entityContext.get().getEntityManager();
    }

    private Query createQuery(String sql) {
        return getEntityManager().createQuery(sql);
    }

    private Query createNativeQuery(String sql) {
        return getEntityManager().createNativeQuery(sql);
    }

    /**
     * Carries out a native SQL query without changing the L2 Cache.
     * This is not normally needed as Hibernate should be able to tell if a select is used that it doesn't need to
     * invalidate anything. This is only needed if your query might confuse hibernate into thinking something
     * could have changed.
     * DO NOT USE IF YOU ARE CARRYING OUT AN UPDATE.
     * DO NOT USE UNLESS YOU ARE SURE THIS IS A PROBLEM - I'd like to delete this at somepoint if hibernate improves.
     */
    private Query createNativeQueryWithNoCacheInvalidation(String sql) {
        Query query = createNativeQuery(sql);
        setQuerySpaces(query, Collections.emptyList());
        return query;
    }

    /**
     * This is to allow a native query to be carried out without invalidating the whole of the Level 2 cache. By default
     * if Query.executeUpdate() is called hibernate flushes the whole of the L2 cache for the whole cluster. This is
     * 'bad' as it kills performance. This method allows for more fine-grained clearing of the cache.
     * @param querySpaces The names of the tables that will be effected and so should have their caches flushed
     *                    If this is empty then nothing will be removed from the cache.
     */
    private static void setQuerySpaces(Query wrappedQuery, @Nonnull Collection<String> querySpaces) {
        NativeQuery nativeQuery = wrappedQuery.unwrap(NativeQuery.class);
        if (querySpaces.isEmpty()) {
            nativeQuery.addSynchronizedQuerySpace("");
        } else {
            for (String querySpace : querySpaces) {
                nativeQuery.addSynchronizedQuerySpace(querySpace);
            }
        }
    }
}
