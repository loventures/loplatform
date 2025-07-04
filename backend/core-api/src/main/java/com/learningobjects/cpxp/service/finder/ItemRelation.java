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

package com.learningobjects.cpxp.service.finder;

import com.learningobjects.cpxp.entity.DomainEntity;
import com.learningobjects.cpxp.entity.LeafEntity;
import com.learningobjects.cpxp.entity.PeerEntity;
import com.learningobjects.cpxp.service.item.Item;

/**
 * Indicate how the finder support code should handle a relation, if any, with
 * the {@link Item} entity.
 */
public enum ItemRelation {

    /**
     * On creation, {@link Finder} instances are immediately paired with an
     * {@link Item} instance to establish their place in the hairarchy.
     */
    PEER(PeerEntity.class),

    /**
     * {@link Finder}s marked with this relation participate in the
     * {@link Item} hierarchy as leaf children but do not have peer
     * {@link Item} instances.
     */
    LEAF(LeafEntity.class),

    /**
     * {@link Finder}s marked with this relation do not participate in the
     * {@link Item} hierarchy, they are just associated with a domain.
     */
    DOMAIN(DomainEntity.class);

    private final Class<? extends Finder> baseClass;

    ItemRelation(final Class<? extends Finder> baseClass) {
        this.baseClass = baseClass;
    }

    /**
     * @return the base finder class for finders with this relationship.
     */
    public Class<? extends Finder> getBaseClass() {
        return baseClass;
    }

    /**
     * Is this item relation peered with an item.
     */
    public boolean isPeered() {
        return this == PEER;
    }
}
