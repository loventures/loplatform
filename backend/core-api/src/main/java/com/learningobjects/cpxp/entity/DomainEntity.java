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
 * A very lightweight entity that only obeys the contract that it
 * has a PK and exists in a particular domain.
 */
@MappedSuperclass
public abstract class DomainEntity implements Finder {
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

    @Transient
    private Item owner;

    @Override
    public Item getOwner() {
        if (owner == null) {
            // make a fake, non-persisted item
            owner = new Item(this);
        }
        return owner;
    }

    @Override
    public void setOwner(final Item owner) {
        this.owner = owner;
    }

    @Override
    public Item getParent() {
        return root;
    }

    @Override
    public void setParent(final Item parent) {
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void setPath(final String path) {
    }

    @Override
    public String getDel() {
        return null;
    }

    @Override
    public void setDel(final String del) {
    }

    @Override
    public ItemRelation getItemRelation() {
        return ItemRelation.DOMAIN;
    }

    @Transient
    private transient boolean isNew;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public void setNew(final boolean isNew) {
        this.isNew = isNew;
    }

    @PostPersist
    @SuppressWarnings("unused")
    private void clearNew() {
        isNew = false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }
}
