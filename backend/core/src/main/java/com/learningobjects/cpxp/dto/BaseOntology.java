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
import com.learningobjects.cpxp.entity.EntityUtils;
import com.learningobjects.cpxp.entity.IndexType;
import com.learningobjects.cpxp.entity.annotation.*;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataMapping;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemTypedef;
import com.learningobjects.cpxp.util.ParallelStartup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import io.github.classgraph.ScanResult;
import scala.Tuple2;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Ontology describing the known item and data types.
 */
public class BaseOntology implements Ontology {
    private static final Logger logger = Logger.getLogger(BaseOntology.class.getName());

    private static BaseOntology __ontology;

    private final ScanResult _classGraph;

    private final Map<String, ItemTypedef> _itemTypes = new ConcurrentHashMap<>();
    private final Map<String, DataTypedef> _dataTypes = new ConcurrentHashMap<>();

    private final Map<String, ItemTypedef> _entityItemTypes = new ConcurrentHashMap<>();
    private final Map<String, DataTypedef> _entityDataTypes = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> _entityClasses = new ConcurrentHashMap<>();

    private final Map<String, ItemTypedef> _constantItemTypes = new ConcurrentHashMap<>();
    private final Map<String, DataTypedef> _constantDataTypes = new ConcurrentHashMap<>();

    private final Map<String, EntityDescriptor> _entityDescriptors = new ConcurrentHashMap<>();
    private final Map<String, EntityDescription> _entityDescriptions = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> _itemTypeByFinder = new ConcurrentHashMap<>();

    /**
     * Private constructor.
     */
    private BaseOntology(ScanResult reflections) {
        _classGraph = reflections;
    }

    /**
     * Initialize the ontology.
     */
    public static synchronized Ontology initOntology(ScanResult reflections) {
        BaseOntology ontology = new BaseOntology(reflections);
        ontology.analyzeClasses();
        __ontology = ontology;
        return ontology;
    }

    /**
     * Get the current ontology. The intent is to evolve this such that the
     * current filter will install the ontology of the current domain in
     * thread-local storage.
     */
    public static synchronized Ontology getOntology() {
        if (__ontology == null) {
            throw new IllegalStateException("Ontology not initialized");
        }
        return __ontology;
    }

    /**
     * Get whether a value is a know item type.
     * @param itemType the value
     * @return whether it is a known item type
     */
    @Override
    public boolean isItemType(String itemType) {
        return _itemTypes.containsKey(itemType);
    }

    /**
     * Get the known item types.
     *
     * @return a map from item type names to item type definitions
     */
    @Override
    public Map<String, ItemTypedef> getItemTypes() {
        return Collections.unmodifiableMap(_itemTypes);
    }

    /**
     * Get a particular item type.
     *
     * @param typeName
     *            the item type name
     *
     * @return the item type definition
     */
    @Override
    public ItemTypedef getItemType(String typeName) {
        ItemTypedef typedef = _itemTypes.get(typeName);
        if (typedef == null) {
            throw new IllegalArgumentException(
              "Unknown item type: " + typeName);
        }
        return typedef;
    }

    /**
     * Get the known data types.
     *
     * @return a map from strings to data type definitions
     */
    @Override
    public Map<String, DataTypedef> getDataTypes() {
        return Collections.unmodifiableMap(_dataTypes);
    }

    /**
     * Get a particular data type.
     *
     * @param typeName
     *            the data type name
     *
     * @return the data type definition
     */
    @Override
    public DataTypedef getDataType(String typeName) {
        DataTypedef typedef = _dataTypes.get(typeName);
        if (typedef == null) {
            throw new IllegalArgumentException(
              "Unknown data type: " + typeName);
        }
        return typedef;
    }

    @Override
    public boolean isStandalone(String itemType) {
        if (_itemTypes.containsKey(itemType)) {
            return !_itemTypes.get(itemType).itemRelation().isPeered();
        } else {
            return false;
        }
    }

    @Override
    public DataFormat getDataFormat(String typeName) {
        if (typeName.startsWith("#")) {
            return DataFormat.item;
        } else {
            return getDataType(typeName).value();
        }
    }

    @Override
    public Map<String, EntityDescriptor> getEntityDescriptors() {
        return Collections.unmodifiableMap(_entityDescriptors);
    }

    @Override
    public EntityDescriptor getEntityDescriptor(String itemType) {
        return itemType == null ? null : _entityDescriptors.get(itemType);
    }

    @Override
    public EntityDescription getEntityDescription(String itemType) {
        return itemType == null ? null : _entityDescriptions.get(itemType);
    }

    @Override
    public String getItemTypeForFinder(Class<?> finderType) {
        String type = _itemTypeByFinder.get(finderType);
        if (type == null) {
            type = _itemTypeByFinder.get(finderType.getSuperclass());
        }
        return type;
    }

    /**
     * Analyze the service package for all item and data type definitions.
     */
    private void analyzeClasses() {
        var entityTypes = _classGraph.getClassesWithAnnotation(Entity.class).loadClasses();
        var itemTypeFields = new ArrayList<Field>();
        _classGraph.getClassesWithFieldAnnotation(ItemTypedef.class).forEach(ci ->
            ci.getFieldInfo().forEach(fi -> {
                if (fi.hasAnnotation(ItemTypedef.class)) {
                    itemTypeFields.add(fi.loadClassAndGetField());
                }
            })
        );
        var dataTypeFields = new ArrayList<Field>();
        _classGraph.getClassesWithFieldAnnotation(DataTypedef.class).forEach(ci ->
            ci.getFieldInfo().forEach(fi -> {
                if (fi.hasAnnotation(DataTypedef.class)) {
                    dataTypeFields.add(fi.loadClassAndGetField());
                }
            })
        );

        logger.info("entity types: " + entityTypes.size() + ", item typedefs: " + itemTypeFields.size() + ", data typedefs; " + dataTypeFields.size());

        ParallelStartup.foreach(entityTypes, this::analyzeClass);

        ParallelStartup.foreach(itemTypeFields,
          field -> mapMetadata(ItemTypedef.class, _constantItemTypes, field));

        ParallelStartup.foreach(dataTypeFields,
          field -> mapMetadata(DataTypedef.class, _constantDataTypes, field));

        updateMaps();
    }

    @Override
    public void updateMaps() {
        // Combine entity and constant field maps into one, with constant fields having precedence.
        _itemTypes.clear();
        _itemTypes.putAll(_entityItemTypes);
        _itemTypes.putAll(_constantItemTypes);

        _dataTypes.clear();
        _dataTypes.putAll(_entityDataTypes);
        _dataTypes.putAll(_constantDataTypes);

        // map the entities in a second pass so that all of the data types
        // will be available

        ParallelStartup.foreach(_itemTypes.entrySet(), entityEntry -> {
            mapEntity(entityEntry.getKey(), entityEntry.getValue());
        });

        logger.info("item types: " + _itemTypes.size() + ", data types: " + _dataTypes.size() + ", entity classes: " + _entityClasses.size() + ", entity descriptors: " + _entityDescriptors.size());
    }

    @Override
    public void analyzeClass(Class<?> clas) {
        try {
            logger.log(Level.INFO, "Analyzing entity, {0}", clas.getName());
            //Pull Meta information from the entity first
            final String itemType = EntityUtils.getItemType(clas);

            if (Finder.class.isAssignableFrom(clas)) {
                final ItemRelation itemRelation = getItemRelation(clas);
                Collection<DataTypedef> dataTypes = new ArrayList<>();
                Collection<DataMapping> dataMappings = new ArrayList<>();
                String friendlyName = "";
                for (Field field : clas.getDeclaredFields()) {
                    mapEntityField(field, itemType, dataTypes, dataMappings);
                    if (field.isAnnotationPresent(FriendlyName.class)) {
                        friendlyName = getDataType(field, itemType);
                    }
                }
                final SqlIndex[] sqlIndices = Optional.ofNullable(clas.getAnnotation(SqlIndex.class))
                  .map(i -> new SqlIndex[] { i })
                  .orElse(Optional.ofNullable(clas.getAnnotation(SqlIndices.class))
                      .map(SqlIndices::value)
                      .orElse(NO_SQL_INDICES)
                  );
                final ItemTypedef newItemDef = newItemDef(dataMappings, itemRelation, friendlyName, sqlIndices);
                _entityItemTypes.put(itemType, newItemDef);
                _entityClasses.put(itemType, clas);
            } else if (!Data.class.isAssignableFrom(clas) && !Item.class.isAssignableFrom(clas)) {
                var entity = clas.getAnnotation(Entity.class);
                var entityName = entity.name().isEmpty() ? clas.getSimpleName() : entity.name();
                var table = Optional.ofNullable(clas.getAnnotation(Table.class));
                var tableName = table.map(Table::name).filter(n -> !n.isEmpty()).orElse(entityName);
                var properties = BeanIntrospector.propertyDescriptorsJava(clas);

                var columnProps = Arrays.stream(clas.getDeclaredFields())
                  .filter(field -> field.isAnnotationPresent(Column.class))
                  .map(field -> {
                      var property = properties.get(field.getName());
                      if (property == null) {
                          throw new RuntimeException("@Column " + field.getName() + " does not form a JavaBean property");
                      }
                      var dataFormat = inferDataFormat(field);
                      var dataType = itemType + "." + field.getName();
                      var newDef = newDataTypeDef(dataFormat, "", null);
                      _entityDataTypes.put(dataType, newDef);
                      return Tuple2.apply(dataType, property);
                  }).collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

                var description = EntityDescription.fromJava(itemType, entityName, tableName, Optional.empty(), columnProps);
                _entityDescriptions.put(itemType, description);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error analyzing entity: "
              + clas.getName(), e);
        }
    }

    private static ItemRelation getItemRelation(final Class<?> clas) {
        return Arrays.stream(ItemRelation.values())
          .filter(rel -> rel.getBaseClass().isAssignableFrom(clas))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Cannot infer the item relation of " + clas.getName() + " from its type"));
    }

    private void mapEntityField(Field field, final String ownerType, Collection<DataTypedef> dataTypes, Collection<DataMapping> dataMappings) {
        if (field.isAnnotationPresent(Transient.class)) {
            return;
        }
        final Optional<FinderDataDef> finderDataDef = Optional.ofNullable(field.getAnnotation(FinderDataDef.class));
        final Class<?> fieldType = field.getType();
        final String dataType = getDataType(field, ownerType);
        final DataFormat dataFormat = finderDataDef.map(FinderDataDef::value).orElseGet(() -> inferDataFormat(field));
        final String targetType = Finder.class.isAssignableFrom(fieldType) ? EntityUtils.getItemType((Class<? extends Finder>) fieldType) : "";
        final String mappedName = field.isAnnotationPresent(DataType.class) ? field.getName() : null;
        final DataTypedef newDef = newDataTypeDef(dataFormat, targetType, mappedName);
        // TODO: When we drop DataMapping from ItemTypedef then we can switch to storing a map to the FunctionalIndex annotations instead of doing this adaption
        final Optional<FunctionalIndex> functionalIndex = Optional.ofNullable(field.getAnnotation(FunctionalIndex.class));
        final IndexType indexType = functionalIndex.map(FunctionalIndex::function).orElse(IndexType.NONE);
        final boolean byParent = functionalIndex.map(FunctionalIndex::byParent).orElse(false);
        final boolean nonDeleted = functionalIndex.map(FunctionalIndex::nonDeleted).orElse(false);
        dataTypes.add(newDef);
        _entityDataTypes.put(dataType, newDef);
        final DataMapping newMapping = newDataMapping(dataType, indexType, byParent, nonDeleted);
        dataMappings.add(newMapping);
    }

    private static String getDataType(final Field field, final String ownerType) {
        final Optional<DataType> dataDef = Optional.ofNullable(field.getAnnotation(DataType.class));
        return dataDef.map(DataType::value).orElseGet(() -> ownerType + "." + field.getName());
    }

    private static DataFormat inferDataFormat(Field field) {
        final Class<?> clas = field.getType();
        if (String.class.equals(clas)) { // path, s3json, tsvector
            return isClob(field) ? DataFormat.text : DataFormat.string;
        } else if (Finder.class.isAssignableFrom(clas)) {
            return DataFormat.item;
        } else if (clas.isEnum() || clas.isArray()) {
            return DataFormat.string;
        } else {
            final DataFormat fmt = TYPE_FORMATS.get(clas);
            if (fmt == null) {
                throw new IllegalStateException("Cannot infer data format for field " + field.toString() + " of type " + clas.getName());
            }
            return fmt;
        }
    }

    // Registry of known types to the corresponding data formats
    private static final Map<Class<?>, DataFormat> TYPE_FORMATS = new HashMap<>();

    static {
        TYPE_FORMATS.put(Date.class, DataFormat.time);
        TYPE_FORMATS.put(Boolean.class, DataFormat.bool);
        TYPE_FORMATS.put(Double.class, DataFormat.DOUBLE);
        TYPE_FORMATS.put(Item.class, DataFormat.item);
        TYPE_FORMATS.put(JsonNode.class, DataFormat.json);
        TYPE_FORMATS.put(Long.class, DataFormat.number);
        TYPE_FORMATS.put(Json.class, DataFormat.json);
        TYPE_FORMATS.put(Integer.class, DataFormat.number);
        TYPE_FORMATS.put(String.class, DataFormat.string);
        TYPE_FORMATS.put(LocalDate.class, DataFormat.time);
        TYPE_FORMATS.put(UUID.class, DataFormat.uuid);
    }

    // Column definition used for CLOBs
    private static final String CLOB_DEFINITION = "TEXT";

    private static boolean isClob(AnnotatedElement el) {
        final Column col = el.getAnnotation(Column.class);
        return (col != null) && CLOB_DEFINITION.equals(col.columnDefinition());
    }

    private void mapEntity(String itemType, ItemTypedef itemTypedef) {
        DataMapping[] mappings = itemTypedef.dataMappings();

        if (mappings.length == 0) {
            return;
        }

        try {
            //noinspection unchecked
            Class<? extends Finder> clas = (Class<? extends Finder>) _entityClasses.get(itemType);
            if (clas == null) {
                throw new Exception("Could not find entity class for item type: " + itemType);
            }
            EntityDescriptor entityDescriptor = new BaseEntityDescriptor(clas, itemType,
                    itemTypedef.dataMappings(), itemTypedef.itemRelation(), itemTypedef.sqlIndexes(), _dataTypes);
            _entityDescriptors.put(itemType, entityDescriptor);
            _entityDescriptions.put(itemType, entityDescriptor.toDescription());
            _itemTypeByFinder.put(entityDescriptor.getEntityType(), itemType);
        } catch (Exception e) {
            throw new IllegalStateException("While mapping " + itemType, e);
        }
    }

    private static <T extends Annotation> void mapMetadata(Class<T> typedef, Map<String, T> typedefMap,
                                 Field field) {
        try {
            T annotation = typedef.cast(field.getAnnotation(typedef));
            Object constantValue = field.get(null);

            if (!(constantValue instanceof String)) {
                logger.log(Level.WARNING, "Value of constant, {0}, is not a String.", field.getName());
                return;
            }

            String name = (String) constantValue;
            logger.log(Level.FINE, "Found type, {0}, {1}", new Object[]{typedef.getSimpleName(), name});
            typedefMap.put(name, typedef.cast(annotation));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static DataTypedef newDataTypeDef(final DataFormat dataFormat, final String itemType, final String mappedName) {
        return new DataTypedef() {
            @Override
            public DataFormat value() {
                return dataFormat;
            }

            @Override
            public boolean global() {
                return false;
            }

            @Override
            public String mappedName() {
                return mappedName;
            }

            @Override
            public String itemType() {
                return itemType;
            }

            @Override
            public int length() {
                return -1;
            }

            @Override
            public boolean nullable() {
                return false;
            }


            @Override
            public boolean entityOnly() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return DataTypedef.class;
            }
        };
    }

    private static ItemTypedef newItemDef(final Collection<DataMapping> dataMappings, final ItemRelation itemRelation, final String friendlyName, final SqlIndex[] sqlIndices) {
        return new ItemTypedef() {
            @Override
            public DataMapping[] dataMappings() {
                return dataMappings.toArray(new DataMapping[dataMappings.size()]);
            }

            @Override
            public ItemRelation itemRelation() {
                return itemRelation;
            }

            @Override
            public SqlIndex[] sqlIndexes() {
                return sqlIndices;
            }

            @Override
            public String friendlyName() {
                return friendlyName;
            }

            @Override
            public boolean noData() {
                return false;
            }

            @Override
            public boolean l2Cache() { return true; }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ItemTypedef.class;
            }
        };
    }

    private static final SqlIndex[] NO_SQL_INDICES = new SqlIndex[0];

    private static DataMapping newDataMapping(final String dataType, final IndexType indexType, final boolean byParent, final boolean nonDeleted) {
        return new DataMapping() {
            @Override
            public String type() {
                return dataType;
            }

            @Override
            public IndexType indexType() {
                return indexType;
            }

            @Override
            public boolean byParent() {
                return byParent;
            }

            @Override
            public boolean nonDeleted() {
                return nonDeleted;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return DataMapping.class;
            }
        };
    }
}
