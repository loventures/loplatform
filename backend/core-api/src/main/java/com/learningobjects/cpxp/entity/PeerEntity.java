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

package com.learningobjects.cpxp.entity;

import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.item.Item;
import org.hibernate.proxy.HibernateProxy;

import jakarta.persistence.*;

/**
 * A base class for finders that participate in the item tree and have a peer
 * item. Consequently they can have children and additional data-mapped attributes.
 */
@MappedSuperclass
public abstract class PeerEntity implements Finder {
    @Id
    private Long id;

    @Override
    public Long getId() {
        if ((id == null) && (this instanceof HibernateProxy)) {
            return (Long) ((HibernateProxy) this).getHibernateLazyInitializer().getIdentifier();
        }
        return id;
    }

    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROOT_ID", nullable = false)
    private Item root;

    @Override
    public Item getRoot() {
        return root;
    }

    @Override
    public void setRoot(final Item root) {
        this.root = root;
    }

    // This uses lazy fetch because our batch loaders load the owners in bulk.
    // Otherwise, loading 100 finders from the database in a query is followed
    // by 100 single item loads by hibernate.
    // Don't query on this column it's not indexed.
    // This column is either NULL or always equal to the id column, and the id column is indexed so
    // query on that. (We do fancy things under hibernate's hood to insure the finder's id is always its
    // owner's id)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Item owner;

    @Override
    public Item getOwner() {
        // This is plausibly bad because it will fetch the owner, but... reflection on a
        // proxy doesn't work so.
        // TODO: make it so that you can operate purely on a finder without fetching its owner.
        // would require facades and such to have a finder vs an actual item
        owner.setFinder(this); // post-load we need to make the finder link...
        return owner;
    }

    @Override
    public void setOwner(final Item owner) {
        owner.setFinder(this);
        this.owner = owner;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID", nullable = false)
    private Item parent;

    @Override
    public Item getParent() {
        return parent;
    }

    @Override
    public void setParent(final Item parent) {
        this.parent = parent;
    }

    @Column
    private String path;

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(final String path) {
        this.path = path;
    }

    @Column(updatable = false, insertable = false)
    private String del;

    @Override
    public String getDel() {
        return del;
    }

    @Override
    public void setDel(final String del) {
        this.del = del;
    }

    @Override
    public ItemRelation getItemRelation() {
        return ItemRelation.PEER;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public void setNew(final boolean isNew) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }
}
