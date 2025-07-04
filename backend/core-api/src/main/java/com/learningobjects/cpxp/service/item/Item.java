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

import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.finder.Finder;
import org.hibernate.annotations.Cache;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import org.hibernate.proxy.HibernateProxy;

import jakarta.persistence.*;
import java.util.*;

/**
 * An item. This is the primary representation of objects in the system.
 */
@Entity
@Cache(usage = READ_WRITE)
public class Item implements IdType {

    /**
     * Create a new Item prior to persistence.
     */
    public Item() {
    }

    /**
     * Create a new transient Item for an un-peered Finder.
     * @param finder the associated finder
     */
    public Item(final Finder finder) {
        this.id = finder.getId();
        this.type = finder.getItemType();
        this.deleted = finder.getDel();
        this.parent = finder.getParent();
        this.root = finder.getRoot();
        this.path = finder.getPath();
        this.finder = finder;
    }

    /** The id primary key. */
    @Id
    private Long id;

    /**
     * Get the id.
     *
     * @return The id.
     */
    @Override
    public Long getId() {
        if ((id == null) && (this instanceof HibernateProxy)) {
            return (Long) ((HibernateProxy) this).getHibernateLazyInitializer().getIdentifier();
        }
        return id;
    }

    /**
     * Sets the id on this item.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    public boolean equalsId(final Item other) {
        return (other != null) && getId().equals(other.getId());
    }

    /** The item type. */
    @Column(name = "TYPE_NAME", nullable = false)
    private String type;

    /**
     * Get the item type.
     *
     * @return The item type.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the item type.
     *
     * @param type
     *            The item type.
     */
    public void setType(final String type) {
        this.type = type;
    }

    public String type() {
        return this.type;
    }

    @Override
    public String getItemType() {
        return type;
    }

    @Column(updatable = false, insertable = false)
    private String deleted;

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    /** The data associated with this item. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @Cache(usage = READ_WRITE)
    private Collection<Data> data = new ArrayList<Data>();

    /**
     * Get the data associated with this item.
     *
     * @return The data associated with this item.
     */
    public Collection<Data> getData() {
        return data;
    }

    /**
     * Set the data associated with this item.
     *
     * @param data
     *            The data associated with this item.
     */
    public void setData(final Collection<Data> data) {
        this.data = data;
    }

    /**
     * Add data to this item.
     *
     * @param datum
     *            The data.
     */
    public void addData(final Data datum) {
        // assertions? non-null, not present?
        clearCache();
        Item owner = datum.getOwner();
        if (owner != null) {
            owner.removeData(datum);
        }
        getData().add(datum);
        datum.setOwner(this);
        datum.setRoot(this.getRoot());
    }

    /**
     * Remove data from this item.
     *
     * @param datum
     *            The data.
     */
    public void removeData(final Data datum) {
        // assertions? non-null, present?
        clearCache();
        getData().remove(datum);
        // data.setOwner(null); causes gs barf
    }

    /**
     * Removes all data from this item.
     *
     */
    public void removeAllData() {
        clearCache();
        getData().clear();
    }

    /** The parent item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID", nullable = true)
    private Item parent;

    /**
     * Get the parent.
     *
     * @return The parent.
     */
    public Item getParent() {
        return parent;
    }

    /**
     * DO NOT CALL THIS METHOD. Set the parent.
     *
     * @param parent
     *            The parent.
     */
    public void setParent(final Item parent) {
        this.parent = parent;
    }

    public Item parent() {
        return this.parent;
    }

    /** The root item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROOT_ID", nullable = true)
    private Item root;

    /**
     * Get the root.
     *
     * @return The root.
     */
    public Item getRoot() {
        return root;
    }

    /**
     * Set the root.
     *
     * @param root
     *            The root.
     */
    public void setRoot(final Item root) {
        this.root = root;
    }

    public Item root() {
        return this.root;
    }

    /** The finder. */
    @Transient
    private Finder finder;

    /**
     * Get the finder.
     *
     * @return The finder.
     */
    public Finder getFinder() {
        return finder;
    }

    /**
     * Set the finder.
     *
     * @param finder
     *            The finder.
     */
    public void setFinder(final Finder finder) {
        this.finder = finder;
    }

    public Finder finder() {
        return this.finder;
    }

    @Column
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String path() {
        return this.path;
    }

    /** Meta. */

    @Transient
    private transient Map<String,Set<Data>> _cache;

    private void clearCache() {
        _cache = null;
    }

    public void cacheData(final Collection<Data> datas) {
        _cache = new HashMap<String,Set<Data>>();
        for (Data data : datas) {
            final Set<Data> dataz = _cache.get(data.getType());
            if(dataz == null) {
                final Set<Data> newData = new HashSet<Data>();
                newData.add(data);
                _cache.put(data.getType(), newData);
            } else {
                dataz.add(data);
                _cache.put(data.getType(),dataz);
            }
        }
    }

    private Map<String,Set<Data>> dataCache(final EntityManager entityManager) {
        if (_cache == null) {
            cacheData(getData());
        }
        return _cache;
    }

    /**
     * Get all the data of a particular type by way of a cache. Note that this
     * will include no data that is exclusively mapped to a finder.
     *
     * @param typeName
     *            the data type name
     *
     * @param entityManager
     * @return the matching data
     */
    @SuppressWarnings("unchecked")
    public Collection<Data> findRawData(final String typeName, final EntityManager entityManager) {
        // Logger.getLogger("access-log").info(getType().getName() + "/" +
        // typeName + " ... " + where());
        Collection<Data> result = dataCache(entityManager).get(typeName);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Transient
    private transient boolean isNew;

    public boolean isNew() {
        return isNew;
    }

    public void setNew(final boolean isNew) {
        this.isNew = isNew;
    }

    @PostPersist
    @SuppressWarnings("unused")
    private void clearNew() {
        isNew = false;
    }

    /**
     * Return a summary of this item.
     *
     * @return a summary of this item
     */
    @Override
    public String toString() {
        return "Item[" + getId() + "/" + getType() + "]";
    }
}
