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

package com.learningobjects.cpxp.util.entity;

/**
 * Implementations of this interface eliminate the need for reflection and
 * ensure that lazy loading in JPA/Hiberante behaves normally triggered by
 * concrete calls rather than potentially failing because of RTTI/reflection
 * based calls.
 */
public interface FinderManipulator {
    /**
     * Generic get method, implementations will add type specificity.
     *
     * @param item
     *            generic item to access
     * @param propertyName
     *            property whose value to retrieve
     * @return the value belonging to the named property
     */
    Object get(Object item, String propertyName);

    /**
     * Generic set method, implementations will add type specificity.
     *
     * @param item
     *            generic item to access
     * @param propertyName
     *            property whose value to set
     * @value the value to set on the named property
     */
    void set(Object item, String propertyName, Object value);
}
