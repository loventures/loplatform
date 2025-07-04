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

import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.service.finder.Finder;

import javax.ejb.Local;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The item service.
 */
@Local
public interface ItemService {
    /**
     * Create a new item.
     *
     * @param type
     *            the item type
     *
     * @return the new item
     */
    public Item create(String type);

    /**
     * Create a new child item.
     *
     * @param parent
     *            the parent item
     * @param type
     *            the item type
     *
     * @return the new item
     */
    public Item create(Item parent, String type);

    /**
     * Create a new child item.
     *
     * @param parent
     *            the parent item
     * @param type
     *            the item type
     * @param init
     *            an initializer to apply before the item is persisted. During initialization the item
     *            has neither PK nor path so may not be added to any external references.
     *
     * @return the new item
     */
    public Item create(Item parent, String type, Consumer<Item> init);

    /**
     * Move an item.
     *
     * @param item
     *            the item to move
     * @param parent
     *            the new parent
     */
    public void move(Item item, Item parent);

    /**
     * Clear all data references to an item.
     *
     * @param item
     *            the item to clear
     *
     * @return the number of references
     */
    public int clearItemRefs(Item item);

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
    public int replaceItemRefs(Item item, Item value);

    /**
     * Destroy a subtree of items.
     *
     * @param item
     *            the apex item
     */
    public void destroy(Item item);

    /**
     * Destroy a subtree of items, optionally leaving the apex untouched.
     *
     * @param item
     *            the apex item
     * @param inclusive
     *            whether to also destroy the apex
     */
    public void destroy(Item item, boolean inclusive);

    public void deleteDomain(Item item);
    public void delete(Item item);
    public void delete(Finder finder);
    public void setDeleted(Item item, String deleted);

    public void clearDeleted(String deleted);

    /**
     * Get an item.
     *
     * @param id
     *            The id.
     *
     * @return The item, or null.
     */
    public Item get(Long id);
    public Item get(Long id, String itemType);
    public <T extends Finder> T get(Long id, Class<T> clas);

    /**
     * Get a collection of items.
     *
     * @param ids the ids
     *
     * @return The found items. Order is maintained. Deleted or unknown ids will not be present.
     */
    public List<Item> get(Iterable<Long> ids);
    public List<Item> get(Iterable<Long> ids, String itemType);
    public Map<Long, Item> map(Iterable<Long> ids);
    public Map<Long, Item> map(Iterable<Long> ids, String itemType);
    public void preloadItemsInPath(String path, String itemType);
    public Multimap<Long, Item> preloadChildren(Iterable<Long> parentIds, String itemType);
    public Multimap<Long, Item> preloadSingleChildren(Iterable<Long> parentIds, String itemType);
    public Multimap<Long, Item> preloadOrderedChildren(Iterable<Long> parentIds, String itemType, String dataType);
    public void preloadData(Iterable<Item> items);

    /**
     * Ensures the finder, if the item is finderized, is loaded so that data
     * operations work correctly.
     *
     * @param item
     *            the item to check and load
     * @return if false a finder could not be load, if true it has one already
     *         or one was loaded if needed without a problem
     */
    public boolean loadFinder(Item item);

    /**
     * Count the number of descendants of this item, including orphans.
     *
     * @param item
     *          the ancestor item
     * @return
     *          the descendant count
     */
    public long countDescendants(Item item);

    /**
     * Find all descendents of the specified node that are parent to a leaf.
     *
     * @param root
     *            the tree under which to search for leaf parents
     *
     * @return a multimap from parent ids to the leaf item types
     */
    public Multimap<Long, String> findLeafParents(Item root);

    boolean pessimisticLock(Item item);

    /**
     * Refresh an entity from the database and lock its backing rows for
     * the duration of the transaction.
     *
     * @param item The item to refresh
     * @param lock whether to lock the item while refreshing
     * @param timeout A duration in milliseconds to wait, or None to wait forever.
     *                Ignored if {@param lock} is false
     * @return whether the lock was successfully obtained
     */
    public boolean lock(Item item, boolean lock, boolean refresh, Optional<Long> timeout);

    /**
     * Checks if an entity has been locked by Hibernate through the item service
     * @param item The item to check
     * @return whether a Hibernate lock has been obtained for this item
     */
    public boolean isLocked(Item item);
}
