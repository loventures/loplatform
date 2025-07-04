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

package com.learningobjects.de.authorization;

import com.learningobjects.cpxp.component.ComponentSupport;
import loi.cp.right.Right;
import loi.cp.right.RightMatch;
import loi.cp.right.RightService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The rights collected by the subject in the {@link SecurityContext}.
 *
 * This class will eventually be more than a primitive forwarding set. It'll cache stuff and be lazy n stuff.
 */
public class CollectedRights implements CollectedAuthority {

    private final Set<Class<? extends Right>> rights = new HashSet<>();

    public boolean contains(final Class<? extends Right> right, RightMatch match) {
        if (match == RightMatch.EXACT) {
            return this.rights.contains(right);
        } else {
            RightService rightService = ComponentSupport.lookupService(RightService.class);
            Set<Class<? extends Right>> rightTree = rightService.getDescendants(right);
            if (match == RightMatch.ANY) {
                return !Collections.disjoint(this.rights, rightTree);
            } else {
                return this.rights.containsAll(rightTree);
            }
        }
    }

    public boolean add(final Class<? extends Right> right) {
        return rights.add(right);
    }

    public boolean addAll(final Collection<? extends Class<? extends Right>> c) {
        return rights.addAll(c);
    }

    public Set<Class<? extends Right>> getRights() {
        return rights;
    }
}
