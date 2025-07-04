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

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.web.util.ExtendedISO8601DateFormat;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.de.web.QueryHandler;
import com.learningobjects.de.web.Queryable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Predicate;

/**
 * Encapsulates filtering, ordering and paging. Immutable, use {@link Builder} to make
 * new ones from existing ones.
 */
public class ApiQuery {

    /**
     * A no-op ApiQuery (won't filter or order or anything)
     */
    public static final ApiQuery ALL = new Builder().build();

    /**
     * An ApiQuery that will just count the number of entities.
     */
    public static final ApiQuery COUNT = ALL.toCountQuery();

    /**
     * Logical operator on all {@link #_filters} (e.g. AND/OR them)
     */
    private final FilterOperator _filterOp;

    /**
     * Prefilter predicates that will be applied to the query. Prefilter predicates are
     * applied before the total count is counted. Prefilter predicates are always
     * conjoined (and-ed).
     */
    private final List<ApiFilter> _prefilters;

    /**
     * Non-prefilter predicates that will be applied to the query. Non-prefilter
     * predicates are applied after the total count is counted.
     */
    private final List<ApiFilter> _filters;

    /**
     * Orderings to apply to the query
     */
    private final List<ApiOrder> _orders;

    /**
     * Pagination attributes to apply to the query
     */
    private final ApiPage _page;

    /**
     * Hint about embeds, useful for performance reasons
     */
    private final Set<String> _embeds;

    /**
     * Item mapping for query support
     */
    private final ItemMapping _itemMapping;

    /**
     * All the component-property-name-to-filterer-sorter mappings that this query can use
     */
    private final Map<String, Queryable> _propertyMappings;

    /**
     * Whether this is an embed.
     */
    private final boolean _embed;

    /**
     * Whether or not to skip doing a total count (used for performance reasons)
     */
    private final boolean _excludeTotalCount;

    /**
     * Use {@link Builder} to create an {@link ApiQuery}
     */
    private ApiQuery(final Builder builder) {
        this._filterOp = builder.filterOp;
        this._prefilters = builder.prefilters;
        this._filters = builder.filters;
        this._orders = builder.orders;
        this._page = builder.page;
        this._itemMapping = builder.itemMapping;
        this._propertyMappings = builder.propertyMappings;
        this._embeds = builder.embeds;
        this._embed = builder.embed;
        this._excludeTotalCount = builder.excludeTotalCount;
    }

    /**
     * @return logical operator on all {@link #_filters} (e.g. AND/OR them)
     */
    @Nonnull
    public FilterOperator getFilterOp() {
        return _filterOp;
    }

    /**
     * Returns prefilter predicates that will be applied to the query. Prefilter
     * predicates are applied before the total count is counted. Prefilter predicates
     * are always conjoined (and-ed).
     *
     * @return prefilter predicates to apply
     */
    @Nonnull
    public List<ApiFilter> getPrefilters() {
        return _prefilters;
    }

    /**
     * Returns non-prefilter predicates that will be applied to the query. Non-prefilter
     * predicates are applied after the total count is counted.
     *
     * @return predicates to apply
     */
    @Nonnull
    public List<ApiFilter> getFilters() {
        return _filters;
    }

    /**
     * @return prefilters and non-prefilters
     */
    @Nonnull
    public Iterable<ApiFilter> getAllFilters() {
        return Iterables.concat(_prefilters, _filters);
    }

    /**
     * @return orderings to apply to a {@link QueryBuilder}
     */
    @Nonnull
    public List<ApiOrder> getOrders() {
        return _orders;
    }

    /**
     * @return pagination attributes to apply to a {@link QueryBuilder}
     */
    @Nonnull
    public ApiPage getPage() {
        return _page;
    }

    /**
     * @return the properties that will be embedded
     */
    @Nonnull
    public Set<String> getEmbeds() {
        return _embeds;
    }

    /**
     * @return whether this request is an embed
     */
    public boolean isEmbed() {
        return _embed;
    }

    public boolean excludeTotalCount() {
        return _excludeTotalCount;
    }

    /**
     * The mappings from component properties to datastore properties.
     *
     * @return all the component-property-name-to-filterer-sorter mappings that this query
     * can use
     */
    public Map<String, Queryable> getPropertyMappings() {
        return _propertyMappings;
    }

    @Nullable
    public final Queryable getPropertyMapping(final String propertyName) {
        return _propertyMappings.get(propertyName);
    }

    @Nonnull
    public Queryable getRequiredPropertyMapping(final String propertyName) throws ValidationException {
        Queryable queryable = getPropertyMapping(propertyName);
        if (queryable == null) {
            throw new ValidationException("filter#property", propertyName, "Unsupported filter property");
        }
        return queryable;
    }

    /**
     * The component item mapping.
     * @return te component item mapping
     */
    @Nullable
    public final ItemMapping getItemMapping() {
        return _itemMapping;
    }

    static final DateFormat dateFormat = new ExtendedISO8601DateFormat();

    public static class Builder {

        private FilterOperator filterOp;
        private List<ApiFilter> prefilters;
        private List<ApiFilter> filters;
        private List<ApiOrder> orders;
        private ApiPage page;
        private Set<String> embeds;
        private boolean embed;
        private ItemMapping itemMapping;
        private Map<String, Queryable> propertyMappings;
        private boolean excludeTotalCount;

        /**
         * Build from scratch
         */
        public Builder() {
            this(FilterOperator.AND, new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(),
                 BaseApiPage.DEFAULT_PAGE, new HashSet<>(), false,
                 new HashMap<>(), null,
              false);
        }

        /**
         * Build from existing query
         */
        public Builder(final ApiQuery query) {
            this(query.getFilterOp(), new ArrayList<>(query.getPrefilters()),
                    new ArrayList<>(query.getFilters()),
                    new ArrayList<>(query.getOrders()), query.getPage(),
                    new HashSet<>(query.getEmbeds()), query.isEmbed(),
                    new HashMap<>(query.getPropertyMappings()),
                    query.getItemMapping(),
                    query.excludeTotalCount());
        }

        /**
         * Build from existing query, but without its property mappings
         */
        public static Builder unmapped(final ApiQuery query) {
            return new Builder(query.getFilterOp(), new ArrayList<>(query.getPrefilters()),
                    new ArrayList<>(query.getFilters()),
                    new ArrayList<>(query.getOrders()), query.getPage(),
                    new HashSet<>(query.getEmbeds()), false,
                    new HashMap<>(), null,
                    query.excludeTotalCount());
        }

        private Builder(final FilterOperator filterOp, final List<ApiFilter> prefilters,
                        final List<ApiFilter> filters, final List<ApiOrder> orders,
                        final ApiPage page, final Set<String> embeds, final boolean embed,
                        final Map<String, Queryable> propertyMappings,
                        final ItemMapping itemMapping,
                        final boolean excludeTotalCount) {
            this.filterOp = filterOp;
            this.prefilters = prefilters;
            this.filters = filters;
            this.orders = orders;
            this.page = page;
            this.embeds = embeds;
            this.embed = embed;
            this.propertyMappings = propertyMappings;
            this.itemMapping = itemMapping;
            this.excludeTotalCount = excludeTotalCount;
        }

        public Builder setFilterOp(final FilterOperator filterOp) {
            this.filterOp = filterOp;
            return this;
        }

        public Builder addPrefilter(final String property, final PredicateOperator op, final String value) {
            return addPrefilter(new BaseApiFilter(property, op, value));
        }

        public Builder addPrefilter(final ApiFilter filter) {
            this.prefilters.add(filter);
            return this;
        }

        public Builder addPrefilters(final Iterable<? extends ApiFilter> prefilters) {
            final List<ApiFilter> ps = ImmutableList.copyOf(prefilters); // defensive copy
            this.prefilters.addAll(ps);
            return this;
        }

        public Builder addPrefilter(final String property, final PredicateOperator op, final Collection<Long> ids) {
            return addPrefilter(new BaseApiFilter(property, op, StringUtils.join(ids, ",")));
        }

        public Builder addPrefilter(final String property, final PredicateOperator op, final Date date) {
            return addPrefilter(new BaseApiFilter(property, op, dateFormat.format(date)));
        }

        /**
         * Sets the prefilters, wiping out any previous filters
         * @param prefilters the new set of filters
         * @return
         */
        public Builder setPrefilters(final Iterable<ApiFilter> prefilters) {
            this.prefilters = new ArrayList<>();
            return addPrefilters(prefilters);
        }

        public Builder addFilter(final String property, final PredicateOperator op, final String value) {
            return addFilter(new BaseApiFilter(property, op, value));
        }

        public Builder addFilter(final String property, final String value) {
            return addFilter(new BaseApiFilter(property, null, value));
        }

        public Builder addFilter(final ApiFilter filter) {
            this.filters.add(filter);
            return this;
        }

        public Builder addFilters(final Iterable<ApiFilter> filters) {
            final List<ApiFilter> fs = ImmutableList.copyOf(filters); // defensive copy
            this.filters.addAll(fs);
            return this;
        }

        public Builder addFilter(final String property, final PredicateOperator op, final Collection<Long> ids) {
            return addFilter(new BaseApiFilter(property, op, StringUtils.join(ids, ",")));
        }

        public Builder addFilter(final String property, final PredicateOperator op, final Date date) {
            return addFilter(new BaseApiFilter(property, op, dateFormat.format(date)));
        }


        /**
         * Sets the filters, wiping out any previous filters
         * @param filters the new set of filters
         * @return
         */
        public Builder setFilters(final Iterable<ApiFilter> filters) {
            this.filters = new ArrayList<>();
            return addFilters(filters);
        }

        /**
         * Removes the filters with the given names from the filter collection of the ApiQuery to be built.
         * This is useful when you mostly want to use the out-of-the-box filters intuited by the SRS framework,
         * but also define a limited number of filters you want to handle yourself.
         * @param collector a container for the ApiFilter objects which are extracted (indexed by property name); handle these yourself.
         * @param customFilterNames the names of the filters you are declaring you want to handle yourself.
         * @return
         */
        public Builder removeCustomFiltersByPropertyName(final Multimap<String, ApiFilter> collector, final String... customFilterNames) {
            return setFilters(removeCustomFiltersByPropertyName(collector, filters, customFilterNames));
        }

        /**
         * Removes the prefilters with the given names from the prefilter collection of the ApiQuery to be built.
         * This is useful when you mostly want to use the out-of-the-box prefilters intuited by the SRS framework,
         * but also define a limited number of prefilters you want to handle yourself.
         * @param collector a container for the ApiFilter objects which are extracted (indexed by property name); handle these yourself.
         * @param customPrefilterNames the names of the prefilters you are declaring you want to handle yourself.
         * @return
         */
        public Builder removeCustomPrefiltersByPropertyName(final Multimap<String, ApiFilter> collector, final String... customPrefilterNames) {
            return setPrefilters(removeCustomFiltersByPropertyName(collector, prefilters, customPrefilterNames));
        }

        public Optional<ApiFilter> removeFilter(final String name) {
            final Predicate<ApiFilter> predicate = f -> f.getProperty().equals(name);
            Optional<ApiFilter> match = filters.stream().filter(predicate).findFirst();
            filters.removeIf(predicate);
            return match;
        }

        public Optional<ApiFilter> removePrefilter(final String name) {
            final Predicate<ApiFilter> predicate = f -> f.getProperty().equals(name);
            Optional<ApiFilter> match = prefilters.stream().filter(predicate).findFirst();
            prefilters.removeIf(predicate);
            return match;
        }

        private static ImmutableList<ApiFilter> removeCustomFiltersByPropertyName(Multimap<String, ApiFilter> collector, List<ApiFilter> elements, final String[] customFilterNames) {
            ImmutableListMultimap<Boolean, ApiFilter> filtersByCustom = Multimaps.index(elements, new Function<ApiFilter, Boolean>() {
                @Override
                public Boolean apply(ApiFilter input) {
                    return Arrays.asList(customFilterNames).contains(input.getProperty());
                }
            });
            ImmutableListMultimap<String, ApiFilter> customFiltersByProperty = Multimaps.index(filtersByCustom.get(Boolean.TRUE), new Function<ApiFilter, String>() {
                @Override
                public String apply(ApiFilter input) {
                    return input.getProperty();
                }
            });
            collector.putAll(customFiltersByProperty);
            return filtersByCustom.get(Boolean.FALSE);
        }


        public Builder addOrder(final String property, final OrderDirection direction) {
            return addOrder(new BaseApiOrder(property, direction));
        }

        public Builder addOrder(final ApiOrder order) {
            this.orders.add(order);
            return this;
        }

        public Builder addOrders(final Iterable<ApiOrder> orders) {
            final List<ApiOrder> os = ImmutableList.copyOf(orders); // defensive copy
            this.orders.addAll(os);
            return this;
        }

        public Builder setPage(final int offset, final int limit) {
            return setPage(new BaseApiPage(offset, limit));
        }

        public Builder setPage(final ApiPage page) {
            this.page = page;
            return this;
        }

        public Builder setExcludeTotalCount(final boolean excludeTotalCount) {
            this.excludeTotalCount = excludeTotalCount;
            return this;
        }

        public Builder addEmbed(final String embed) {
            this.embeds.add(embed);
            return this;
        }

        public Builder setEmbedded(final boolean embed) {
            this.embed = embed;
            return this;
        }

        public Builder addEmbeds(final Iterable<String> embeds) {
            final List<String> os = ImmutableList.copyOf(embeds); // defensive copy
            this.embeds.addAll(os);
            return this;
        }

        public Builder setItemMapping(final ItemMapping itemMapping) {
            this.itemMapping = itemMapping;
            return this;
        }

        public Builder addPropertyMapping(final String name, final Queryable queryable) {
            this.propertyMappings.put(name, queryable);
            return this;
        }

        public Builder addPropertyMapping(final String name, final String dataType) {
            this.addPropertyMapping(name, dataTypeQueryable(name, dataType));
            return this;
        }

        public Builder addPropertyMapping(final String name, final QueryHandler handler) {
            this.addPropertyMapping(name, handlerQueryable(name, handler));
            return this;
        }

        /**
         * You can lookup the {@link Queryable}s for a type using {@link
         * ComponentSupport#getPojoDataMappings(Class)}, and then pass them in here.
         */
        public Builder addPropertyMappings(
                final Map<String, Queryable> propertyMappings) {
            this.propertyMappings.putAll(propertyMappings);
            return this;
        }

        public Builder addPropertyMappings(
                final Class<?> clazz) {
            itemMapping = clazz.getAnnotation(ItemMapping.class);
            final Map<String, Queryable> pojoDataMappings =
                    ComponentSupport.getPojoDataMappings(clazz);
            return addPropertyMappings(pojoDataMappings);
        }

        public ApiQuery build() {

            prefilters = Collections.unmodifiableList(prefilters);
            filters = Collections.unmodifiableList(filters);
            orders = Collections.unmodifiableList(orders);
            propertyMappings = Collections.unmodifiableMap(propertyMappings);

            return new ApiQuery(this);
        }
    }

    /**
     * Reduces this API query to a count query.
     * @return a new query containing all filters from this query as prefilters and a zero-limit page
     */
    public ApiQuery toCountQuery() {
        return new Builder().addPrefilters(getAllFilters()).setPage(0, 0).build();
    }

    public static ApiQuery byId(final Long id, Class<?> clazz) {
        return new Builder()
            .addFilter(new BaseApiFilter("id", PredicateOperator.EQUALS, id.toString()))
            .addPropertyMappings(clazz)
            .addPropertyMapping("id", ID_QUERY)
            .build();
    }

    public static ApiQuery byId(final Long id) {
        final Builder builder = new Builder();
        final BaseApiFilter filter = new BaseApiFilter("id", PredicateOperator.EQUALS, id.toString());
        builder.addPrefilter(filter);
        return builder.addPropertyMapping("id", ID_QUERY).build();
    }

    public static ApiQuery byIds(final String ids) {
        final Builder builder = new Builder();
        final BaseApiFilter filter = new BaseApiFilter("id", PredicateOperator.IN, ids);
        builder.addPrefilter(filter);
        return builder.addPropertyMapping("id", ID_QUERY).build();
    }

    private static final Queryable ID_QUERY = new Queryable() {
            @Override
            public String name() {
                return "id";
            }

            @Override
            public String dataType() {
                return DataTypes.META_DATA_TYPE_ID;
            }

            @Override
            public Class<? extends ComponentInterface> joinComponent() {
                return ComponentInterface.class;
            }

            @Override
            public Class<? extends QueryHandler> handler() {
                return QueryHandler.class;
            }

            @Override
            public Trait[] traits() {
                return new Trait[0];
            }

            @Override
            public Class<Queryable> annotationType() {
                return Queryable.class;
            }
        };

    private static Queryable dataTypeQueryable(String name, String dataType) {
        return new Queryable() {
            public String name() { return name; }
            public String dataType() { return dataType; }
            public Class<? extends ComponentInterface> joinComponent() { return ComponentInterface.class; }
            public Class<? extends QueryHandler> handler() { return QueryHandler.class; }
            public Trait[] traits() { return Trait.NO_TRAITS; }
            public Class<? extends Annotation> annotationType() { return Queryable.class; }
        };
    }

    public interface ReifiedQueryable extends Queryable {
        QueryHandler handlerInstance();
    }

    private static Queryable handlerQueryable(final String name, final QueryHandler handler) {
        return new ReifiedQueryable() {
            public String name() { return name; }
            public String dataType() { return ""; }
            public Class<? extends ComponentInterface> joinComponent() { return ComponentInterface.class; }
            public Class<? extends QueryHandler> handler() { return handler.getClass(); }
            public QueryHandler handlerInstance() { return handler; }
            public Trait[] traits() { return Trait.NO_TRAITS; }
            public Class<? extends Annotation> annotationType() { return Queryable.class; }
        };
    }
}
