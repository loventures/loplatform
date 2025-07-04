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

package com.learningobjects.cpxp.dto;

import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.item.Item;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;

public interface EntityDescriptor {
    /**
     * @return the type of the generated entity, useful for reflection
     */
    Class<?> getEntityType();

    /**
     * @return fully qualified name, useful in JQL statements
     */
    String getEntityName();

    /**
     * @return table name, useful in SQL statements
     */
    String getTableName();

    /**
     * Factory method using reflection to create a new instance of the
     * associated entity type.
     *
     * @return a new instance ready for use with JPA/Hibernate
     */
    Finder newInstance();

    /**
     * Get the property descriptors.
     *
     * @return the property descriptors
     */
    Map<String, PropertyDescriptor> getPropertyDescriptors();

    /**
     * Wrapper to dynamically access an entity's property value given a
     * reference of the owning type, {@link Item}, and a known data type.
     *
     * @param item
     *            entity to access
     * @param dataType
     *            known data type of the provided value
     * @return value of the appropriate entity property
     */
    Object get(Item item, String dataType);

    /**
     * Wrapper to dynamically access the entity PK from an entity's property value given a
     * reference of the owning type, {@link Item}, and a known data type.
     *
     * This does not perform a deleted check on the related entity and should only be
     * used where performance is a concern and the deleted check is not necessary or
     * performed at a higher level.
     *
     * @param item
     *            entity to access
     * @param dataType
     *            known data type of the provided value
     * @return PK value of the appropriate entity property
     */
    Long getPK(Item item, String dataType);

    /**
     * Wrapper to dynamically access an entity's property values give a generic
     * reference to a {@link Finder} and a known data type.
     *
     * @param finder
     *            entity to access
     * @param dataType
     *            known data type of the provided value
     * @return value of the appropriate entity property
     */
    Object get(Finder finder, String dataType);

    /**
     * Wrapper to dynamically access an entity's property value given a
     * reference of the owner type, {@link Item}, and a known data type.
     *
     * @param item
     *            entity to access
     * @param dataType
     *            known data type of the provided value
     * @param value
     *            to set on the appropriate entity property
     */
    void set(Item item, String dataType, Object value);

    /**
     * Wrapper to dynamically access an entity's property value given a
     * reference of the {@link Finder}, and a known data type.
     *
     * @param finder
     *            entity to access
     * @param dataType
     *            known data type of the provided value
     * @param value
     *            to set on the appropriate entity property
     */
    void set(Finder finder, String dataType, Object value);

    /**
     * @return policy for creation and association of an {@link Item} with a
     *         {@link Finder}
     */
    ItemRelation getItemRelation();

    /**
     * @return sorted, all of the data type names associated with this entity
     */
    List<String> getDataTypes();

    /**
     * Useful check to see if a data type is part of the generated entity or
     * needs to be fetched and manipulated as a Data entity.
     *
     * @param name
     *            type name
     * @return whether this description includes the specified type
     */
    boolean containsDataType(String name);

    /**
     * Useful in EJB-QL generation to refer to properties on generated entities
     * correctly.
     *
     * @param name
     *            type name
     * @return property name relative to the owning entity
     */
    String getPropertyName(String name);

    /**
     * @return DDL statements that can be run to index the backing table for the
     *         entity
     */
    List<String> generateIndices();

    EntityDescription toDescription();
}
