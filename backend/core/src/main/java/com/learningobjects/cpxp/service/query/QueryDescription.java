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

package com.learningobjects.cpxp.service.query;

import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.EntityDescription;
import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.persistence.Query;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.logging.Logger;

/**
 * Helper class to hold declarative objects used to generate a query language
 * statement.
 */
// TODO: switch to a more efficient container than ConditionDTO
class QueryDescription {

    /**
     * Rather than being privy to all the details of AbstractQueryBuilders, QueryDescription only needs to
     * know certain things. TODO add more interfaces to remove dependency on AbstractQueryBuilder.
     */
    public static interface SubQuery {
        public String getCacheKey();
        public String buildExistsQuery(String field);
        QueryDescription getDescription();
        public Map<String, Object> setParameters(Query query);
        public String buildSubquery();
        public Set<String> getInvalidationKeys();
    }

    private static final Logger logger = Logger
            .getLogger(QueryDescription.class.getName());

    final Ontology _ontology = BaseOntology.getOntology();

    // A conjunction of disjunction of conjunction
    final DisjunctionSeries _disjunctions;

    final Conjunction _conditions;

    // In a join query, the order of the orders matter so we have to maintain this explicitly
    final List<Pair<QueryDescription, Order>> _orderSequence = new ArrayList<>();

    final List<Order> _orders = new ArrayList<>();

    final List<SubQuery> _subqueries = new ArrayList<>();

    private final Map<String, Integer> _dataIndices = new HashMap<String, Integer>();

    Item _root;

    boolean _rootImplicit, _joinRoot;

    Item _parent;

    Item _item;

    boolean _parentImplicit;

    List<SubQuery> _initialQueries = new ArrayList<>();

    List<SubQuery> _existsQueries = new ArrayList<>();

    List<Join> _joinQueries = new LinkedList<>();

    String _itemType;

    boolean _itemTypeImplicit;

    boolean _includeDeleted = false, _joinDeleted = false;

    String _path;

    EntityDescription _entityDescription;

    List<String> _groupType;

    boolean _excludePath;

    boolean _inclusive = true;

    int _limit = -1;

    int _firstResult = -1;

    Projection _projection = Projection.ITEM;

    DataProjection[] _dataProjection;

    Long _itemContext;

    Date _calendarStart, _calendarEnd;

    AbstractQueryBuilder _aggregateQuery;

    Function _function;

    boolean _distinct;

    boolean _deny;

    String _itemLabel = "i";

    String _parentLabel = "p";

    String _degenerateItemLabel = "di";

    String _pathLabel = "h";

    String _typeLabel = "t";

    String _dataLabel = "d";

    String _valueLabel = "v";

    boolean _orderByQuery = false;

    private boolean _forceNative;

    QueryDescription() {
        _conditions = new Conjunction(this);
        _disjunctions = new DisjunctionSeries(this);
    }

    void setRoot(Item root, boolean implicit) {
        _root = root;
        _rootImplicit = implicit;
    }

    // value for binding, conditioned on native or EJB-QL query
    Object getRootForBinding() {
        return nativeQuery() ? _root.getId() : _root;
    }

    void setParent(Item parent, boolean implicit) {
        _parent = parent;
        _parentImplicit = implicit;
        setRoot(_parent.getRoot(), true);
    }


    void setItem(Item item) {
        _item = item;
        setRoot(item.getRoot(), true);
    }

    // value for binding, conditioned on native or EJB-QL query
    Object getParentforBinding() {
        return nativeQuery() ? _parent.getId() : _parent;
    }

    Object getItemForBinding() {
        return _item.getId();
    }

    void addInitialQuery(QueryBuilder initialQuery) {
        SubQuery qb = (SubQuery) initialQuery;
        _initialQueries.add(qb);
        addSubquery(qb);
    }

    void addExistsQuery(QueryBuilder existsQuery) {
        addExistsQuery((SubQuery) existsQuery);
    }

    void addExistsQuery(SubQuery existsQuery){
        _existsQueries.add(existsQuery);
        addSubquery(existsQuery);
    }

    void addJoin(Join join) {
        addDataType(join.leftDataType());
        AbstractQueryBuilder qb = (AbstractQueryBuilder) join.query();
        _joinQueries.add(join);
        addSubquery(qb);
        if (join.leftDataType().startsWith("#")
                || _ontology.getDataType(join.leftDataType()).itemType().isEmpty()
                || !join.rightDataType().equals("#id")) { // can't use HQL to do the join
            forceNative();
        }
        if (!(join instanceof Join.Inner)) {
            QueryDescription outer = join instanceof Join.Left ?
              ((AbstractQueryBuilder) join.query())._description : this;

            /* If we would be explicitly including the root as a WHERE condition,
             * we will include it in the ON instead. */
            if ((outer._root != null) && !outer._rootImplicit) {
                outer._joinRoot = true;
                outer._rootImplicit = true;
            }

            if (!outer._includeDeleted && outer.softDeletable()) {
                outer._joinDeleted = true;
            }

            if (outer._entityDescription == null && join.conditions().value().nonEmpty()) {
                forceNative();
            }

            /* data-mapped conditions can't be checked by jpaql */
            for (List<Condition> conj : join.conditions().asJava()) {
                for (Condition cond : conj) {
                    if (cond.getDataType().startsWith("#")
                      || !outer._entityDescription.containsDataType(cond.getDataType())) {
                        forceNative();
                        break;
                    }
                }
            }
        }
    }

    private void addSubquery(SubQuery qb) {
        if (qb == null) {
            return;
        }
        final QueryDescription description = qb.getDescription();
        if ((_root == null) && (description._root != null)) {
            setRoot(description._root, true);
        }
        description.setSubqueryIndex(_subqueries.size());
        _subqueries.add(qb);
        if (description.nativeQuery()) {
            forceNative();
        } else if (_forceNative) {
            description.forceNative();
        }
    }

    void setItemType(String itemType, boolean implicit) {
        _itemType = itemType;
        _itemTypeImplicit = implicit;
        _entityDescription = _ontology.getEntityDescription(_itemType);
    }

    void setIncludeDeleted(boolean includeDeleted) {
        _includeDeleted = includeDeleted;
    }

    void setPath(String path) {
        _path = path;
    }

    void addDisjunction(List<List<Condition>> conditions) {
        _disjunctions.addDisjunction(conditions);
        for (List<Condition> list : conditions) {
            for (Condition condition : list) {
                checkCondition(condition);
            }
        }
    }

    void addCondition(Condition condition) {
        addDataType(condition.getDataType());
        _conditions.addCondition(condition);
        checkCondition(condition);
    }

    private void checkCondition(Condition condition) {
        if (condition.getQuery() != null) {
            addSubquery((SubQuery) condition.getQuery());
        }
        if ((condition.getField() != null) || condition.getComparison().forcesNative()) {
            forceNative();
        }
    }

    void setGroup(String type) {
        // TODO handle removal correctly and cleanly
        addDataType(type);
        if (_groupType == null) {
            _groupType = new ArrayList<String>();
        }
        _groupType.add(type);
    }

    void setExcludePath(boolean exclude) {
        _excludePath = exclude;
    }

    void setInclusive(boolean inclusive) {
        _inclusive = inclusive;
    }

    void clearOrder() {
        _orderSequence.clear();
        _orders.clear();
    }

    void addOrder(AbstractQueryBuilder qb, Order order) {
        _orderSequence.add(Pair.of(qb._description, order));
        qb._description.addOrder(order);
    }

    void addOrder(Order order) {
        _orders.add(order);
        addDataType(order.getType());
        addSubquery((SubQuery)order.getQuery());
        if (order.getJsonField() != null) {
            forceNative();
        }
    }

    void setLimit(final Integer limit) {
        _limit = null == limit ? -1 : limit;
    }

    void setLimit(int limit) {
        _limit = limit;
    }

    void setFirstResult(final Integer firstResult) {
        _firstResult = null == firstResult ? -1 : firstResult;
    }

    void setFirstResult(int firstResult) {
        _firstResult = firstResult;
    }

    void setProjection(Projection projection, Object[] parameters) {
        assert projection != null : "Projection must be valid.";
        _projection = projection;
        switch (projection) {
        case DATA:
        case AGGREGATE_ITEM_WITH_DATA:
            _dataProjection = new DataProjection[parameters.length];
            for (int i=0; i<parameters.length; i++) {
                if (parameters[i] instanceof String) {
                    _dataProjection[i] = BaseDataProjection.ofDatum((String) parameters[i]);
                    addDataType(_dataProjection[i].getType());
                } else if (parameters[i] instanceof DataProjection) {
                    DataProjection dp =  (DataProjection) parameters[i];
                    _dataProjection[i] = dp;
                    addDataType(dp.getType());
                    if (dp.getJsonField() != null) {
                        forceNative();
                    }
                } else {
                    throw new IllegalArgumentException("Can't convert parameter["+i+"] to a String");
                }
            }
            break;
        case ITEM_CONTEXT:
            _itemContext = (parameters[0] instanceof Item) ? ((Item) parameters[0]).getId()
                    : (Long) parameters[0];
            break;
        case CALENDAR_INFO:
            _calendarStart = (Date) parameters[0];
            _calendarEnd = (Date) parameters[1];
            break;
        case ITEM_WITH_AGGREGATE:
            _aggregateQuery = (AbstractQueryBuilder) parameters[0];
            break;
        }
    }

    void setFunction(Function function) {
        _function = function;
    }

    void setDistinct(boolean distinct) {
        _distinct = distinct;
    }

    AbstractQueryBuilder _having;
    Comparison _havingCmp;
    Number _havingValue;

    void having(QueryBuilder qb, Comparison cmp, Number value) {
        _having = (AbstractQueryBuilder) qb;
        _havingCmp = cmp;
        _havingValue = value;
    }

    int setSubqueryIndex(int index) {
        char c = (char) ('A' + (index++));
        _valueLabel = "v" + c;
        _typeLabel = "t" + c;
        _dataLabel = "d" + c;
        _itemLabel = "i" + c;
        _parentLabel = "p" + c;
        _pathLabel = "p" + c;
        for (QueryDescription.SubQuery qb : _subqueries) {
            index = qb.getDescription().setSubqueryIndex(index);
        }
        return index;
    }

    String getProjection(boolean isSubquery) {
        return getProjection(isSubquery, false);
    }

    String getProjection(boolean isSubquery, boolean forceId) {
        String field = null;
        switch (_projection) {
        case ID:
            field = _itemLabel + ".id";
            break;
        case ITEM:
            /* if it's a peered finderized item type, then we are a finder and should use .owner
             * otherwise we're either a standalone finder or an unknown item and should use ourselves
             * TODO: what happens when we're peered but the pertinent data type of the owning query points to our finder? */
            field = (isSubquery && _entityDescription != null && _entityDescription.isPeered())
                    ? itemFieldItself("owner")
                    : itemItself();
            break;
        case PARENT_ID:
            field = itemFieldIdentity("parent");
            break;
        case PARENT:
            field = itemFieldItself("parent");
            break;
        case ROOT_ID:
            field = itemFieldIdentity("root");
            break;
        case ROOT:
            field = itemFieldItself("root");
            break;
        case DATA: {
            // In a subquery I can't follow the default data behaviour
            // of just returning the id
            List<String> projections = new ArrayList<>();
            for (DataProjection dataProjection : _dataProjection) {
                String dfield = getEntityField(dataProjection.getType(), isSubquery);
                if (forceId) dfield = dfield + ".id";
                if (dataProjection.getJsonField() != null) {
                    dfield = dfield + "->>'" + dataProjection.getJsonField() + "'"; // forces to string ..
                }
                if (dataProjection.getFunction() != null) {
                    dfield = dataProjection.getFunction().toSQL(dfield);
                }
                if (_forceNative) {
                    // if we're selecting the "name" field from multiple relations, a native
                    // query will barf unless we disambiguate, even though we only care about
                    // positional results and not named results.
                    dfield = dfield + " AS " + _itemLabel + "_v" + projections.size();
                }
                projections.add(dfield);
            }
            field = StringUtils.join(projections, ", ");
            break;
        }
        case AGGREGATE_ITEM_WITH_DATA:
            field = _itemLabel + ".id";
            break;
        case ITEM_CONTEXT:
            // is native only
            field = _itemLabel + ".id";
            break;
        case CALENDAR_INFO:
            // is native only
            field = getEntityField(_orders.get(0).getType(), false);
            break;
        case ITEM_WITH_AGGREGATE:
            // is native only
            field = _itemLabel + ".id"+ " AS " + _itemLabel + "_id";
            break;
        }
        if (_distinct) {
            field = "DISTINCT " + field;
        }
        if (_function != null) {
            field = _function.toSQL(field);
        }
        if (_groupType != null) {
            for (String groupType : _groupType) {
                field = appendToField(field, getEntityField(groupType, false));
            }
        }
        // This is an abomination
        if (_projection == Projection.AGGREGATE_ITEM_WITH_DATA) {
            for (DataProjection dataProjection : _dataProjection) {
                field = appendToField(field, getEntityField(dataProjection.getType(), false));
            }
        } else if (_projection == Projection.ITEM_WITH_AGGREGATE) {
            // TODO: Query plan to verify that this performs okay given that i
            // order by COALESCE(AVG, 0) but then just return AVG here. Don't
            // want that computation done twice.
            field = appendToField(field, _aggregateQuery._description.getProjection(true));
        }

        // This is a hack for AssessmentReportingRoot. I should support
        // functions bound to individual projections and just straight
        // out support multiple projections. This would kill the peculiar
        // multiple-projections above. This comment now makes no sense
        // because we do, and even less sense because that file does not
        // even exist anymore!
        for (Join join : _joinQueries) {
            AbstractQueryBuilder jq = ((AbstractQueryBuilder) join.query());
            String joinProjection = jq._description.getProjection(isSubquery);
            // the projection would just be ITEM if we are doing an order by the subquery but not a projection
            if (jq._description._projection != Projection.ITEM && StringUtils.isNotEmpty(joinProjection)) {
                if (StringUtils.isEmpty(field)) {
                    field = joinProjection;
                } else {
                    field = appendToField(field, joinProjection);
                }
            }
        }

        return field;
    }

    private String appendToField(String field, String s) {
        // Prevents "SELECT , iA.col1, ib.col1 ..."
        if (StringUtils.isBlank(field)) {
            return s;
        }
        // Prevents duplicate alias errors
        return (!StringUtils.contains(field, s)) ? field + ", " + s : field;
    }

    boolean isItemProjection() {
        return (_function == null)
            && ((_projection == Projection.ITEM) || (_projection == Projection.PARENT));
    }

    void addDataType(String type) {
        if ((type == null) || type.startsWith("#")) {
            return;
        }
        // Implicitly requires the item type to be set before the data
        // projection but is error safe; will fail in dev if wrong
        // FIXME this assertion trips in otherwise correct functioning code
//        assert (_itemType != null) || _ontology.getDataType(type).global() : String.format("Data types used in non item-type-specific queries must be flagged as global.  Type: %1$s.", type);

        if (inDataCollection(type)) {
            Integer index = _dataIndices.get(type);
            if (index == null) {
                index = _dataIndices.size();
                _dataIndices.put(type, index);
            }
        }
    }

    String getEntityField(String dataType, boolean isSubquery) {
        switch (dataType) {
          case DataTypes.META_DATA_TYPE_ID:
              return _itemLabel + ".id";
          case DataTypes.META_DATA_TYPE_PARENT:
              return itemFieldItself("parent");
          case DataTypes.META_DATA_TYPE_PARENT_ID:
              return itemFieldIdentity("parent");
          case DataTypes.META_DATA_TYPE_ROOT:
              return itemFieldItself("root");
          case DataTypes.META_DATA_TYPE_ROOT_ID:
              return itemFieldIdentity("root");
          default:
              return getAlias(dataType) + '.' + getField(dataType, isSubquery);
        }
    }

    Set<String> getDataTypes() {
        return _dataIndices.keySet();
    }

    String getAlias(String dataType) {
        if (inDataCollection(dataType)) {
            return _dataLabel + _dataIndices.get(dataType);
        }
        return _itemLabel;
    }

    String getField(String dataType) {
        return getField(dataType, false);
    }

    // TODO: FIXME: This is inconsistent about that _itemLabel.prefix - in no
    // other cases does this prefix the field by its owning entity
    String getField(String dataType, boolean valueIsNull) {
        switch (dataType) {
          case DataTypes.META_DATA_TYPE_ID:
              return _itemLabel + ".id";
          case DataTypes.META_DATA_TYPE_PARENT:
              return itemFieldItself("parent");
          case DataTypes.META_DATA_TYPE_PARENT_ID:
              return itemFieldIdentity("parent");
          case DataTypes.META_DATA_TYPE_ROOT:
              return itemFieldItself("root");
          case DataTypes.META_DATA_TYPE_ROOT_ID:
              return itemFieldIdentity("root");
        }

        DataFormat format = BaseOntology.getOntology().getDataFormat(dataType);

        if (!inDataCollection(dataType)) {
            String propertyName = _entityDescription.getPropertyName(dataType);

            if (format == DataFormat.item) {
                return nativeQuery() ? propertyName.concat("_id")
                    : valueIsNull ? propertyName
                    : propertyName.concat(".id");
            }

            return propertyName;
        }

        switch (format) {
        case string:
        case path:
        case DOUBLE: // string equality bogosity...
            return "string";
        case text:
        case json:
            return "text";
        case number:
        case time:
        case bool:
            return nativeQuery() ? "num" : "number";
        case item:
            return nativeQuery() ? "item_id"
                : valueIsNull ? "item"
                : "item.id";
        default:
            return null;
        }
    }

    boolean inDataCollection(String dataType) {
        return (_entityDescription == null)
                || !_entityDescription.containsDataType(dataType);
    }

    void applyConditions(StringBuilder buffer, ConjunctionBuilder builder,
            QueryParameterHandler handler) {
        QuerySyntax.append(!_conditions.isEmpty(), buffer, QuerySyntax.AND);
        applyConjunction(_conditions, buffer, null, builder, handler);
    }

    void applyConjunction(Conjunction conjunction, StringBuilder buffer,
            String stub, ConjunctionBuilder builder,
            QueryParameterHandler handler) {
        buffer.append(builder.build(stub, 0, conjunction, handler));
    }

    boolean isDataRewriteNeeded() {
        String dataProjection = ((_projection == Projection.DATA) && (_dataProjection.length == 1))
            ? _dataProjection[0].getType() : null;
        DataFormat format = (dataProjection == null) ? null :
            BaseOntology.getOntology().getDataFormat(dataProjection);
        // Entities store dates and booleans as such in the database
        // whereas data sticks them in a Long. So in order to return
        // consistent results whether in entity or data, we need
        // to convert these data values to java types.
        return (dataProjection != null) && // data projected
                ((_function == null) || !_function.isAggregate()) && // not count
                ((format == DataFormat.time) || (format == DataFormat.bool)) // relevant type
                && inDataCollection(dataProjection); // stored in data
    }

    boolean isFinderRewriteNeeded() {
        // The relation between Finders and Items is one way, now, so to get a
        // list of items, the list of finders needs to be manually "pivoted"; it
        // cannot be done in the database in SQL or in Hibernate with WJB-QL

        // no projection on specific data type, no count and it uses a finder
        return ((_projection == Projection.ITEM) || (_projection == Projection.ITEM_WITH_AGGREGATE))
                && (_function == null) && (_entityDescription != null);
    }

    boolean nativeQuery() {
        return _forceNative || (_projection != null && _projection.nativeQuery())
                || _orderByQuery;
    }

    void forceNative() {
        _forceNative = true;
        _subqueries.forEach(sq -> sq.getDescription().forceNative());
    }

    String itemFieldIdentity(String field) {
        return _itemLabel.concat(".").concat(field).concat(nativeQuery() ? "_id" : ".id");
    }

    String itemFieldItself(String field) {
        return _itemLabel.concat(".").concat(field).concat(nativeQuery() ? "_id" : "");
    }

    String itemItself() {
        return nativeQuery() ? _itemLabel.concat(".id") : _itemLabel;
    }

    boolean softDeletable() {
        return (_entityDescription == null) || (!_entityDescription.isDomained());
    }
}
