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

import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataUtil;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.ObjectUtils;
import de.mon.DeMonitor;
import de.mon.StatisticType;

import java.util.*;

public abstract class DataTransfer {

    // Centralized ontology-based data access.

    public static String getStringData(Item item, String type, String... def) {
        return (String) getDataValue(item, type, false, (Object[]) def);
    }

    public static String getTextData(Item item, String type, String... def) {
        return (String) getDataValue(item, type, (Object[]) def);
    }

    public static Long getNumberData(Item item, String type, Long... def) {
        return (Long) getDataValue(item, type, (Object[]) def);
    }

    public static Item getItemData(Item item, String type, Item... def) {
        return (Item) getDataValue(item, type, (Object[]) def);
    }

    /**
     * Get a PK data field without checking for its deleted status.
     * This should only be used in performance-critical code that will
     * apply the deleted check elsewhere.
     */
    public static Long getPKData(Item item, String type, Item... def) {
        return (Long) getDataValue(item, type, true, (Object[]) def);
    }

    public static Date getTimeData(Item item, String type, Date... def) {
        return (Date) getDataValue(item, type, (Object[]) def);
    }

    public static Boolean getBooleanData(Item item, String type, Boolean... def) {
        return (Boolean) getDataValue(item, type, (Object[]) def);
    }

    // TODO: handle multiplicate values
    public static boolean isUpdate(Item item, Collection<Data> data) {
        for (Data datum: data) {
            if (!ObjectUtils.equals(datum.getValue(BaseOntology.getOntology()), getDataValue(item, datum.getType()))) {
                return true;
            }
        }
        return false;
    }

    public static Object getDataValue(Item item, String dataType, Object... def) {
        return getDataValue(item, dataType, false, def);
    }

    public static Object getDataValue(Item item, String dataType, boolean pk, Object... def) {
        if (item == null) {
            return (def.length > 0) ? def[0] : null;
        }
        Ontology ontology = BaseOntology.getOntology();
        String itemType = item.getType();
        final EntityDescriptor descriptor = ontology
                .getEntityDescriptor(itemType);
        Object value = null;
        if (descriptor == null || !descriptor.containsDataType(dataType)) {
            for (Data data : findRawData(item, dataType)) {
                value = data.getValue(BaseOntology.getOntology());
                if (value != null) {
                    break;
                }
            }
            if (pk && (value != null)) {
                value = ((Item) value).getId();
            }
        } else if (pk) {
            value = ((BaseEntityDescriptor) descriptor).getPK(item, dataType);
        } else {
            value = descriptor.get(item, dataType);
        }
        if ((value == null) && (def.length > 0)) {
            value = def[0];
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Collection<String> findStringData(Item item, String type) {
        return (Collection<String>) findDataValues(item, type);
    }

    @SuppressWarnings("unchecked")
    public static Collection<String> findTextData(Item item, String type) {
        return (Collection<String>) findDataValues(item, type);
    }

    @SuppressWarnings("unchecked")
    public static Collection<Long> findNumberData(Item item, String type) {
        return (Collection<Long>) findDataValues(item, type);
    }

    @SuppressWarnings("unchecked")
    public static Collection<Item> findItemData(Item item, String type) {
        return (Collection<Item>) findDataValues(item, type);
    }

    @SuppressWarnings("unchecked")
    public static Collection<Date> findTimeData(Item item, String type) {
        return (Collection<Date>) findDataValues(item, type);
    }

    @SuppressWarnings("unchecked")
    public static Collection<Boolean> findBooleanData(Item item, String type) {
        return (Collection<Boolean>) findDataValues(item, type);
    }

    public static Collection<?> findDataValues(Item item, String dataType) {
        if (item == null) {
            return Collections.emptySet();
        }
        Ontology ontology = BaseOntology.getOntology();
        String itemType = item.getType();
        final EntityDescriptor descriptor = ontology
                .getEntityDescriptor(itemType);
        if (fetchFromDataCollection(descriptor, dataType, item)) {
            List<Object> values = new ArrayList<Object>();
            for (Data data : findRawData(item, dataType)) {
                values.add(data.getValue(BaseOntology.getOntology()));
            }
            return values;
        } else {
            Object value = descriptor.get(item, dataType);
            return Collections.singleton(value);
        }
    }

    public static Collection<Data> getData(Item item) {
        DeMonitor.recordGlobalStatistic(StatisticType.Data$.MODULE$, item.getItemType(), 0);
        return item.getData();
    }

    public static Collection<Data> findRawData(Item item, String dataType) {
        DeMonitor.recordGlobalStatistic(StatisticType.Data$.MODULE$, item.getItemType(), 0);
        return item.findRawData(dataType, com.learningobjects.cpxp.util.ManagedUtils.getEntityContext().getEntityManager());
    }

    public static Iterable<Data> getCombinedData(Item item) {
        if (item == null) {
            return Collections.emptySet();
        }
        return Iterables.concat(getData(item), getNormalizedData(item, true));
    }

    /**
     * Returns all data normalized into an entity sub-class of Item, not
     * including nulls. The owner field is set, but these are not real
     * entities; they are just useful containers.
     */
    public static List<Data> getNormalizedData(Item item) {
        return getNormalizedData(item, false);
    }

    private static List<Data> getNormalizedData(Item item, boolean nodup) {
        if (item == null) {
            return Collections.emptyList();
        }
        Ontology ontology = BaseOntology.getOntology();
        String itemType = item.getType();
        EntityDescriptor descriptor = ontology.getEntityDescriptor(itemType);
        if (descriptor == null) {
            return Collections.emptyList();
        }
        List<Data> data = new ArrayList<Data>();
        for (String dataType : descriptor.getDataTypes()) {
            if (nodup && ontology.getDataType(dataType).global()) {
                continue;
            }
            Object value = descriptor.get(item, dataType);
            if (value != null) {
                Data datum = DataUtil.getInstance(dataType, value, BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
                datum.setOwner(item);
                data.add(datum);
            }
        }
        return data;
    }

    private static boolean fetchFromDataCollection(EntityDescriptor descriptor,
            String dataTypeName, Item item) {
        // no entity metadata, so the data value(s) have to be in the collection
        if (null == descriptor) {
            return true;
        }
        // if the data type is not in the entity metada, its value(s) have to be
        // in the data collection
        if (!descriptor.containsDataType(dataTypeName)) {
            return true;
        }
        // this is an edge case, it typically occurs when an Item that otherwise
        // has a generated entity is loaded indirectly so Hibernate is not able
        // to figure out the specific Item sub-class to use; is it possible the
        // item in question was not created correctly, as just a bare Item?
        if (!descriptor.getEntityType().isInstance(item.getFinder())) {
            return true;
        }
        return false;
    }
}
