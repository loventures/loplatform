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

import argonaut.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.entity.annotation.SqlIndex;
import com.learningobjects.cpxp.scala.json.JacksonCodecs$;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataMapping;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.DigestUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.entity.FinderManipulator;
import com.learningobjects.cpxp.util.entity.PropertySupport;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Reflection support code for generated entities discovered by the
 * {@link BaseOntology} class' analysis.
 *
 */
public class BaseEntityDescriptor implements EntityDescriptor {

    private final String _itemType;

    private final List<String> _dataTypes;

    private final Map<String, DataMapping> _indexTypes;

    private final String _entityName;

    private final Class<? extends Finder> _entityClass;

    private final FinderManipulator _entityManipulator;

    private final Map<String, PropertyDescriptor> _propertyDescriptors;

    private final ItemRelation _itemRelation;

    private final SqlIndex[] _sqlIndexes;

    private final boolean _cacheable;

    BaseEntityDescriptor(Class<? extends Finder> entityClass, String itemType,
                         DataMapping[] dataMapping, ItemRelation itemRelation, SqlIndex[] sqlIndexes,
                         Map<String, DataTypedef> allDataTypes) throws IntrospectionException {
        _entityClass = entityClass;
        _itemType = itemType;
        _entityName = Optional.ofNullable(_entityClass.getAnnotation(Table.class)).map(Table::name).filter(StringUtils::isNotEmpty).orElse(entityClass.getSimpleName());
        _itemRelation = itemRelation;
        _sqlIndexes = sqlIndexes;

        _entityManipulator = ManipulatorSynthesis.spin(entityClass);

        _propertyDescriptors = Collections.unmodifiableMap(mapProperties(
                _itemType, dataMapping, allDataTypes));

        // capture a sorted list of the fully qualified data type names for bulk
        // operations against the whole entity
        List<String> dataTypes = new ArrayList<String>(_propertyDescriptors
                .keySet());
        Collections.sort(dataTypes);
        _dataTypes = Collections.unmodifiableList(dataTypes);

        _indexTypes = Collections.unmodifiableMap(mapIndices(
                _propertyDescriptors, dataMapping));

        _cacheable = entityClass.isAnnotationPresent(Cache.class);
    }

    /**
     * @return the type of the generated entity, useful for reflection
     */
    @Override
    public Class<?> getEntityType() {
        return _entityClass;
    }

    /**
     * @return fully qualified name, useful in JQL statements
     */
    @Override
    public String getEntityName() {
        return _entityClass.getName();
    }

    /**
     * @return table name, useful in HQL statements
     */
    @Override
    public String getTableName() {
        return _entityName;
    }

    /**
     * Factory method using reflection to create a new instance of the
     * associated entity type.
     *
     * @return a new instance ready for use with JPA/Hibernate
     */
    @Override
    public Finder newInstance() {
        try {
            return (Finder) _entityClass.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the property descriptors.
     *
     * @return the property descriptors
     */
    @Override
    public Map<String, PropertyDescriptor> getPropertyDescriptors() {
        return _propertyDescriptors;
    }

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
    @Override
    public Object get(Item item, String dataType) {
        checkItemType(item);
        if (!ServiceContext.getContext().loadFinder(item)) {
            return null;
        }
        if (item.getFinder() == null) {
            throw new RuntimeException("Item lacked a finder: " + item.getType() + "/" + item.getId());
        }
        return get(item.getFinder(), dataType);
    }

    // @Override
    public Long getPK(Item item, String dataType) {
        checkItemType(item);
        if (!ServiceContext.getContext().loadFinder(item)) {
            return null;
        }
        if (item.getFinder() == null) {
            throw new RuntimeException("Item lacked a finder: " + item.getType() + "/" + item.getId());
        }
        return (Long) get(item.getFinder(), dataType, true);
    }

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
    @Override
    public Object get(Finder finder, String dataType) {
        return get(finder, dataType, false);
    }

    private Object get(Finder finder, String dataType, boolean pk) {
        checkDataType(dataType);
        PropertyDescriptor descriptor = _propertyDescriptors.get(dataType);
        Method readMethod = descriptor.getReadMethod();
        assert readMethod.getParameterTypes().length == 0 : String
                .format(
                        "Read method for property, %1$s (data type %2$s) should not take any arguments: %3$s.",
                        descriptor.getName(), dataType, Arrays
                                .deepToString(readMethod.getParameterTypes()));
        try {
            checkPropertyType(descriptor.getPropertyType(), dataType);
            checkPropertyType(readMethod.getReturnType(), dataType);
            Object value = _entityManipulator.get(finder, descriptor.getName());
            if (pk && (value != null)) {
                return ((Id) value).getId();
            }
            if (((value instanceof Item) && (((Item) value).getDeleted() != null)) ||
                ((value instanceof Finder) && (((Finder) value).getDel() != null))) {
                value = null;
            }
            loadFinder(value);
            return value;
        } catch (Exception e) {
            Item owner = finder.getOwner();
            throw new IllegalStateException(String.format(
                    "Trying to get %1$s on %2$s for item %3$d.", dataType,
                    _itemType, (owner == null) ? finder.getId() : owner.getId()), e);
        }
    }

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
    @Override
    public void set(Item item, String dataType, Object value) {
        checkItemType(item);
        if (!ServiceContext.getContext().loadFinder(item)) {
            return;
        }
        set(item.getFinder(), dataType, value);
    }

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
    @Override
    public void set(Finder finder, String dataType, Object value) {
        checkDataType(dataType);
        PropertyDescriptor descriptor = _propertyDescriptors.get(dataType);
        Method writeMethod = descriptor.getWriteMethod();
        assert writeMethod.getParameterTypes().length == 1 : String
                .format(
                        "Write method for property, %1$s (data type %2$s) should take only 1 argument: %3$s.",
                        descriptor.getName(), dataType, Arrays
                                .deepToString(writeMethod.getParameterTypes()));
        try {
            if (value instanceof Item) {
                Item item = (Item) value;
                if (!item.getRoot().getId().equals(finder.getRoot().getId())) {
                    throw new Exception("Cross domain set: " + finder.getId() + "/" + item.getId());
                }
                if (Finder.class.isAssignableFrom(descriptor.getPropertyType()) && !ServiceContext.getContext().loadFinder(item)) {
                    throw new Exception("Error loading finder");
                }
            }

            // StorageBrowser always passes `value` of type JsonNode for DateFormat.json data
            Object value1;
            if (Json.class.isAssignableFrom(descriptor.getPropertyType()) && value instanceof JsonNode) {
                value1 = JacksonCodecs$.MODULE$.jsonNodeEnc().apply((JsonNode) value);
            } else {
                value1 = value;
            }

            checkPropertyType(descriptor.getPropertyType(), dataType);
            checkPropertyType(writeMethod.getParameterTypes()[0], dataType);
            _entityManipulator.set(finder, descriptor.getName(), value1);
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Trying to set %1$s on %2$s.", dataType, _itemType), e);
        }
    }

    /**
     * @return policy for creation and association of an {@link Item} with a
     *         {@link Finder}
     */
    @Override
    public ItemRelation getItemRelation() {
        return _itemRelation;
    }

    /**
     * @return sorted, all of the data type names associated with this entity
     */
    @Override
    public List<String> getDataTypes() {
        return _dataTypes;
    }

    /**
     * Useful check to see if a data type is part of the generated entity or
     * needs to be fetched and manipulated as a Data entity.
     *
     * @param name
     *            type name
     * @return whether this description includes the specified type
     */
    @Override
    public boolean containsDataType(String name) {
        return _propertyDescriptors.containsKey(name);
    }

    /**
     * Useful in EJB-QL generation to refer to properties on generated entities
     * correctly.
     *
     * @param name
     *            type name
     * @return property name relative to the owning entity
     */
    @Override
    public String getPropertyName(String name) {
        return _propertyDescriptors.get(name).getName();
    }

    /**
     * @return DDL statements that can be run to index the backing table for the
     *         entity
     */
    @Override
    public List<String> generateIndices() {
        List<String> statements = new LinkedList<String>();

        String tableName = getTableName();

        if(!"AnalyticFinder".equals(tableName)) {
            statements.add(String.format(
              "CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s_root_idx ON %1$s(root_id)", tableName));
        }
        if (!ItemRelation.DOMAIN.equals(_itemRelation)) {
            statements.add(String.format(
              "CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s_parent_idx ON %1$s(parent_id)", tableName));
            statements.add(String.format(
              "CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s_del_nz ON %1$s(del) WHERE del IS NOT NULL", tableName));
            statements.add(String.format(
              "CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s_path_idx ON %1$s(path varchar_pattern_ops)", tableName));
        }

        for (SqlIndex sqlIndex : _sqlIndexes) {
            String definition = sqlIndex.value();
            String md5 = DigestUtils.md5Hex(definition).substring(0, 12).toLowerCase();

            String indexName = StringUtils.isNotBlank(sqlIndex.name()) ? sqlIndex.name()
                                 : String.format("%1$s_sql_%2$s_idx", tableName, md5);
            indexName = StringUtils.substring(indexName, 0, 63);

            statements.add(String.format("CREATE INDEX CONCURRENTLY IF NOT EXISTS %2$s ON %1$s %3$s", tableName, indexName, definition));
        }

        // use the sorted data types for consistency on repeated calls
        for (String dataType : _dataTypes) {
            PropertyDescriptor propertyDescriptor = _propertyDescriptors
                    .get(dataType);

            String propertyName = propertyDescriptor.getName();

            if (!_indexTypes.containsKey(propertyName)) {
                continue;
            }

            DataMapping dataMapping = _indexTypes.get(propertyName);

            if (Item.class.isAssignableFrom(propertyDescriptor.getPropertyType()) ||
                Finder.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                propertyName = propertyName.concat("_id");
            }

            String statement = dataMapping.indexType().getStatement(tableName, propertyName, dataMapping.byParent(), dataMapping.nonDeleted());
            if (!statement.isEmpty()) {
                statements.add(statement);
            }
        }

        return statements;
    }

    @Override
    public EntityDescription toDescription() {
        return EntityDescription.fromJava(_itemType, getEntityName(), getTableName(), Optional.of(_itemRelation), _propertyDescriptors);
    }

    public boolean isCacheable() {
        return _cacheable;
    }

    private void loadFinder(Object value) {
        if ((null == value) || !(value instanceof Item)) {
            return;
        }
        ServiceContext.getContext().loadFinder((Item) value);
    }

    private void checkPropertyType(Class<?> returnType, String dataType) {
        // TODO: Re-enable these.. These don't understand that finder
        // references return a finder, not an Item.
        /* Ontology ontology = BaseOntology.getOntology();
        DataTypedef typeDef = ontology.getDataTypes().get(dataType);
        Class<?> dataClass = typeOf(typeDef.value());

        if (!dataClass.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The property type, %1$s, is not valid for data, %2$s, of type, %3$s.",
                                    returnType.getName(), dataType, typeDef
                                            .value()));
        } */
    }

    private Class<?> typeOf(DataFormat format) {
        switch (format) {
        case bool:
            return Boolean.class;
        case item:
            return Item.class;
        case number:
            return Long.class;
        case string:
        case text:
            return String.class;
        case time:
            return Date.class;
        }

        throw new IllegalArgumentException(String.format(
                "Unsupported format, %1$s.", format));
    }

    private final Map<String, DataMapping> mapIndices(
            Map<String, PropertyDescriptor> propertyDescriptors,
            DataMapping[] dataMapping) {
        Map<String, DataMapping> indexTypes = new HashMap<>();

        for (DataMapping datumMapping : dataMapping) {
            String dataType = datumMapping.type();
            PropertyDescriptor propertyDescriptor = propertyDescriptors
                    .get(dataType);
            String propertyName = propertyDescriptor.getName();
            indexTypes.put(propertyName, datumMapping);
        }

        return indexTypes;
    }

    private final Map<String, PropertyDescriptor> mapProperties(
            String itemType, DataMapping[] dataMapping,
            Map<String, DataTypedef> dataTypes) {
        Map<String, PropertyDescriptor> allDescriptors = BeanIntrospector.propertyDescriptorsJava(_entityClass);

        Map<String, PropertyDescriptor> dataDescriptors = new HashMap<String, PropertyDescriptor>(
                dataMapping.length);

        for (DataMapping datumMapping : dataMapping) {
            String dataType = datumMapping.type();
            DataTypedef dataTypedef = dataTypes.get(dataType);
            String mappedName = dataTypedef.mappedName();
            String dataPropertyName = StringUtils.isEmpty(mappedName) ?
              PropertySupport.getPropertyName(itemType, dataType) : mappedName;

            // this should match the transform that the GF module applies when
            // generating the entities
            assert allDescriptors.containsKey(dataPropertyName) : String
                    .format(
                            "Entity, %1$s, should have a property for data type, %2$s (%3$s); server may need to be re-started",
                            _entityName, datumMapping.type(), dataPropertyName);

            // map to the full data type name, which may have a prefix, usually
            // the simple entity name separated by a dot
            PropertyDescriptor dataDescriptor = allDescriptors
                    .get(dataPropertyName);
            dataDescriptors.put(dataType, dataDescriptor);
        }

        return dataDescriptors;
    }

    private void checkDataType(String dataType) {
        if (_propertyDescriptors.containsKey(dataType)) {
            return;
        }
        throw new IllegalArgumentException(
                String
                        .format(
                                "Entity for item type, %1$s, doesn't have a property for data type, %2$s; check the @ItemTypedef.",
                                _itemType, dataType));
    }

    private void checkItemType(Item item) {
        if (_itemType.equals(item.getType())) {
            return;
        }
        throw new IllegalArgumentException(
                String
                        .format(
                                "This entity, %1$s, support item type, %2$s, not type, %3$s; check the @ItemTypedef.",
                                _entityName, _itemType, item.getType()));
    }
}
