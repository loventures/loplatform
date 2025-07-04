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

import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.item.ItemTypedef;

import java.util.Map;

public interface Ontology {
    /**
     * Get the known item types.
     *
     * @return a map from item type names to item type definitions
     */
    Map<String, ItemTypedef> getItemTypes();

    /**
     * Get whether a value is a known item type.
     * @param itemType the value
     * @return whether it is a known item type
     */
    boolean isItemType(String itemType);

    /**
     * Get a particular item type.
     *
     * @param typeName
     *            the item type name
     *
     * @return the item type definition
     */
    ItemTypedef getItemType(String typeName);

    /**
     * Get the known data types.
     *
     * @return a map from strings to data type definitions
     */
    Map<String, DataTypedef> getDataTypes();

    /**
     * Get a particular data type.
     *
     * @param typeName
     *            the data type name
     *
     * @return the data type definition
     */
    DataTypedef getDataType(String typeName);

    /**
     * Is a given item type {@link ItemRelation}.STANDALONE.
     *
     * NOTE: Returns false for unrecognized types.
     *
     * @param typeName
     *          The item type name
     *
     * @return boolean
     */
    boolean isStandalone(String typeName);

    DataFormat getDataFormat(String typeName);

    Map<String, EntityDescriptor> getEntityDescriptors();

    EntityDescriptor getEntityDescriptor(String itemType);

    EntityDescription getEntityDescription(String itemType);

    String getItemTypeForFinder(Class<?> finderType);

    void updateMaps();

    void analyzeClass(Class<?> clas);
}
