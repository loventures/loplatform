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

package com.learningobjects.cpxp.util;

import com.google.common.base.Predicate;
import com.learningobjects.cpxp.Id;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Ids {
    private Ids() {
    }

    public static Id of(final Long id) {
        return new Id() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public boolean equals(Object o) {
               return (o instanceof Id) && id.equals(((Id) o).getId());
            }

            @Override
            public int hashCode() {
                return id.hashCode();
            }
        };
    }

    public static Long get(final Id id) {
        return (id == null) ? null : id.getId();
    }

    @Nonnull
    public static List<Long> get(Iterable<? extends Id> ids) {
        List<Long> longs = new ArrayList<>();
        if (ids != null) {
            for (Id id : ids) {
                Long idNumber = get(id);
                if (id != null) {
                    longs.add(idNumber);
                }
            }
        }
        return longs;
    }

    /**
     * A predicate for "does not have an id in this list of ids"
     * @param idsToExclude the ids which should not match the id of the thing tested
     * @param <T> a class which extends Id
     * @return a predicate to test for excluding these ids
     */
    public static <T extends Id> Predicate<T> notIn(final List<Long> idsToExclude) {
        return filterOnInclusivity(idsToExclude, false);
    }

    /**
     * A predicate for "has an id in this list of ids"
     * @param idsToInclude the ids which should match the id of the thing tested
     * @param <T> a class which extends Id
     * @return a predicate to test for including these ids
     */
    public static <T extends Id> Predicate<T> in(final List<Long> idsToInclude) {
        return filterOnInclusivity(idsToInclude, true);
    }

    private static <T extends Id> Predicate<T> filterOnInclusivity(final List<Long> idsToInclude, final boolean include) {
        return item -> include == idsToInclude.contains(item.getId());
    }
}
