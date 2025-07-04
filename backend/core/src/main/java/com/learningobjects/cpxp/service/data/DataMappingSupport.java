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

package com.learningobjects.cpxp.service.data;

import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.item.Item;

/**
 * Mixin to handle Finder CRUD operations conditionally triggered from
 * callers manipulating {@link Data} entities.
 */
class DataMappingSupport {
    private static DataMappingSupport __supportInstance;

    private final Ontology _ontology = BaseOntology.getOntology();

    private DataMappingSupport() {
        // enforce Singleton pattern
    }

    static DataMappingSupport getInstance() {
        synchronized (DataMappingSupport.class) {
            if (null == __supportInstance) {
                __supportInstance = new DataMappingSupport();
            }

            return __supportInstance;
        }
    }

    /**
     * Test whether a particular data type is normalized for a given item.
     * Note that a data type may be both normalized and data mapped.
     *
     * @param item
     *            to look up metadata
     * @param dataType
     *            to look up metadata
     * @return whether the requested data type is normalized into a generated
     *         Finder
     */
    static boolean isNormalized(Item item, String dataType) {
        assert item != null : "Item must be valid!";
        assert item.getType() != null : String.format("Item type must be valid, for item %1$s.", item);
        EntityDescriptor entityDescriptor = getInstance()._ontology
                .getEntityDescriptor(item.getType());

        return null == entityDescriptor ? false : entityDescriptor
                .containsDataType(dataType);
    }

    /**
     * Test whether a particular data type is data mapped for a given item.
     * Note that a data type may be both data mapped and normalized.
     *
     * @param item
     *            to look up metadata
     * @param dataType
     *            to look up metadata
     * @return whether the requested types is data mapped
     */
    static boolean isDataMapped(Item item, String dataType) {
        return !isNormalized(item, dataType) || isGlobal(dataType);
    }

    /**
     * Test whether a particular datatype is global and needs to be
     * data mapped, possibly in addition to the normalized mapping.
     *
     * @param dataType
     *            to look up metadata
     * @return whether the requested type is global
     */
    static boolean isGlobal(String dataType) {
        DataTypedef typedef = getInstance()._ontology
                .getDataType(dataType);
        return typedef.global();
    }

    /**
     * Wraps metadata and reflection code to update the configured generated
     * entity property based on the requested types.
     *
     * @param item
     *            to look up metadata
     * @param data
     *            to look up metadata
     */
    static void update(Item item, Data data) {
        getInstance().updateProperty(item, data);
    }

    /**
     * Wraps metadata and reflection code to get the configured generated
     * entity property based on the requested types.
     *
     * @param item
     *            to look up metadata
     * @param dataType
     *            to look up metadata
     */
    static Object get(Item item, String dataType) {
        return getInstance().getProperty(item, dataType);
    }

    /**
     * Wraps metadata and reflection code to clear the configured generated
     * entity property based on the requested types.
     *
     * @param item
     *            to look up metadata
     * @param dataType
     *            to look up metadata
     */
    static void clear(Item item, String dataType) {
        getInstance().clearProperty(item, dataType);
    }

    /**
     * Useful for pivoting a clear all from a Data collection to a normalized
     * entity.
     *
     * @param item
     *            possible generated sub-class instance
     */
    public static void clearAll(Item item) {
        getInstance().clearAllProperties(item);
    }

    private void updateProperty(Item item, Data data) {
        assert data.getType() != null : "Data type must be valid.";
        String dataType = data.getType();
        assert item.getType() != null : "Item type must be valid.";
        String itemType = item.getType();
        EntityDescriptor descriptor = _ontology.getEntityDescriptor(itemType);

        if (null == descriptor) { // TODO: throw
            return;
        }

        descriptor.set(item, dataType, data.getValue(BaseOntology.getOntology()));
    }

    private Object getProperty(Item item, String type) {
        String dataType = type;
        assert item.getType() != null : "Item type must be valid.";
        String itemType = item.getType();
        EntityDescriptor descriptor = _ontology.getEntityDescriptor(itemType);

        if (null == descriptor) { // TODO: throw
            return null;
        }

        return descriptor.get(item, dataType);
    }

    private void clearProperty(Item item, String dataType) {
        assert dataType != null : "Data type must be valid.";
        String dataTypeName = dataType;
        assert item.getType() != null : "Item type must be valid.";
        String itemType = item.getType();
        EntityDescriptor descriptor = _ontology.getEntityDescriptor(itemType);

        if (null == descriptor) { // TODO: throw
            return;
        }

        descriptor.set(item, dataTypeName, null);
    }

    private void clearAllProperties(Item item) {
        assert item.getType() != null : "Item type must be valid.";
        String itemType = item.getType();
        EntityDescriptor descriptor = _ontology.getEntityDescriptor(itemType);

        if (null == descriptor) { // TODO: throw
            return;
        }

        for (String dataType : descriptor.getDataTypes()) {
            descriptor.set(item, dataType, null);
        }
    }
}
