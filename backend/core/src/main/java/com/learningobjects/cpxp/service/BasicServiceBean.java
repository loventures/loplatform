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

package com.learningobjects.cpxp.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.service.copy.DefaultItemCopier;
import com.learningobjects.cpxp.service.copy.ItemCopier;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.query.QueryCache;
import com.learningobjects.cpxp.service.query.SubselectQueryBuilder;
import com.learningobjects.cpxp.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Superclass for basic service bean implementations. Provides access to the
 * current user and the basic services.
 */
public abstract class BasicServiceBean {
    private static final Logger logger = Logger.getLogger(BasicServiceBean.class.getName());

    /** The item service. */
    @Inject
    protected ItemService _itemService;

    @Inject
    protected QueryCache _queryCache;

    protected BasicServiceBean() { /* and use field di... */ }

    protected BasicServiceBean(
      ItemService itemService,
      QueryCache queryCache
    ) {
        _itemService = itemService;
        _queryCache = queryCache;
    }

    // This is necessary for two reasons..
    // 1. If an item is locked in the same transaction as it is created,
    // it needs to be flushed or the lock attempt fails.
    // 2. If an item is set as the data on another item in the same
    // transaction as it is created, it needs to be flushed or the
    // JPA may try to create the data first and fail because the item
    // has no id.
    protected Long flush(Item item) {
        getEntityManager().flush();
        return item.getId();
    }

    protected Item refresh(Item item) {
        // I need to reload because the EM clear may have wiped me out
        if ((item.getId() == null) || (item.getId().longValue() < 0)) {
            Finder finder = item.getFinder();
            if (!getEntityManager().contains(finder)) {
                item = getEntityManager().find(finder.getClass(), finder.getId()).getOwner();
            }
        } else if (!getEntityManager().contains(item)) {
            item = getEntityManager().find(Item.class, item.getId());
        }
        return item;
    }

    /** The current domain. */
    protected Item getCurrentDomain() {
        Long domainId = Current.getDomain();
        if (domainId == null) {
            throw new IllegalStateException("Current domain unset");
        }
        Item domain = _itemService.get(domainId);
        return domain;
    }

    /** The current user. */
    protected Item getCurrentUser() {
        Long userId = Current.getUser();
        if (userId == null) {
            throw new IllegalStateException("Current user unset");
        }
        return _itemService.get(userId);
    }

    /** The current time. */
    protected Date getCurrentTime() {
        Date time = Current.getTime();
        if (time == null) {
            throw new IllegalStateException("Current time unset");
        }
        return time;
    }

    /** The approximate time rounded down to a minute. */
    @Nonnull
    protected Date getApproximateTime() {
        Date time = Current.getTime();
        if (time == null) {
            time = new Date();
        }
        return DateUtils.getApproximateTime(time);
    }

    /** The approximate time rounded up to a minute. */
    @Nonnull
    protected Date getApproximateTimeCeiling() {
        Date time = Current.getTime();
        if (time == null) {
            time = new Date();
        }
        return DateUtils.getApproximateTimeCeiling(time);
    }

    // TODO: asUser() and asDomain() methods that perform op with overridden
    // setting

    /** Throws if the item is non-null and of a different type. */
    protected Item assertItemType(Item item, String type) {
        if ((item != null) && !type.equals(item.getType())) {
            throw new RuntimeException("Item<"
              + item.getType() + ":" + item.getId()
              + "> is not a: " + type);
        }
        return item;
    }

    // TODO: assertParent(), assertSibling() methods, etc.
    /**
     * Gets the Id of a possibly-null item.
     */
    protected Long getId(Item item) {
        return (item == null) ? null : item.getId();
    }

    protected Item getDomainItemById(String id) {
        return getDomainItemById(getCurrentDomain().getId(), id);
    }

    protected Item getDomainItemById(Long domainId, String id) {
        Item item = ServiceContext.getContext()
                .findDomainItemById(domainId, id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown domain item: "
                    + domainId + "/" + id);
        }
        return item;
    }

    protected Item findDomainItemById(String id) {
        return findDomainItemById(getCurrentDomain().getId(), id);
    }

    protected Item findDomainItemById(Long domainId, String id) {
        return ServiceContext.getContext().findDomainItemById(domainId, id);
    }

    // Utility methods for name service

    public static final int MAX_BINDING_LENGTH = 32;

    public static String getBindingPattern(String path, String name, String alt) {
        return getBindingPattern(path, name, alt, MAX_BINDING_LENGTH);
    }

    public static String getBindingPattern(String path, String name,
            String alt, int maxLength) {
        if (name != null) {
            name = StringUtils.asciify(name); // remove accents
            name = name.replaceAll("[^-_.a-zA-Z0-9 @]", ""); // strip
            // non-alphanumerics, - or _ or @
            name = name.replaceAll(" +", " ").trim(); // collapse spaces
            if (name.length() > maxLength) { // trim to n chars, pref at word
                // boundary
                int index = name.lastIndexOf(' ', 1 + maxLength);
                if (index > maxLength / 2) { // only trim to space if it is at
                    // least half the max length
                    name = name.substring(0, index);
                } else {
                    name = name.substring(0, maxLength);
                }
            }
            name = name.replace(' ', '_'); // space to underscore
            name = name.replace('@', '_'); // @ to underscore
            if (name.matches("[^a-zA-Z0-9]*")) { // all symbols..
                name = "";
            }
        }
        String separator = path.endsWith("/") ? "" : "/";
        String suffix = StringUtils.defaultIfEmpty(name, alt);
        if (StringUtils.isEmpty(suffix)) {
            suffix = NumberUtils.generateId();
        }
        String prefix = path + separator + suffix;
        return prefix + "$_%$";
    }

    public static final int MAX_FILENAME_BINDING_LENGTH = 64;
    public static final int MAX_FILETYPE_BINDING_LENGTH = 10; // properties

    public static String getFilenameBindingPattern(String path, String name,
            String alt) {
        String base = cleanFilename(StringUtils.defaultIfEmpty(name, alt));
        int index = base.lastIndexOf('.');
        String prefix = (index < 0) ? base : base.substring(0, index);
        if (prefix.length() > MAX_FILENAME_BINDING_LENGTH) {
            prefix = prefix.substring(0, MAX_FILENAME_BINDING_LENGTH);
        }
        if (prefix.matches("[^a-zA-Z0-9]*")) { // all symbols..
            prefix = NumberUtils.generateId().replace('.', '_');
        }
        String separator = path.endsWith("/") ? "" : "/";
        prefix = path + separator + prefix;
        String suffix = (index < 0) ? "" : base.substring(index);
        if (suffix.length() > MAX_FILETYPE_BINDING_LENGTH) {
            suffix = suffix.substring(0, 1 + MAX_FILETYPE_BINDING_LENGTH); // allow the .
        }
        return prefix + "$_%$" + suffix;
    }

    protected static String cleanFilename(String name) {
        return StringUtils.cleanFilename(name);
    }

    // No ACL checks
    protected List<Item> findByParentAndType(Item parent, String type) {
        QueryBuilder qb = queryParent(parent, type);
        return qb.getItems();
    }

    // No ACL checks
    protected List<Item> findByParentAndTypeAndStringData(Item parent,
            String type, String dataType, String value) {
        QueryBuilder qb = queryParent(parent, type);
        qb.addCondition(dataType, "eq", value);
        return qb.getItems();
    }

    // No ACL checks

    protected void invalidateQuery(String invalidationKey) {
        _queryCache.invalidate(invalidationKey);
    }

    protected QueryBuilder queryParent(Item parent, String itemType) {
        QueryBuilder qb = queryBuilder();
        qb.setParent(parent);
        if (itemType != null) {
            qb.setItemType(itemType);
        }
        return qb;
    }

    protected QueryBuilder queryRoot(Item root, String itemType) {
        QueryBuilder qb = queryBuilder();
        qb.setRoot(root);
        if (itemType != null) {
            qb.setItemType(itemType);
        }
        return qb;
    }

    // TODO: This becomes controversial when different domains
    // can have different ontologies. We obviously use it only
    // for our static types, but still..
    protected QueryBuilder querySystem(String itemType) {
        QueryBuilder qb = queryBuilder();
        qb.setItemType(itemType);
        return qb;
    }

    protected QueryBuilder queryBuilder() {
        return new SubselectQueryBuilder(ServiceContext.getContext(), _queryCache);
    }

    protected ItemCopier itemCopier(Item source) {
        return itemCopier(source, false);
    }

    protected ItemCopier itemCopier(Item source, boolean copyInternalData) {
        return new DefaultItemCopier(source, ServiceContext.getContext(), copyInternalData, false);
    }

    /**
     * Get all the children of a particular item including orphans.
     *
     * @param parent
     *            the parent item
     * @param leafParents
     *            the orphan points
     *
     * @return all the children, including orphans
     */
    protected Iterable<Item> getAllChildren(Item parent,
            Multimap<Long, String> leafParents) {
        return getAllChildren(parent, leafParents, false);
    }

    protected Iterable<Item> getAllChildren(Item parent,
            Multimap<Long, String> leafParents, boolean deleted) {
        List<Iterable<Item>> childrens = new ArrayList<>();
        QueryBuilder iqb = queryParent(parent, null);
        iqb.setCacheQuery(false);
        iqb.setIncludeDeleted(deleted);
        childrens.add(iqb.getItems());
        for (String orphan : leafParents.get(parent.getId())) {
            // I don't want to cache this query
            QueryBuilder qb = queryParent(parent, orphan);
            qb.setCacheQuery(false);
            qb.setIncludeDeleted(deleted);
            childrens.add(qb.getItems());
        }
        return Iterables.concat(childrens);
    }

    /**
     * Get all the children of an item of a particular type.
     *
     * @param item
     *            the item
     * @param childType
     *            the child type
     *
     * @return the children
     */
    protected List<Item> getChildren(Item item, String childType) {
        return queryParent(item, childType).getItems();
    }

    /**
     * Gets the persistence context.
     */
    protected EntityManager getEntityManager() {
        EntityContext ec = ManagedUtils.getEntityContext();
        if (ec == null) {
            throw new RuntimeException("Not within a session");
        }
        return ec.getEntityManager();
    }

    protected Query createQuery(String query) {
        logger.log(Level.FINE, "Query, {0}", query);
        return getEntityManager().createQuery(query);
    }

}
