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

package com.learningobjects.cpxp.component.query;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.de.web.QueryHandler;
import com.learningobjects.de.web.Queryable;
import org.apache.commons.collections4.map.DefaultedMap;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class ApiQuerySupport {


    private static final Map<DataFormat, QueryStrategy> QUERY_STRATEGY;
    private static final ISO8601DateFormat DATE_FORMAT = new ISO8601DateFormat();

    static {
        final Map<DataFormat, QueryStrategy> queryStrategy =
          new DefaultedMap<>(QueryStrategy.SIMPLE);
        queryStrategy.put(DataFormat.json, QueryStrategy.JSON);
        queryStrategy.put(DataFormat.tsvector, QueryStrategy.TSVECTOR);
        QUERY_STRATEGY = Collections.unmodifiableMap(queryStrategy);
    }

    /**
     * Apply the the predicates, orderings, and pagination attributes of the given {@link
     * ApiQuery} to a query over children of the given parent, and run the query.
     *
     * @param parent the parent whose children to query
     */
    public static <T extends ComponentInterface> ApiQueryResults<T> query(
      final Long parent, final ApiQuery query, final Class<T> componentType) {
        ItemMapping itemMapping = Optional.ofNullable(query.getItemMapping())
          .orElseGet(() -> componentType.getAnnotation(ItemMapping.class));
        String itemType = itemMapping.value();
        QueryBuilder qb = ServiceContext.getContext().getService(QueryService.class)
          .queryParent(parent, itemType);
        return query(qb, query, componentType);
    }

    /**
     * Apply the the predicates, orderings, and pagination attributes of the given {@link
     * ApiQuery} to the given {@link QueryBuilder}, and run the query.
     *
     * @param qb the {@link QueryBuilder} to decorate. Make sure the first result is
     *           either zero or not set(otherwise the total count on the results will be null)
     */
    public static <T extends ComponentInterface> ApiQueryResults<T> query(
      final QueryBuilder qb, final ApiQuery query0, final Class<T> componentType) {

        /* If SRS thinks we're returning a component type it'll give us an
         * ApiQuery with property mappings attached. If it can't figure that
         * out or we're not using component interfaces we get a raw ApiQuery,
         * so throw the property mappings from `componentType` on since we
         * know them now, at least. (This is a symptom of laziness, yes.)
         */
        ApiQuery query;
        if (query0.getPropertyMappings().isEmpty()) {
            query = new ApiQuery.Builder(query0)
              .addPropertyMappings(componentType)
              .build();
        } else query = query0;

        /*
         * the defining characteristic of prefilters is that they run before totalCount
         * is calculated
         */
        for (final ApiFilter prefilter : query.getPrefilters()) {
            applyFilter(qb, prefilter, query.getPropertyMappings());
        }

        return query(qb, query, componentType, ApiQuerySupport::getQueryBuilder);
    }

    /**
     * Apply the the predicates, orderings, and pagination attributes of the given {@link
     * ApiQuery} to the given {@link QueryBuilder}, and run the query, using a supplied
     * function for decorating the query builder with the api query. The caller should
     * already have applied any prefilters.
     *
     * @param qb             the {@link QueryBuilder} to decorate. Make sure the first result is
     *                       either zero or not set(otherwise the total count on the results will be null)
     * @param filterFunction a function responsible for applying the query filters and
     *                       order to the query builder
     */
    public static <T extends ComponentInterface> ApiQueryResults<T> query(
      QueryBuilder qb, final ApiQuery query, final Class<T> componentType,
      BiFunction<QueryBuilder, ApiQuery, QueryBuilder> filterFunction) {
        /* clients have to paginate to get counts */
        final boolean doTotalCount = query.getPage().isSet() && !query.isEmbed() && !query.excludeTotalCount();
        final boolean doFilterCount =
          query.getPage().isSet() && !query.getFilters().isEmpty() && !query.isEmbed();

        final Long totalCount =
          doTotalCount ? qb.getAggregateResult(Function.COUNT) : null;

        qb = filterFunction.apply(qb, query);

        final Long filterCount =
          doFilterCount ? qb.getAggregateResult(Function.COUNT) : totalCount;

        boolean nothing = query.getPage().isSet() && (query.getPage().getLimit() == 0);

        ItemMapping itemMapping = Optional.ofNullable(query.getItemMapping())
          .orElseGet(() -> componentType.getAnnotation(ItemMapping.class));
        String itemType = (itemMapping == null) ? null : itemMapping.value();
        boolean singleton = (itemMapping != null) && itemMapping.singleton();
        final List<T> results = nothing ? Collections.<T>emptyList()
          : qb.getComponentList(componentType, itemType, singleton);

        return new ApiQueryResults<>(results, filterCount, totalCount);
    }

    // as above, so below
    public static <T extends Finder> ApiQueryResults<T> queryFinder(
      final ApiQuery query,
      final QueryBuilder qb0,
      final Class<T> clas
    ) {
        for (final ApiFilter prefilter : query.getPrefilters()) {
            applyFilter(qb0, prefilter, query.getPropertyMappings());
        }

        final boolean doTotalCount = query.getPage().isSet() && !query.isEmbed() && !query.excludeTotalCount();
        final boolean doFilterCount =
          query.getPage().isSet() && !query.getFilters().isEmpty() && !query.isEmbed();

        final Long totalCount =
          doTotalCount ? qb0.getAggregateResult(Function.COUNT) : null;

        final var qb = ApiQuerySupport.getQueryBuilder(qb0, query);

        final Long filterCount =
          doFilterCount ? qb.getAggregateResult(Function.COUNT) : totalCount;

        boolean nothing = query.getPage().isSet() && (query.getPage().getLimit() == 0);

        final List<T> results = nothing ? Collections.emptyList() : qb.getItems().stream().map(item -> clas.cast(item.getFinder())).collect(Collectors.toList());

        return new ApiQueryResults<T>(results, filterCount, totalCount);
    }

    /**
     * Apply the predicates, orderings and pagination attributes of the given {@link
     * ApiQuery} to the given {@link QueryBuilder}. Prefilter predicates (predicates that
     * should be applied before the totalCount is calculated) are not applied to the given
     * {@link QueryBuilder}.
     *
     * @return the given {@link QueryBuilder} with the predicates, orderings and
     * pagination attributes of {@link ApiQuery} applied to it
     */
    public static QueryBuilder getQueryBuilder(QueryBuilder qb, ApiQuery query) {
        return getQueryBuilder(qb, query, false);
    }

    public static QueryBuilder getQueryBuilder(QueryBuilder qb, ApiQuery query, boolean applyPrefilters) {
        if (applyPrefilters) {
            for (final ApiFilter prefilter : query.getPrefilters()) {
                applyFilter(qb, prefilter, query.getPropertyMappings());
            }
        }

        final FilterOperator filterOp = query.getFilterOp();
        if (filterOp.isConjunction()) {
            for (ApiFilter filter : query.getFilters()) {
                applyFilter(qb, filter, query.getPropertyMappings());
            }
        } else {
            // filterOp.isDisjunction(), uh, let's hope no one adds a new enum entry
            List<Condition> conditions = new ArrayList<>();
            for (ApiFilter filter : query.getFilters()) {

                final Queryable queryable =
                  query.getPropertyMapping(filter.getProperty());
                conditions.add(getApplyFilterCondition(queryable, filter));
            }
            qb.addDisjunction0(conditions);
        }

        for (ApiOrder order : query.getOrders()) {
            applyOrder(qb, qb, order, query.getPropertyMappings());
        }

        if (query.getPage().isSet()) {
            qb.setFirstResult(query.getPage().getOffset());
            qb.setLimit(query.getPage().getLimit());
        }

        return qb;
    }

    private static void applyJoinFilter(final QueryBuilder qb, final ApiFilter filter, final Map<String, Queryable> propertyMappings) {
        final String property = StringUtils.substringBefore(filter.getProperty(), ".");
        final Queryable queryable = propertyMappings.get(property);
        if (queryable == null) {
            throw new ValidationException("filter#property", filter.getProperty(), "Unsupported filter property");
        }
        final var joinComponent = queryable.joinComponent();
        final Map<String, Queryable> subMap = ComponentSupport.getPojoDataMappings(joinComponent);
        final String subProperty = StringUtils.substringAfter(filter.getProperty(), ".");
        String itemType = joinComponent.getAnnotation(ItemMapping.class).value();
        QueryBuilder join = qb.getOrCreateJoinQuery(queryable.dataType(), itemType);
        final BaseApiFilter subFilter = new BaseApiFilter(subProperty, filter.getOperator(), filter.getValue());
        applyFilter(join, subFilter, subMap);
    }

    private static void applyFilter(final QueryBuilder qb,
                                    final ApiFilter filter,
                                    final Map<String, Queryable> propertyMappings) {
        if (filter.getProperty().contains(".")) {
            applyJoinFilter(qb, filter, propertyMappings);
        } else {
            final Queryable queryOptions = propertyMappings.get(filter.getProperty());
            if (queryOptions == null) {
                throw new ValidationException("filter#property", filter.getProperty(), "Unsupported filter property");
            }
            if (queryOptions.handler() == QueryHandler.class) {
                final Condition condition = getApplyFilterCondition(queryOptions, filter);
                if (condition.getComparison() == Comparison.in && condition.getList().contains(null)) {
                    condition.getList().remove(null);
                    qb.addDisjunction0(
                      List.of(
                        condition,
                        BaseCondition.getInstance(condition.getDataType(), Comparison.eq, null)
                      )
                    );
                } else {
                    qb.addCondition(condition);
                }
            } else if (queryOptions instanceof ApiQuery.ReifiedQueryable) {
                ((ApiQuery.ReifiedQueryable) queryOptions).handlerInstance().applyFilter(qb, filter);
            } else {
                try {
                    ComponentSupport.newInstance(queryOptions.handler()).applyFilter(qb, filter);
                } catch (ValidationException ve) {
                    throw ve;
                } catch (Throwable th) {
                    throw (ValidationException) new ValidationException
                      ("filter#property", filter.getProperty(), "Error applying filter property")
                      .initCause(th);
                }
            }
        }
    }

    public static Condition getApplyFilterCondition(final Queryable queryOptions, final ApiFilter filter) {
        if (queryOptions.handler() == QueryHandler.class) {
            return getApplyFilterCondition(queryOptions.dataType(), Arrays.asList(queryOptions.traits()), filter);
        } else {
            return ComponentSupport.newInstance(queryOptions.handler()).asCondition(filter);
        }
    }

    public static Condition getApplyFilterCondition(final String dataType, final List<Queryable.Trait> traits, final ApiFilter filter) {
        if (traits.contains(Queryable.Trait.NOT_FILTERABLE)) {
            throw new ValidationException("filter#property", filter.getProperty(),
              "Unsupported filter property");
        }

        final PredicateOperator operator = filter.getOperator();
        final Comparison cmp = (operator == null) ? null : operator.getComparison();
        final String value = (operator == null) ? filter.getValue() : operator.getSqlLiftStrategy().apply(filter.getValue());
        final boolean caseInsensitive = traits.contains(Queryable.Trait.CASE_INSENSITIVE);
        if (StringUtils.isEmpty(dataType)) {
            throw new IllegalStateException("Missing data type mapping for: " + filter.getProperty());
        }
        final DataFormat dataFormat =
          BaseOntology.getOntology().getDataFormat(dataType);
        var nullable = false;

        final Object arg;
        if (cmp == Comparison.in) {
            List<Object> list = new ArrayList<>();
            // TODO: Support escapes so you can have commas in strings in filter=x:in(a,b,c)
            for (String val : StringUtils.splitString(value)) {
                if ("null".equals(val)) {
                    nullable = true;
                } else {
                    list.add(parseString(val, dataFormat, caseInsensitive));
                }
            }
            arg = list;
        } else {
            arg = parseString(value, dataFormat, caseInsensitive);
        }

        final QueryStrategy queryStrategy = QUERY_STRATEGY.get(dataFormat);
        final Condition cond = queryStrategy.getCondition(dataType, cmp, filter.getProperty(), arg);
        cond.setFunction(caseInsensitive ? Function.LOWER.name() : null);
        if (traits.contains(Queryable.Trait.NO_STEMMING) && (cmp == Comparison.search)) {
            cond.setLanguage(Condition.LANGUAGE_SIMPLE);
        }
        if (nullable) cond.getList().add(null); // OMG
        return cond;
    }

    private static Object parseString(final String value, final DataFormat dataFormat, final boolean caseInsensitive) {
        try {
            switch (dataFormat) {
                case time:
                    return DATE_FORMAT.parse(value);
                case item:
                case number:
                    return NumberUtils.parseLong(value);
                case bool:
                    return BooleanUtils.toBoolean(value);
                case uuid:
                    return UUID.fromString(value);
                default:
                    return caseInsensitive ? StringUtils.lowerCase(value) : value;
            }
        } catch (Exception ex) {
            throw new ValidationException("filter#value", value, "Unsupported value");
        }
    }

    private static void applyOrder(QueryBuilder primary, QueryBuilder qb, ApiOrder order, Map<String, Queryable> propertyMappings) {
        if (order.getProperty().contains(".")) {
            applyJoinOrder(primary, qb, order, propertyMappings);
        } else {
            final Queryable queryOptions = propertyMappings.get(order.getProperty());
            if (queryOptions == null) {
                throw new ValidationException("order#property", order.getProperty(),
                  "Unsupported order property");
            }

            if (queryOptions.handler() == QueryHandler.class) {
                primary.addJoinOrder(qb, getApplyOrder(queryOptions, order));
            } else if (queryOptions instanceof ApiQuery.ReifiedQueryable) {
                ((ApiQuery.ReifiedQueryable) queryOptions).handlerInstance().applyOrder(qb, order);
            } else {
                try {
                    ComponentSupport.newInstance(queryOptions.handler()).applyOrder(qb, order);
                } catch (ValidationException ve) {
                    throw ve;
                } catch (Throwable th) {
                    throw (ValidationException) new ValidationException
                      ("order#property", order.getProperty(), "Error applying order property")
                      .initCause(th);
                }
            }
        }
    }

    private static void applyJoinOrder(final QueryBuilder primary, final QueryBuilder qb, final ApiOrder order, final Map<String, Queryable> propertyMappings) {
        final String property = StringUtils.substringBefore(order.getProperty(), ".");
        final Queryable queryable = propertyMappings.get(property);
        if (!isSortable(queryable)) {
            throw new ValidationException("order#property", order.getProperty(), "Unsupported order property");
        }

        final Map<String, Queryable> subMap = ComponentSupport.getPojoDataMappings(queryable.joinComponent());
        final String subProperty = StringUtils.substringAfter(order.getProperty(), ".");
        String itemType = queryable.joinComponent().getAnnotation(ItemMapping.class).value();

        QueryBuilder join = qb.getOrCreateJoinQuery(queryable.dataType(), itemType);

        final BaseApiOrder subOrder = new BaseApiOrder(subProperty, order.getDirection());
        applyOrder(primary, join, subOrder, subMap);
    }

    public static Order getApplyOrder(final Queryable queryOptions,
                                      final ApiOrder order) {
        if (!isSortable(queryOptions)) {
            throw new ValidationException("order#property", order.getProperty(),
              "Unsupported order property");
        }

        final List<Queryable.Trait> traits = Arrays.asList(queryOptions.traits());
        final Function function =
          traits.contains(Queryable.Trait.CASE_INSENSITIVE) ? Function.LOWER :
            Function.NONE;
        if (StringUtils.isEmpty(queryOptions.dataType())) {
            throw new IllegalStateException("Missing data type mapping for: " + order.getProperty());
        }
        final DataFormat dataFormat =
          BaseOntology.getOntology().getDataFormat(queryOptions.dataType());

        final QueryStrategy queryStrategy = QUERY_STRATEGY.get(dataFormat);
        return queryStrategy.getOrder(queryOptions.dataType(), function, order);
    }

    public static <T, U> ApiQueryResults<U> transform(ApiQueryResults<T> results, java.util.function.Function<T, U> transformer) {
        List<U> transformed = results.stream()
          .map(transformer)
          .collect(Collectors.toList());

        return new ApiQueryResults<>(transformed, results.getFilterCount(), results.getTotalCount());
    }

    public static boolean isSortable(@Nullable Queryable queryOptions) {
        if (queryOptions == null) {
            return false;
        }

        final List<Queryable.Trait> traits = Arrays.asList(queryOptions.traits());
        return !traits.contains(Queryable.Trait.NOT_SORTABLE);
    }

    public static <T> List<T> applyPage(List<T> elements, ApiPage page) {
        if (page.isSet()) {
            int startIndex = Math.min(page.getOffset(), elements.size());
            int maxIndex = Math.min(page.getOffset() + page.getLimit(), elements.size());
            return elements.subList(startIndex, maxIndex);
        } else {
            return elements;
        }
    }
}
