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
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.ManagedUtils;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The data service.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class DataServiceBean implements DataService {
    private static final Logger logger = Logger.getLogger(DataServiceBean.class.getName());

    /**
     * Create a new string data.
     */
    public void createString(Item owner, String type, String string) {

        Data data = new Data();
        data.setType(type);
        data.setString(string, BaseOntology.getOntology());
        persist(owner, data, false);

    }

    /**
     * Add a new time data.
     */
    public void createTime(Item owner, String type, Date value) {

        Data data = new Data();
        data.setType(type);
        data.setTime(value);
        persist(owner, data, false);

    }

    /**
     * Create a new text data.
     */
    public void createText(Item owner, String type, String text) {

        Data data = new Data();
        data.setType(type);
        data.setText(text);
        persist(owner, data, false);

    }

    /**
     * Create a new number data.
     */
    public void createNumber(Item owner, String type, Long number) {

        Data data = new Data();
        data.setType(type);
        data.setNumber(number);
        persist(owner, data, false);

    }

    /**
     * Create a new boolean data.
     */
    public void createBoolean(Item owner, String type, Boolean bool) {

        Data data = new Data();
        data.setType(type);
        data.setBoolean(bool);
        persist(owner, data, false);

    }

    /**
     * Add a new item data.
     */
    public void createItem(Item owner, String type, Item value) {

        try {

        Data data = new Data();
        data.setType(type);
        data.setItem(value); // TODO: Verify domain of value is sane
        persist(owner, data, false);

        } catch (Exception ex) {
            logger.log(Level.WARNING, "createItem error", ex);
        }

    }

    /**
     * Remove a data entity from the database. The entity is
     * unlinked from its owner collection, if any.
     *
     * Data must be a real entity (i.e. non-finderized).
     */
    public void remove(Data data) {

        Item owner = data.getOwner();
        if (owner != null) {
            owner.removeData(data); // TODO: technically this is a hit on data
        }
        getEntityManager().remove(data);

    }

    /**
     * Data may be of a finderized type.
     */
    public void copy(Item owner, Data data) {

        persist(owner, data, false);

    }

    /**
     * Remove all data of a specific type from an Item.
     */
    public void clear(Item owner, String type) {

        if (DataMappingSupport.isNormalized(owner, type)) {
            DataMappingSupport.clear(owner, type);
        }
        if (DataMappingSupport.isDataMapped(owner, type)) {
            List<Data> toRemove = new ArrayList<Data>();
            for (Data data: DataTransfer.getData(owner)) {
                if (type.equals(data.getType())) {
                    toRemove.add(data);
                }
            }
            for (Data data: toRemove) {
                remove(data);
            }
        }

    }

    /**
     * Remove all data from an item.
     */
    public void clear(Item owner) {

        for (Data data: new ArrayList<Data>(DataTransfer.getData(owner))) {
            remove(data);
        }
        DataMappingSupport.clearAll(owner);

    }

    /**
     * Set a text value. Any existing value will be replaced.
     */
    public void setText(Item owner, String type, String value) {

        setType(owner, type, value);

    }

    /**
     * Set a string value. Any existing value will be replaced.
     */
    public void setString(Item owner, String type, String value) {

        setType(owner, type, value);

    }

    /**
     * Set a number value. Any existing value will be replaced.
     */
    public void setNumber(Item owner, String type, Long value) {

        setType(owner, type, value);

    }

    /**
     * Set a date value. Any existing value will be replaced.
     */
    public void setTime(Item owner, String type, Date value) {

        setType(owner, type, value);

    }

    /**
     * Set a boolean value. Any existing value will be replaced.
     */
    public void setBoolean(Item owner, String type, Boolean value) {

        setType(owner, type, value);

    }

    /**
     * Set an item value. Any existing value will be replaced.
     */
    public void setItem(Item owner, String type, Item value) {

        setType(owner, type, value);

    }

    public void setType(Item owner, String type, Object value) {
        Data existing = DataMappingSupport.isDataMapped(owner, type)
            ? com.google.common.collect.Iterables.getFirst(DataTransfer.findRawData(owner, type), null) : null;
        if (existing != null) {
            DataUtil.setValue(existing, value, BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
            Collection<Data> c = DataTransfer.findRawData(owner, type);
            if (c.size() > 1) { // Strip out duplicates
                for (Data d: c) {
                    if ((d != null) && (d != existing)) {
                        remove(d);
                    }
                }
            }
        }
        if (DataMappingSupport.isNormalized(owner, type) || (existing == null)) {
            Data data = DataUtil.getInstance(type, value, BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
            persist(owner, data, (existing != null));
        }
    }

    /**
     * Set a collection of data on an item.
     *
     * Data may include finderized and non-finderized.
     */
    public void setData(Item item, Iterable<Data> datas) {

        // First organize the new data by type.
        Map<String, LinkedList<Data>> dataMap = new HashMap<String, LinkedList<Data>>();
        for (Data data : datas) {
            String type = data.getType();
            LinkedList<Data> dataTypeList = dataMap.get(type);
            if (dataTypeList == null) {
                dataTypeList = new LinkedList<Data>();
                dataMap.put(type, dataTypeList);
            }
            dataTypeList.add(data);
        }

        // Next update existing data on the item, removing lost fields
        List<Data> destroyList = new ArrayList<Data>();
        for (Data data : DataTransfer.getData(item)) {
            String type = data.getType();
            LinkedList<Data> dataTypeList = dataMap.get(type);
            if (dataTypeList != null) {
                // If it is normalized and global then I set the normal and data instances here
                if (!dataTypeList.isEmpty() && DataMappingSupport.isDataMapped(item, type)) {
                    Data replacement = dataTypeList.removeFirst();
                    DataUtil.setValue(data, replacement.getValue(BaseOntology.getOntology()), BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
                    if (DataMappingSupport.isNormalized(item, type)) {
                        persist(item, data, true);
                    }
                } else {
                    destroyList.add(data);
                }
            }
        }
        for (Data data : destroyList) {
            remove(data);
        }

        // Finally add any necessary new data fields
        for (LinkedList<Data> dataList : dataMap.values()) {
            for (Data data : dataList) {
                persist(item, data, false);
            }
        }

    }

    /**
     * @param owner      the owner for the data
     * @param data       the data to persist
     * @param dataMapped whether data mapping has already been performed
     */
    private void persist(Item owner, Data data, boolean dataMapped) {
        // I don't roll the setType() code in here because this needs to
        // support multiple instances of a data type where setType()
        // keeps just a single instance
        if (DataMappingSupport.isNormalized(owner, data.getType())) {
            DataMappingSupport.update(owner, data);
        }
        if (!dataMapped && DataMappingSupport.isDataMapped(owner, data.getType())) {
            Ontology ontology = BaseOntology.getOntology();
            Item item = data.getItem();
            if ((item != null) && !item.getRoot().equals(owner.getRoot())) {
                throw new IllegalArgumentException("Cross-domain data: " + item.getId());
            }
            if (ontology.isStandalone(owner.getType())) {
                throw new IllegalArgumentException("Non-finderized data: " + data.getType() + " on standalone itemtype: " + owner.getType());
            }
            if (ontology.getDataType(data.getType()).value() == DataFormat.json) {
                throw new IllegalArgumentException("Setting JSON on data-mapped type " + data.getType());
            }
            if (ontology.getDataType(data.getType()).entityOnly()) {
                throw new IllegalArgumentException("Setting entity-only data type " + data.getType() + " on " + owner);
            }
            if (ontology.getItemType(owner.getType()).noData()) {
                throw new IllegalArgumentException("Setting data type " + data.getType() + " on non-data item " + owner);
            }
            Data newData = new Data();
            newData.setType(data.getType());

            DataUtil.setValue(newData, data.getValue(ontology), ontology, ServiceContext.getContext().getItemService());
            owner.addData(newData); // TODO: this is technically a hit on data
            getEntityManager().persist(newData);
        }
    }

    public void normalize(String itemType){
        normalize(itemType, Optional.empty());
    }

    public void normalize(String itemType, Set<String> dataTypes){
        normalize(itemType, Optional.of(dataTypes));
    }

    private void normalize(String itemType, Optional<Set<String>> dataTypes) {
        logger.log(Level.INFO, "Normalizing, {0}", itemType);
        EntityDescriptor entityDescriptor = BaseOntology.getOntology()
            .getEntityDescriptor(itemType);
        int mapped = 0, deleted = 0;
        if (entityDescriptor != null) {
            Set<String> typesToNormalize = dataTypes.orElseGet(() -> new HashSet<>(entityDescriptor.getDataTypes()));

            String table = entityDescriptor.getTableName();
            for (String dataType : typesToNormalize) {
                DataTypedef dataTypedef = BaseOntology.getOntology().getDataType(dataType);
                String column = entityDescriptor.getPropertyDescriptors().get(dataType).getName();
                String dataValue = null;
                switch (dataTypedef.value()) {
                  case string:
                      dataValue = "d.string";
                      break;
                  case text:
                      dataValue = "d.text";
                      break;
                  case number:
                      dataValue = "d.num";
                      break;
                  case item:
                      column = column + "_id";
                      dataValue = "d.item_id";
                      break;
                  case bool:
                      dataValue = "(d.num <> 0)";
                      break;
                  case time:
                      dataValue = "(TIMESTAMP WITH TIME ZONE 'epoch' + d.num / 1000 * INTERVAL '1 second')";
                      break;
                }
                Query update = getEntityManager().createNativeQuery("UPDATE " + table + " f SET " + column
                        + " = " + dataValue + " FROM Data d WHERE f." + column + " IS NULL"
                        + " AND d.owner_id = f.id"
                        + " AND d.type_name = :dataType");
                update.setParameter("dataType", dataType);
                mapped += update.executeUpdate();
                if (!dataTypedef.global()) {
                    Query delete = getEntityManager().createNativeQuery("DELETE FROM Data WHERE"
                            + " type_name = :dataType"
                            + " AND owner_id IN (SELECT id FROM " + table + ")");
                    delete.setParameter("dataType", dataType);
                    deleted += delete.executeUpdate();
                }
            }
        }
        logger.log(Level.INFO, "Normalization completed, {0}, {1}, {2}", new Object[]{itemType, mapped, deleted});
        getEntityManager().flush();
        getEntityManager().clear();
    }

    private EntityManager getEntityManager() {
        return ManagedUtils.getEntityContext().getEntityManager();
    }
}
