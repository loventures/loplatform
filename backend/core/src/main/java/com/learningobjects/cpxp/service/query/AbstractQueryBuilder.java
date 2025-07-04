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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.DataModel;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.facade.FacadeSupport;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemSupport;
import com.learningobjects.cpxp.util.Ids;
import com.learningobjects.cpxp.util.ObjectUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.persistence.Query;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;
import scala.reflect.ClassTag;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.learningobjects.cpxp.service.query.QueryBuilderPlan.explain;

/**
 * Parent class takes care of the call forwarding to {@link QueryDescription}
 * and caching behavior for its daughters.
 *
 * <strong>Note:</strong> Cached results are only stored for non-global queries,
 * that is queries that use a parent other than root or the from/by/to link
 * semantics.
 *
 * @see QueryCacheKey
 */
// TODO: switch to a more efficient container than ConditionDTO
abstract class AbstractQueryBuilder implements QueryBuilder, QueryDescription.SubQuery {
    private static final Logger logger = Logger
            .getLogger(AbstractQueryBuilder.class.getName());

    private List<String> _invalidationKeys;

    private final QueryCache _qbCache;

    final ServiceContext _serviceContext;

    final QueryDescription _description;

    final QueryParameterHandler _handler;

    final ItemSyntax _itemSyntax;

    boolean _doCache = true;

    boolean _forceCache = false;

    boolean _cacheNothing = true;

    boolean _logQuery = false;

    boolean _joinFetch = false;

    /**
     * Create an unconfigured query builder.
     *
     * @param serviceContext
     *            used internally to help look up additional information as
     *            needed
     */
    AbstractQueryBuilder(ServiceContext serviceContext, QueryCache queryCache) {
        _serviceContext = serviceContext;
        _description = new QueryDescription();
        _handler = new QueryParameterHandler(_description);
        _qbCache = queryCache;
        _itemSyntax = new ItemSyntax(_description);
        _invalidationKeys = new ArrayList<>();
    }

    @Override
    public Optional<Item> getParent() {
        return Optional.ofNullable(_description._parent);
    }

    @Override
    public List<Condition> getConditions() {
        return _description._conditions.conjoinedConditions();
    }

    public final QueryBuilder setRoot(Item root) {
        _description.setRoot(root, false);
        return this;
    }

    public final QueryBuilder setImplicitRoot(Item root) {
        _description.setRoot(root, true);
        return this;
    }

    public final QueryBuilder setParent(Item parent) {
        _description.setParent(parent, false);
        return this;
    }

    public final QueryBuilder setImplicitParent(Item parent) {
        _description.setParent(parent, true);
        return this;
    }

    @Override
    public final QueryBuilder setId(Long id) {
        return addCondition(DataTypes.META_DATA_TYPE_ID, "eq", id);
    }

    public final QueryBuilder setItem(Item item) {
        _description.setItem(item);
        if (item != null) {
            _description.setItemType(item.getType(), true);
        } else if (_description._itemTypeImplicit) {
            // hmm.
            _description.setItemType(null, true);
        }
        return this;
    }

    public final QueryBuilder addInitialQuery(QueryBuilder initialQuery) {
        _description.addInitialQuery(initialQuery);
        return this;
    }

    public final QueryBuilder createInitialQuery() {
        var initialQuery = _serviceContext.queryBuilder();
        _description.addInitialQuery(initialQuery);
        return initialQuery;
    }

    public final QueryBuilder addExistsQuery(QueryBuilder existsQuery) {
        _description.addExistsQuery(existsQuery);
        return this;
    }

    @Override
    public final QueryBuilder addJoinQuery(String dataType, QueryBuilder joinQuery) {
        return addJoin(new Join.Inner(dataType, joinQuery));
    }

    @Override
    public QueryBuilder addJoin(Join join) {
        _description.addJoin(join);
        return this;
    }

    private final Map<String, QueryBuilder> joinQueries = new HashMap<>();

    @Override
    public final QueryBuilder getOrCreateJoinQuery(String dataType, String itemType) {
        String key = dataType + '/' + itemType;
        QueryBuilder qb = joinQueries.get(key);
        if (qb == null) {
            qb = _serviceContext.queryBuilder().setItemType(itemType);
            joinQueries.put(key, qb);
            addJoinQuery(dataType, qb);
        }
        return qb;
    }

    @Override
    public final QueryBuilder getOrAddJoinQuery(String dataType, String itemType, scala.Function0<Join> joinf) {
        String key = dataType + '/' + itemType;
        QueryBuilder qb = joinQueries.get(key);
        if (qb == null) {
            final var join = joinf.apply();
            qb = join.query();
            joinQueries.put(key, qb);
            addJoin(join);
        }
        return qb;
    }

    public final QueryBuilder setItemType(String itemType) {
        _description.setItemType(itemType, false);
        return this;
    }

    public final QueryBuilder setImplicitItemType(String itemType) {
        _description.setItemType(itemType, true);
        return this;
    }

    public final QueryBuilder setIncludeDeleted(boolean includeDeleted) {
        _description.setIncludeDeleted(includeDeleted);
        return this;
    }

    public final QueryBuilder setPath(String path) {
        _description.setPath(path);
        return this;
    }

    public final QueryBuilder setExcludePath(boolean exclude) {
        _description.setExcludePath(exclude);
        return this;
    }

    public final QueryBuilder setInclusive(boolean inclusive) {
        _description.setInclusive(inclusive);
        return this;
    }

    public final QueryBuilder addDisjunction0(List<Condition> conditions) {
        List<List<Condition>> l2 = new ArrayList<>();
        for (Condition condition : conditions) {
            l2.add(Collections.singletonList(condition));
        }
        _description.addDisjunction(l2);
        return this;
    }

    public final QueryBuilder addDisjunction(List<List<Condition>> conditions) {
        _description.addDisjunction(conditions);
        return this;
    }

    @Override
    public final <C extends Condition> QueryBuilder addDisjunction(scala.collection.Iterable<C> conditions) {
        List<Condition> disjunction = new ArrayList<>();
        for (scala.collection.Iterator<? extends Condition> i = conditions.iterator(); i.hasNext(); ) {
            disjunction.add(i.next());
        }
        return addDisjunction0(disjunction);
    }

    public final QueryBuilder addCondition(String type, String cmp, Object value) {
        return addCondition(BaseCondition.getInstance(type, Comparison.valueOf(cmp), value));
    }

    public final QueryBuilder addCondition(String type, Comparison cmp, Object value) {
        return addCondition(BaseCondition.getInstance(type, cmp, value));
    }

    public final QueryBuilder addCondition(String type, Comparison cmp, Object value, Function fn) {
        return addCondition(BaseCondition.getInstance(type, cmp, value, fn));
    }

    public final QueryBuilder addCondition(Condition condition) {
        assert condition.getComparison() != null : "The condition must have a valid comparison.";

        _description.addCondition(condition);
        return this;
    }

    @Override
    public final <C extends Condition>QueryBuilder addConjunction(Iterable<C> conditions) {
        for (Condition condition: conditions) {
            addCondition(condition);
        }
        return this;
    }

    @Override
    public final <C extends Condition> QueryBuilder addConjunction(scala.collection.Iterable<C> conditions) {
        for (scala.collection.Iterator<? extends Condition> i = conditions.iterator(); i.hasNext(); ) {
            addCondition(i.next());
        }
        return this;
    }

    public final QueryBuilder setGroup(String type) {
        _description.setGroup(type);
        return this;
    }

    public final QueryBuilder clearOrder() {
        _description.clearOrder();
        return this;
    }

    public final QueryBuilder setOrder(String type, Direction order) {
        return addOrder(BaseOrder.byData(type, order));
    }

    public final QueryBuilder setOrder(String type, Function aggregate,
            Direction order) {
        return addOrder(BaseOrder.byData(type, aggregate, order));
    }

    public final QueryBuilder setOrder(Function aggregate, Direction order) {
        return addOrder(BaseOrder.byAggregate(aggregate, order));
    }

    public final QueryBuilder setOrder(QueryBuilder query, Direction order) {
        return addOrder(BaseOrder.byChildQuery(query, order));
    }

    @Override
    public final QueryBuilder addOrders(Iterable<Order> orders) {
        orders.forEach(this::addOrder);
        return this;
    }

    @Override
    public final QueryBuilder addOrder(Order order) {
        return addJoinOrder(this, order);
    }

    @Override
    public final QueryBuilder addJoinOrder(QueryBuilder qb, Order order) {
        _description.addOrder((AbstractQueryBuilder) qb, order);
        return this;
    }


    public final QueryBuilder setCacheQuery(boolean cacheQuery) {
        if (cacheQuery) {
            _forceCache = true;
        } else {
            _doCache = false;
        }
        return this;
    }

    public final QueryBuilder setJoinFetch(boolean joinFetch) {
        _joinFetch = joinFetch;
        return this;
    }

    public final QueryBuilder setCacheNothing(boolean cacheNothing) {
        _cacheNothing = cacheNothing;
        return this;
    }

    public final QueryBuilder addInvalidationKey(String invalidationKey) {
        _invalidationKeys.add(invalidationKey);
        return this;
    }

    /**
     * Get all invalidation keys for your query and any of its subqueries.
     * @return Immutable Set of all invalidation keys for your query and any of its subqueries.
     */
    public final Set<String> getInvalidationKeys() {
        Set<String> set = new HashSet<>();
        set.addAll(_invalidationKeys);
        for (QueryDescription.SubQuery subquery : _description._subqueries) {
            set.addAll(subquery.getInvalidationKeys());
        }
        return Collections.unmodifiableSet(set);
    }

    public final QueryBuilder setLimit(int limit) {
        _description.setLimit(limit);
        return this;
    }

    public final QueryBuilder setLimit(Integer limit) {
        _description.setLimit(limit);
        return this;
    }

    public final QueryBuilder setFirstResult(int firstResult) {
        _description.setFirstResult(firstResult);
        return this;
    }

    public final QueryBuilder setFirstResult(Integer firstResult) {
        _description.setFirstResult(firstResult);
        return this;
    }

    public final QueryBuilder setNoResults() {
        _description._deny = true;
        return this;
    }

    @Override
    public final QueryBuilder setProjection(Projection projection) {
        _description.setProjection(projection, new Object[0]);
        return this;
    }

    @Override
    public final QueryBuilder setProjection(Projection projection, String parameter) {
        _description.setProjection(projection, new Object[]{parameter});
        return this;
    }

    @Override
    public final QueryBuilder setProjection(Projection projection, Object[] parameters) {
        _description.setProjection(projection, parameters);
        return this;
    }

    @Override
    public final QueryBuilder setDataProjection(String projection) {
        setProjection(Projection.DATA, new Object[]{projection});
        return this;
    }

    @Override
    public final QueryBuilder setDataProjection(DataProjection projection) {
        setProjection(Projection.DATA, new Object[]{projection});
        return this;
    }

    @Override
    public final QueryBuilder setDataProjection(DataProjection[] projection) {
        setProjection(Projection.DATA, (Object[]) projection);
        return this;
    }

    public final QueryBuilder setDistinct(boolean distinct) {
        _description.setDistinct(distinct);
        return this;
    }

    public final QueryBuilder setFunction(Function function) {
        _description.setFunction(function);
        return this;
    }

    public QueryBuilder having(QueryBuilder qb, Comparison cmp, Number value) {
        _description.having(qb, cmp, value);
        return this;
    }

    public Map<String, Object> setParameters(Query query) {
        return _handler.setParameters(query);
    }

    public QueryBuilder setLogQuery(boolean log) {
        _logQuery = log;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        List<T> results = executeQuery();
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getResultList() {
        return executeQuery();
    }

    @SuppressWarnings("unchecked")
    public List<Item> getItems() {
        return executeQuery();
    }

    @Override
    public List<ProjectionResult> getProjectedResults() {
        return new ProjectionResults(this);
    }

    @Override
    public final <T extends Facade> List<T> getFacadeList(Class<T> facadeClass) {
        FacadeService facadeService = _serviceContext.getService(FacadeService.class);
        List<T> list = new ArrayList<>();
        for (Item item : getItems(FacadeSupport.getItemType(facadeClass))) {
            T t = facadeService.getFacade(item, facadeClass);
            if (t != null) {
                list.add(t);
            }
        }
        return list;
    }

    @Override
    public final <T extends ComponentInterface> List<T> getComponentList(Class<T> componentClass) {
        return ComponentSupport.get(getItems(ComponentSupport.getItemType(componentClass)), componentClass);
    }

    @Override
    public final <T extends ComponentInterface> List<T> getComponentList(Class<T> componentClass, String itemType, boolean singleton) {
        if (itemType == null) {
            itemType = ComponentSupport.getItemType(componentClass);
        }
        return ComponentSupport.get(getItems(itemType), componentClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Seq<T> getValues() {
        return CollectionConverters.asScala((List<T>) executeQuery()).toSeq();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T extends ComponentInterface> Seq<T> getComponents(ClassTag<T> tt, DataModel<T> dm) {
        Class<T> componentClass = (Class<T>) tt.runtimeClass();
        String itemType = dm != null ? dm.itemType() : ComponentSupport.getItemType(componentClass);
        boolean singleton = dm != null ? dm.singleton() : ComponentSupport.isSingleton(componentClass);
        return CollectionConverters.asScala(getComponentList(componentClass, itemType, singleton)).toSeq();
    }

    @Override
    public final <T extends Finder> Seq<T> getFinders(ClassTag<T> tt) {
        final Class<T> cls = (Class<T>) tt.runtimeClass();
        return CollectionConverters.asScala(getItems().stream().map(item -> cls.cast(item.getFinder())).collect(Collectors.toList())).toSeq();
    }

    @Override
    public final <T extends Facade> Seq<T> getFacades(ClassTag<T> tt) {
        return CollectionConverters.asScala(getFacadeList((Class<T>) tt.runtimeClass())).toSeq();
    }

    private List<Item> getItems(String hintType) {
        List<?> results = getResultList();
        if (results.isEmpty()) {
            return Collections.emptyList();
        } else if (results.get(0) instanceof Item) {
            return (List<Item>) results;
        } else {
            return ItemSupport.get((List<Long>) results, hintType);
        }
    }

    public final Long getAggregateResult(Function aggregate) {
        setFunction(aggregate);
        Long result = (Long) getResult();
        setFunction(null);
        return result;
    }

    @SuppressWarnings("unchecked")
    public final <T> List<T> getProjectedResults(String type) {
        setDataProjection(type);
        return (List<T>) executeQuery();
    }

    /**
     * This is used for using a built query as an initial condition.
     */
    public String buildSubquery() {
        // If this is not a native query it is SELECT * FROM X WHERE X.id IN (SELECT bar FOM Y)
        // but now we need to do (SELECT bar.id) except in native where it was already bar_id.
        return buildQuery(_description.getProjection(true, !_description.nativeQuery()), true);
    }

    public String buildExistsQuery(String field) {
        String projection = _description.getProjection(true);
        return buildQuery(projection, true) + " AND (" + projection + " = " + field + ")";
    }

    abstract ConjunctionBuilder getConjunctionBuilder();

    abstract void appendDataJoins(StringBuilder sqlBuffer,
            DataProjection[] projections);

    void appendRelatedJoins(StringBuilder sqlBuffer) {
        for (Order order : _description._orders) {
            if (order.getQuery() != null) {
                appendRelatedJoin(sqlBuffer, order.getQuery(), order.getType());
            }
        }
    }

    // select page.i, avg0(comment.rating) from wikipagefinder page
    //   left outer join
    //   (select * from commentfinder comment where comment.deleted <> true) comment
    //   on comment.parent_id = page.id
    //   group by page.id
    //   order by coalesce(avg(comment.rating), 0), page.id desc;
    // TODO add a non-trivial EJB-QL version, if possible
    void appendRelatedJoin(StringBuilder sqlBuffer, QueryBuilder qbuilder, String type) {
        AbstractQueryBuilder qb = qbuilder instanceof AbstractQueryBuilder ? (AbstractQueryBuilder) qbuilder : null;
        if(qb == null) {
            return;
        }
        // TODO: I think we could rewrite  by getting rid of the SELECT * FROM
        // ... hackery and just say .append("(").append(qb.buildSubquery()).append(")")
        // if we set the qb projection to be Projection.ALL
        sqlBuffer.append(" LEFT OUTER JOIN (SELECT * FROM ");
        sqlBuffer.append(qb._description._entityDescription.tableName());
        sqlBuffer.append(' ').append(qb._description._itemLabel);

        // We only do the most basic tests.. the child must be normalized
        // and the conditions must apply to normalized data only.. We
        // actually might be able to do more now that we are expressing
        // this as a join on a subselect, but that can be done later.
        qb.appendItemTests(sqlBuffer, true);
        qb.appendConditionTests(sqlBuffer);
        sqlBuffer.append(") ").append(qb._description._itemLabel);
        sqlBuffer.append(" ON ").append(qb._description._itemLabel);
        String field = (type == null) ? "parent_id"
            : qb._description.getField(type, true);
        sqlBuffer.append('.').append(field).append(" = ");
        sqlBuffer.append(_description._itemLabel);
        sqlBuffer.append(".id");
    }

    void appendItemTests(StringBuilder sqlBuffer, boolean initial) {
        StringBuilder sb = new StringBuilder();

        if (!_description._initialQueries.isEmpty()) {
            String projection = _itemSyntax.idProjection();
            StringBuilder isb = new StringBuilder();
            for (QueryDescription.SubQuery qb : _description._initialQueries) {
                if (qb.getDescription()._inclusive) {
                    QuerySyntax.or(isb).append(projection).append(" IN ").append('(')
                            .append(qb.buildSubquery()).append(')');
                } else {
                    QuerySyntax.and(isb).append(projection).append(" NOT IN ").append('(')
                            .append(qb.buildSubquery()).append(')');
                }
            }
            sb.append('(').append(isb).append(')');
        }
        if (!_description._existsQueries.isEmpty()) {
            String projection = _itemSyntax.idProjection();
            StringBuilder isb = new StringBuilder();
            for (QueryDescription.SubQuery qb : _description._existsQueries) {
                if (qb.getDescription()._inclusive) {
                    // TODO: This OR is *WRONG* in general, it should only apply
                    // to a a set of explicitly grouped exists/initial queries.
                    // Consider filtering by multiple taxons from different
                    // taxonomies in manage groups. Initial query needs to be
                    // turned into a condition so the caller can be explicit
                    // about con- and dis-junctions. Now an AND..
                    QuerySyntax.and(isb).append(" EXISTS ").append('(')
                            .append(qb.buildExistsQuery(projection)).append(')');
                } else {
                    QuerySyntax.and(isb).append(" NOT EXISTS ").append('(')
                            .append(qb.buildExistsQuery(projection)).append(')');
                }
            }
            sb.append('(').append(isb).append(')');
        }
        if ((_description._root != null) && !_description._rootImplicit && !_description._joinRoot) {
            QuerySyntax.and(sb).append(" (");
            _itemSyntax.rootExpr(sb).append(")");
        }
        if ((_description._parent != null) && !_description._parentImplicit) {
            QuerySyntax.and(sb).append(" (");
            _itemSyntax.parentExpr(sb).append(")");
        }
        if ((_description._item != null)) {
            QuerySyntax.and(sb).append(" (");
            _itemSyntax.itemExpr(sb).append(")");
        }
        if (!_description._includeDeleted && _description.softDeletable() && !_description._joinDeleted) {
            QuerySyntax.and(sb).append(" (");
            _itemSyntax.notDeletedExpr(sb).append(")");
        }
        // If selecting from a finder then the type is implicit
        if ((_description._itemType != null) && !_description._itemTypeImplicit
                && (_description._entityDescription == null)) {
            QuerySyntax.and(sb).append(" (");
            _itemSyntax.typeExpr(sb).append(")");
        }
        if (_description._path != null) {
            QuerySyntax.and(sb).append(" (").append(_description._itemLabel)
                .append(".path ").append(_description._excludePath ? "NOT  LIKE" : "LIKE").append(" :")
                .append(_description._pathLabel).append(")");
        }
        if (sb.length() == 0) {
            if (_description.nativeQuery()) {
                // A system-wide query may have no conditions but the
                // subsequent test methods always begin with AND, leading
                // to a syntax error in that case.
                sb.append("TRUE");
            } else {
                // WHERE TRUE is not valid JPA QL, hence this monstrosity. TODO: FIXME.
                sb.append(" (").append(_description._itemLabel)
                    .append(".id IS NOT NULL)");
            }
        }

        sqlBuffer.append(initial ? " WHERE " : " AND ").append(sb);
    }

    void appendJoinConditionTests(StringBuilder sqlBuilder) {
        for (Join join : _description._joinQueries) {
            AbstractQueryBuilder qb = (AbstractQueryBuilder) join.query();
            qb.appendItemTests(sqlBuilder, false);
            qb.appendConditionTests(sqlBuilder);
            qb.appendDisjunctionTests(sqlBuilder, qb._description._disjunctions, null);
            qb.appendJoinConditionTests(sqlBuilder);
        }
    }

    abstract void appendDataTypeTests(StringBuilder sqlBuffer,
            DataProjection[] projections);

    abstract void appendDisjunctionTests(StringBuilder sqlBuffer,
                                         DisjunctionSeries disjunctions,
                                         String prefix);

    abstract void appendConditionTests(StringBuilder sqlBuffer);

    final String buildQuery(String projection, boolean isSubquery) {
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("SELECT ").append(projection);

        if (_description._having != null) { // TODO: KILLME: Horrible HACK
            // This is because SELECT COUNT(iA.id) FROM ... GROUP BY iA.id ... HAVING
            // returns N rows of 1 (i.e. a count of 1 for each group) rather than 1 row of N
            // So I need to rewrite as SELECT COUNT(iA.id) FROM (SELECT iA.id FROm ... HAVING ...) iA
            String subprojection = StringUtils.substringAfter(StringUtils.substringBefore(projection, ")"), "(");
            sqlBuilder.append(" FROM (SELECT ").append(subprojection);
        }

        if (null == _description._entityDescription) {
            sqlBuilder.append(" FROM Item");
        } else {
            sqlBuilder.append(" FROM ").append(_itemSyntax.entityOrTable());
        }
        sqlBuilder.append(" ").append(_description._itemLabel);

        if (_joinFetch && !isSubquery && (_description._entityDescription != null) && (_description._projection == Projection.ITEM) && (_description._function == null) && _description._entityDescription.isPeered() && !_description.nativeQuery()) {
            sqlBuilder.append(" JOIN FETCH ").append(_description._itemLabel).append(".owner");
        }

        appendJoinQueries(sqlBuilder);

        appendDataJoins(sqlBuilder, _description._dataProjection);
        appendRelatedJoins(sqlBuilder);
        appendItemTests(sqlBuilder, true);
        appendDataTypeTests(sqlBuilder, _description._dataProjection);
        appendDisjunctionTests(sqlBuilder, _description._disjunctions, null);

        appendConditionTests(sqlBuilder);
        appendJoinConditionTests(sqlBuilder);

        // TODO: both of these are not quite right because I can't mix group or order
        // on the main query with group or order on the join query

        appendGroup(sqlBuilder);
        for (Join join : _description._joinQueries) {
            ((AbstractQueryBuilder) join.query()).appendGroup(sqlBuilder);
        }

        if (_description._limit > 0 && _description._orderSequence.isEmpty()) {
            logger.log(Level.FINER, () ->"Setting limit on query with no order:\n" + explain(_description));
        }

        if (!Function.COUNT.equals(_description._function)) {
            appendOrder(sqlBuilder);
        }

        switch (_description._projection) {
          case ITEM_CONTEXT: // OMG
              String sqla = sqlBuilder.toString();
              sqlBuilder.setLength(0);
              sqlBuilder.append("SELECT * FROM item_context(:itemContext, ARRAY(");
              sqlBuilder.append(sqla);
              sqlBuilder.append("))");
              break;
          case CALENDAR_INFO: // OMG
              String sqlb = sqlBuilder.toString();
              sqlBuilder.setLength(0);
              sqlBuilder.append("SELECT * FROM calendar_info(:calendarStart, :calendarEnd, ARRAY(");
              sqlBuilder.append(sqlb);
              sqlBuilder.append("))");
              break;
        }
        if (_description._having != null) { // TODO: KILLME: Horrible HACK
            String alias = StringUtils.substringAfter(StringUtils.substringBefore(projection, "."), "(");
            sqlBuilder.append(") ").append(alias);
        }

        return sqlBuilder.toString();
    }

    private void appendJoinQueries(StringBuilder sqlBuilder) {
        int index = 0;
        for (Join join : _description._joinQueries) {
            AbstractQueryBuilder joinQB = ((AbstractQueryBuilder) join.query());
            if (_description.nativeQuery()) {
                final String tbl = (null == joinQB._description._entityDescription) ? "Item" : joinQB._itemSyntax.entityOrTable();
                sqlBuilder.append(join.joinWord())
                  .append(tbl)
                  .append(" ").append(joinQB._description._itemLabel)
                  .append(" ON (");

                /* This and below conditional blocks are because getField
                 * is inconsistent about its use of prefixes. TODO: Make it consistent. */
                if (!join.rightDataType().startsWith("#")) {
                    sqlBuilder.append(joinQB._description._itemLabel).append(".");
                }
                sqlBuilder.append(joinQB._description.getField(join.rightDataType(), true));

                sqlBuilder.append(" = ");

                if (!join.leftDataType().startsWith("#")) {

                    sqlBuilder.append(_description._itemLabel).append(".");
                }
                sqlBuilder.append(_description.getField(join.leftDataType(), true));

                sqlBuilder.append(buildJoinConditions(join, index)).append(") ");
            } else {
                sqlBuilder.append(join.joinWord())
                  .append(_description._itemLabel)
                  .append(".").append(_description.getField(join.leftDataType(), true))
                  .append(" ").append(joinQB._description._itemLabel);

                String conds = buildJoinConditions(join, index);

                if (!conds.isEmpty()) {
                    /* aaaaaah! */
                    sqlBuilder
                      .append(" ON ((")
                      .append(joinQB._description._itemLabel)
                      .append(".id IS NOT NULL)")
                      .append(conds)
                      .append(") ");
                }
            }
            joinQB.appendJoinQueries(sqlBuilder);
            index++;
        }
    }

    // AND ((A AND B) OR C) AND NOT i.deleted ....
    private String buildJoinConditions(Join join, int index) {
        AbstractQueryBuilder joinQB = ((AbstractQueryBuilder) join.query());
        StringBuilder builder = new StringBuilder();

        /* we don't support join conditions on inner joins */
        if (!(join instanceof Join.Inner)) {

            AbstractQueryBuilder outerSide =
              join instanceof Join.Left ? joinQB : this;

            if (join.conditions().value().size() == 1) {
                /* DisjunctionSeries is too clever here. */
                builder.append(QuerySyntax.AND).append(QuerySyntax.L_PAREN);
                Conjunction conjunction = new Conjunction(outerSide._description);
                join.conditions().asJava().get(0).forEach(conjunction::addCondition);
                outerSide._description.applyConjunction(
                  conjunction, builder, "j_0_0_",
                  outerSide.getConjunctionBuilder(),
                  outerSide._handler);
                builder.append(QuerySyntax.R_PAREN);
            } else {
                DisjunctionSeries dnf = new DisjunctionSeries(outerSide._description);
                dnf.addDisjunction(join.conditions().asJava());
                dnf.preprocessDisjunctions();

                // AND ((A AND B) OR (C AND D))
                outerSide.appendDisjunctionTests(builder, dnf, "j_" + index);
            }

            if (outerSide._description._joinDeleted) {
                builder.append(QuerySyntax.AND);
                outerSide._itemSyntax.notDeletedExpr(builder);
            }

            /* just in case you're cross-domain outer joining for some reason */
            if (outerSide._description._joinRoot) {
                builder.append(QuerySyntax.AND).append(" (");
                if (outerSide._description.nativeQuery()) {
                    outerSide._itemSyntax.rootExpr(builder);
                } else {
                    /* inside an on clause, you can't mention another table at all.
                     * this can be removed if we ever get onto Hibernate 5. */
                    builder.append(outerSide._description._itemLabel)
                      .append(".root.id = :root_id");
                }
                builder.append(") ");
            }
        }

        return builder.toString();
    }

    final void appendJoin(StringBuilder sqlBuffer, String dataType) {
        if ((null == dataType) || dataType.startsWith("#")) {
            return;
        }

        if (!_description.inDataCollection(dataType)) {
            return;
        }

        if(_description._entityDescription != null && !_description._entityDescription.isPeered()){
            throw new IllegalArgumentException(String.format(
              "Cannot map data type [%s] to standalone item type [%s]."
            , dataType, _description._itemType));
        }

        String alias = _description.getAlias(dataType);
        if (_description.nativeQuery()) {
            sqlBuffer.append(" INNER JOIN Data ").append(alias)
            .append(" ON ").append(alias).append(".owner_id = ")
            .append(_description._itemLabel)
            .append( ".id");
        } else {
            sqlBuffer.append(" JOIN ").append(_description._itemLabel);
            if (_description._entityDescription != null) {
                sqlBuffer.append(".owner");
            }
            sqlBuffer.append(".data ").append(alias);
        }
    }

    final void appendType(StringBuilder sqlBuffer, String dataType) {
        if ((null == dataType) || dataType.startsWith("#")) {
            return;
        }

        if (!_description.inDataCollection(dataType)) {
            return;
        }

        String alias = _description.getAlias(dataType);
        sqlBuffer.append(" AND (").append(alias).append(_description.nativeQuery() ? ".type_name = :" : ".type = :")
                .append(alias).append("Type)");
    }

    final void appendGroup(StringBuilder sqlBuffer) {
        boolean orderQuery = false;
        for (Order order : _description._orders) {
            orderQuery |= (order.getQuery() != null);
        }
        if (orderQuery || (_description._groupType != null)) {
            // allows adding grouping statements from joins
            sqlBuffer.append(sqlBuffer.indexOf(" GROUP BY") < 0 ? " GROUP BY " : ", ");
            if (orderQuery) {
                // this only works if we're selecting the item/finder itself
                sqlBuffer.append(_description._itemLabel);
                sqlBuffer.append(".id");
                for (Order order : _description._orders) {
                    if ((order.getQuery() == null) && (order.getType() != null) && !order.getType().startsWith("#")) {
                        String alias = _description.getAlias(order.getType());
                        String field = _description.getField(order.getType());
                        sqlBuffer.append(", ").append(alias).append('.').append(field);
                    }
                }
            } else {
                for (int i = 0; i < _description._groupType.size(); ++i) {
                    if (i != 0) {
                        sqlBuffer.append(", ");
                    }
                    String groupType = _description._groupType.get(i);
                    sqlBuffer.append(_description.getEntityField(groupType, false));
                }
            }
        }
        if (_description._having != null) {
            sqlBuffer.append(" HAVING ");
            String projection = _description._having._description.getProjection(true);
            if (_description._having._description._function == Function.AVG_WITH_COUNT) {
                projection = projection.substring(0, projection.indexOf(','));
            }
            sqlBuffer.append(projection).append(' ').append(_description._havingCmp.getQueryOperator()).append(" :")
                .append(_description._having._description._valueLabel).append("Having");
        }
    }

    final void appendOrder(StringBuilder sqlBuffer) {
        for (int i = 0; i < _description._orderSequence.size(); ++i) {
            Order order = _description._orderSequence.get(i).getRight();
            QueryDescription description = _description._orderSequence.get(i).getLeft();
            sqlBuffer.append((i == 0) ? " ORDER BY " : ", ");
            if (order.getQuery() != null) {
                QueryBuilder qbuilder = order.getQuery();
                AbstractQueryBuilder qb = qbuilder instanceof AbstractQueryBuilder ? (AbstractQueryBuilder) qbuilder : null;
                QueryDescription qbdesc = qb.getDescription();
                Function fn = qbdesc._function;
                if (fn == Function.AVG_WITH_COUNT) { // GRrrr
                    String projection = qbdesc.getProjection(true);
                    int index = projection.indexOf(',');
                    sqlBuffer.append("coalesce(").append(projection.substring(0, index)).append(", 0) ").append(order.getDirection().sql()).append(projection.substring(index));
                } else {
                    if ((fn != null) && fn.coalesceToZeroInOrder()) {
                        sqlBuffer.append("coalesce(");
                    }
                    sqlBuffer.append(qbdesc.getProjection(true));
                    if ((fn != null) && fn.coalesceToZeroInOrder()) {
                        sqlBuffer.append(", 0)");
                    }
                }
            } else if (order.getType() == null) {
                switch (order.getFunction()) {
                  case RANDOM:
                      sqlBuffer.append("random()");
                      break;
                  case ID:
                      sqlBuffer.append(description._itemLabel).append(".id");
                      break;
                  case LAST_MODIFIED:
                      sqlBuffer.append(description._itemLabel).append(".lastModified");
                      break;
                  default:
                      // TODO: AVG_WITH_COUNT as above
                      if (order.getFunction().coalesceToZeroInOrder()) {
                          sqlBuffer.append("coalesce(");
                      }
                      sqlBuffer.append(order.getFunction()).append("(").append(
                          description._itemLabel).append(')');
                      if (order.getFunction().coalesceToZeroInOrder()) {
                          sqlBuffer.append(", 0)");
                      }
                      break;
                }
            } else {
                String entityField = description.getEntityField(order.getType(), false);
                if (order.getJsonField() != null) {
                    // field.field.field for strings, field.field.field::TYPE for other types
                    var jsonField  = order.getJsonField();
                    while (jsonField.indexOf('.') >= 0) {
                        var index = jsonField.indexOf('.');
                        entityField = entityField + "->'" + jsonField.substring(0, index) + "'";
                        jsonField = jsonField.substring(1 + index);
                    }
                    var colon = jsonField.indexOf("::");
                    if (colon < 0) {
                        entityField = entityField + "->>'" + jsonField + "'";
                    } else {
                        var field = jsonField.substring(0, colon);
                        var type = jsonField.substring(colon + 2);
                        entityField = "CAST (" + entityField +  "->>'" + field + "' AS " + type + ")";
                    }
                }
                if ((order.getFunction() == null) || (order.getFunction() == Function.NONE)) {
                    Order coalesce = order.getCoalesce();
                    if (coalesce == null) {
                        sqlBuffer.append(entityField);
                    } else {
                        String coalesceField = ((AbstractQueryBuilder) coalesce.getQuery())
                          .getDescription().getEntityField(coalesce.getType(), false);
                        sqlBuffer.append("COALESCE(" + entityField + ", " + coalesceField + ")");
                    }
                } else if (order.getFunction() == Function.LAST_MODIFIED) {
                    sqlBuffer.append(entityField).append(".lastModified");
                } else {
                    sqlBuffer.append(order.getFunction()).append('(').append(
                        entityField).append(')');
                }
            }
            sqlBuffer.append(' ').append(order.getDirection().sql());
        }
    }

    public String getCacheKey() {
        QueryCacheKey cacheEntryKey = new QueryCacheKey(_description);
        return cacheEntryKey.getKey();
    }

    public void cacheValues(List<?> results) {
        cacheQueryResults(new QueryCacheKey(_description), results);
    }

    @Override
    public <T extends Id> Multimap<Long, Item> preloadPartitionedByParent(Iterable<T> parents) {
        int oldLimit = _description._limit;
        _description.setLimit(-1); // if there's a single item limit, it should be applied per parent instead...
        List<Item> items = getItems();
        _description.setLimit(oldLimit);
        ArrayListMultimap<Long, Item> map = ArrayListMultimap.create();
        for (Item item : items) { //this line will crash if getResults did not in fact return items
           map.put(item.getParent().getId(), item);
        }
        for (Id id: parents) { // important to iterate through actual parents to be able to cache no results
            List<Item> children = map.get(id.getId());
            PartitionedByParentQueryCacheKey key = new PartitionedByParentQueryCacheKey(getDescription(), id);
            cacheQueryResults(key, (oldLimit < 0) ? children : children.subList(0, Math.min(oldLimit, children.size())));
        }
        return map;
    }

    @Override
    public <T extends Id> Map<Id, Collection<Long>> preloadPartitionedByParentToProjection(Iterable<T> parents, DataProjection projection) {
        // Spoof this query to have the parent id as a data projection so we can map what came from where
        DataProjection idProjection = BaseDataProjection.ofDatum(DataTypes.META_DATA_TYPE_PARENT_ID);
        setDataProjection(new DataProjection[]{idProjection, projection});
        List<Object[]> queryResults = getResultList();

        // Undo our spoof so that the cache key from getDescription() is correct
        setDataProjection(projection);

        Multimap<Id, Long> results = ArrayListMultimap.create();
        for (Object[] row : queryResults) {
            Id parentId = Ids.of(((Number) row[0]).longValue());
            Long projectionValue = ((Number)row[1]).longValue();
            results.put(parentId, projectionValue);
        }

        for (Id parent : results.keySet()) {
            Collection<Long> values = results.get(parent);
            PartitionedByParentQueryCacheKey key = new PartitionedByParentQueryCacheKey(getDescription(), parent);
            Current.put(key.getKey(), values); //TODO TECH-70 encapsulate usage of Current in QueryCurrentCache
        }

        return results.asMap();
    }

    @Override
    public <T extends Id> Multimap<Long, Item> preloadPartitionedByInCondition(String dataType, Iterable<T> values) {
        final ArrayListMultimap<Long, Item> multimap = ArrayListMultimap.create();
        final List<Item> results = getItems();//TODO use a projection-aware overload of getResults...
        for (Item result: results) {
            Long dataValue = DataTransfer.getPKData(result, dataType);
            multimap.put(dataValue, result);
        }
        for (T value : values) {
            Long id = value.getId();
            cacheQueryResults(getPartitionedByFieldsQueryCacheKey(_description, dataType, id), multimap.get(id));
        }
        return multimap;
    }

    @VisibleForTesting
    protected <V> QueryCacheKey getPartitionedByFieldsQueryCacheKey(QueryDescription _description, String fieldName, V fieldValue) {
        return new PartitionedByFieldsQueryCacheKey<>(_description, fieldName, fieldValue);
    }

    @VisibleForTesting
    protected void cacheQueryResults(QueryCacheKey queryCacheKey, Collection<?> cacheableResults) {
        Current.put(queryCacheKey.getKey(), cacheableResults); //TODO TECH-70 encapsulate usage of Current in QueryCurrentCache
    }

    @SuppressWarnings("unchecked")
    private List executeQuery() {
        if (_description._deny) {
            return new ArrayList<Object>();
        }

        if (_description._includeDeleted) {
            for (QueryDescription.SubQuery qb : _description._subqueries) {
                qb.getDescription().setIncludeDeleted(_description._includeDeleted);
            }
        }

        if (_description.nativeQuery()) {
            for (QueryDescription.SubQuery qb : _description._subqueries) {
                qb.getDescription().forceNative();
            }
        }
        _description.setSubqueryIndex(0);

        String projection = _description.getProjection(false);
        boolean isOrderedDistinct = _description._distinct
                && !_description._orders.isEmpty();

        if (isOrderedDistinct) {
            if ((_description._projection != Projection.ID)
                    && (_description._projection != Projection.ITEM)) {
                throw new IllegalStateException(
                        "Invalid ordered distinct query");
            }
            for (Order order : _description._orders) {
                String entityField = _description.getEntityField(order
                        .getType(), false);
                if (order.getFunction() == null) {
                    projection = projection + ", " + entityField;
                } else {
                    projection = projection + ", " + order.getFunction() + '('
                            + entityField + ")";
                }
            }
        }

        // If this query has been polluted by this transaction then I can't rely on
        // the query cache and I can't update the query cache.. In theory I could
        // have a transaction query cache that gets published upon commit, but...
        boolean polluted = Current.isPolluted(_description._root, _description._parent, _description._itemType); //TODO TECH-70 encapsulate usage of Current in QueryCurrentCache
        // TODO: if flush mode is commit then i ought to flush on polluted, which == flush auto so..

        String sql = buildQuery(projection, false);
        String key = getCacheKey();

        List preloaded = polluted ? null : (List) Current.get(key); //TODO TECH-70 encapsulate usage of Current in QueryCurrentCache
        if (preloaded != null) {
            logger.log(Level.FINE, "Current cache hit, {0}", new Object[]{key});
            return preloaded;
        }

        Set<String> invalidationKeys = getInvalidationKeys();

        boolean doCache = _doCache && !polluted &&
            ((_description._parent != null) || (!invalidationKeys.isEmpty()) || _forceCache);
        logger.log(Level.FINE, "Key, {0}, {1}", new Object[]{key, doCache});

        QueryResults results = doCache
          ? _qbCache.get(key).getOrElse(() -> null)
          : null;

        if (results != null) {
            logger.log(Level.FINE, "Cached query results, {0}", results);
            return results.getResultList(_serviceContext.getEntityManager());
        }

        Level level2 = _logQuery ? Level.WARNING : Level.FINE;
        logger.log(level2, "SQL, {0}", new Object[]{sql});

        Query query = _description.nativeQuery() ? _serviceContext.createNativeQuery(sql) :
            _serviceContext.createQuery(sql);
        Map<String, Object> params = _handler.setParameters(query);

        // https://blog.makk.es/postgresql-parameter-limitation.html
        if (params.size() > 30000) {
            throw new IllegalArgumentException("Too many query parameters: " + params.size());
        }

        Level level1 = _logQuery ? Level.WARNING : Level.FINE;
        logger.log(level1, "Parameters, {0}", new Object[]{params});

        List resultList;
        try {
            resultList = query.getResultList();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error executing constructed query:\n" + sql + "\nParameters" + params, ex);
            throw ex;
        }

        Level level = _logQuery ? Level.WARNING : Level.FINE;
        logger.log(level, "Results, {0}", new Object[]{resultList});
        //        LogUtils.log(logger, Level.WARNING, "Results", resultList);

        if (isOrderedDistinct) {
            for (ListIterator i = resultList.listIterator(); i.hasNext();) {
                Object[] tuple = (Object[]) i.next();
                i.set(tuple[0]);
            }
        }
        if (_description.nativeQuery()) {
            for (ListIterator i = resultList.listIterator(); i.hasNext();) {
                Object o = i.next();
                if (_description._projection.multiple()) {
                    Object[] a = (Object[]) o;
                    for (int j = 0; j < a.length; ++ j) {
                        if (a[j] instanceof Number) {
                            a[j] = ((Number) a[j]).longValue();
                        }
                    }
                } else {
                    if (o instanceof Number) {
                        i.set (((Number) o).longValue());
                    } else if (o != null) {
                        break;
                    }
                }
            }
        }
        if (_description.isDataRewriteNeeded()) {
            String dataProjection = _description._dataProjection[0].getType();
            boolean isTime = BaseOntology.getOntology().getDataFormat(dataProjection) == DataFormat.time;
            for (ListIterator i = resultList.listIterator(); i.hasNext();) {
                Long value = (Long) i.next();
                i.set(isTime ? DataSupport.toTime(value) : DataSupport.toBoolean(value));
            }
        } else if (_description.isFinderRewriteNeeded()) {
            if (_description.nativeQuery()) {
                List<Long> ids = new ArrayList<>(resultList.size());
                if (_description._projection.multiple()) {
                    for (Object[] o : (List<Object[]>) resultList) {
                        ids.add((Long) o[0]);
                    }
                } else {
                    ids.addAll((List<Long>) resultList);
                }
                Map<Long, Item> items = _serviceContext.getItemService().map(ids, _description._itemType);
                if (_description._projection.multiple()) {
                    for (Object[] o : (List<Object[]>) resultList) {
                        o[0] = items.get((Long) o[0]);
                    }
                } else {
                    for (ListIterator i = resultList.listIterator(); i.hasNext();) {
                        Long id = (Long) i.next();
                        Item item = items.get(id);
                        if (item == null) {
                            logger.warning("Removing unmatched finder from query results: " + _description._itemType + "[" + id + "]");
                            _serviceContext.getItemService().get(id, _description._itemType); // As a side-effect this will tidy up any del mismatch
                            i.remove();
                        } else {
                            i.set(item);
                        }
                    }
                }
            } else if (!_description._entityDescription.isPeered()) {
                if (_description._projection.multiple()) {
                    for (Object[] o : (List<Object[]>) resultList) {
                        o[0] = ((Finder) o[0]).getOwner();
                    }
                } else {
                    for (ListIterator i = resultList.listIterator(); i.hasNext();) {
                        i.set(((Finder) i.next()).getOwner());
                    }
                }
            } else {
                List<Long> ids = new ArrayList<>(resultList.size());
                if (_description._projection.multiple()) {
                    for (Object[] o : (List<Object[]>) resultList) {
                        ids.add(((Finder) o[0]).getId());
                    }
                } else {
                    for (Finder finder : (List<Finder>) resultList) {
                        ids.add(finder.getId());
                    }
                }
                // this will link the finders which will come from L1 cache
                // without N database hits
                Map<Long, Item> items = _serviceContext.getItemService().map(ids);
                if (_description._projection.multiple()) {
                    for (Object[] o : (List<Object[]>) resultList) {
                        o[0] = items.get(((Finder) o[0]).getId());
                    }
                } else {
                    for (ListIterator i = resultList.listIterator(); i.hasNext();) {
                        Finder finder = (Finder) i.next();
                        Item item = items.get(finder.getId());
                        if (item == null) {
                            logger.warning("Removing unmatched finder from query results: " + _description._itemType + "[" + finder.getId() + "]");
                            i.remove();
                        } else {
                            i.set(item);
                        }
                    }
                }
            }
        } else if (_description.isItemProjection() && _description.nativeQuery()) {
            // if i knew the item type, use it...
            List<Item> items = _serviceContext.getItemService().get((List<Long>) resultList);
            resultList.clear();
            resultList.addAll(items);
        }

        if (doCache && (_cacheNothing || !resultList.isEmpty())) {
            CacheElementStorage storage = CacheElementStorageFactory.store(
                    _description, resultList);
            _qbCache.put(new QueryEntry(new QueryResults(_description._parent,
                    _description._itemType, invalidationKeys, storage), key));
        }

        return resultList;
    }

    @Override
    public QueryDescription getDescription() {
        return _description;
    }

    @Override
    public void evictQuery() {
        _qbCache.remove(getCacheKey());
    }

    @Override public QueryBuilder forceNative() {
        _description.forceNative();
        return this;
    }
}
