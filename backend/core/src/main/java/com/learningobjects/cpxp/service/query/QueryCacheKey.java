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
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class QueryCacheKey {

    private final QueryDescription _description;

    private String _cacheKey;

    public QueryCacheKey(QueryDescription description) {
        _description = description;
    }

    public String getKey() {
        if (_cacheKey != null) {
            return _cacheKey;
        }
        // this key is imperfect. for example, it doesn't obey the sequence of orders in a join
        // query. but it is likely suffficient for now.
        List<Order> orders = _description._orderSequence.stream()
                                                        .map(Pair::getRight)
                                                        .collect(Collectors.toList());


        StringBuilder kb = new StringBuilder();
        kb.append(
                exclusivityPrefix(_description._inclusive)
                + subQueries(_description._initialQueries)
                + subQueries(_description._existsQueries)
                + id(getParent(), "/").orElse(id(getRoot(), "//").orElse("//"))
                + Optional.ofNullable(_description._itemType).orElse("*")
                + BRACKETS.prefixed(_description._excludePath ? "!" : "").around(_description._path).orElse("")
                + joinOptionals(_description._disjunctions, disjunction -> PARENS.around(join(" OR ", disjunction, this::conjunctionClause, "")))
                + conjunctionClause(_description._conditions)
                + BRACKETS.prefixed("/").around("/", _description._groupType).orElse("")
                + joinOrderedItems("", orders, this::order, "")
                + join(_description._joinQueries, this::writeQueryEntry));
        appendExtra(kb);
        _cacheKey = kb.toString();
        return _cacheKey;
    }

    protected Id getParent() {
        return _description._parent;
    }

    protected Id getRoot() {
        return _description._root;
    }

    //overriden in partitioning subclasses to ignore tests only valid for the aggregate superquery
    protected Optional<String> conditionTest(Condition condition) {
        String result;
        if (condition.getDataType() == null) {
            QueryBuilder query = condition.getQuery();
            result = condition.getComparison() + BRACKETS.around(query.getCacheKey()).orElse("");
        } else {
            final String property = Joiner.on(":").skipNulls().join(condition.getFunction(), condition.getDataType());
            final String field =
              condition.getField() == null || condition.getField().isEmpty()
                ? ""
                : "->" + StringUtils.join(condition.getField(), "->");
            result = Joiner.on(" ").skipNulls().join(property + field, condition.getComparison(), getValue(condition));
        }
        return Optional.of("/" + result);
    }

    private String subQueries(List<QueryDescription.SubQuery> queries) {
        return joinOptionals("", queries, qb -> BRACKETS.around(qb.getCacheKey()), "/");
    }

    private String conjunctionClause(Conjunction conjunction) {
        return joinOptionals(conjunction.conjoinedConditions(), this::conditionTest);
    }

    private String exclusivityPrefix(boolean inclusive) {
        return inclusive ? "" : "!";
    }

    private String order(Order order) {
        final QueryBuilder query = order.getQuery();
        final String type = order.getType();
        final com.learningobjects.cpxp.service.query.Function function = order.getFunction();

        String result;
        if (query != null) {
            result = Joiner.on("/").skipNulls().join(PARENS.around(query.getCacheKey()).orElse(null), type);
        } else if (function == null) {
            result = type;
        } else {
            final String typeIndicator = Optional.ofNullable(type).orElse("i");
            result = function + PARENS.around(typeIndicator).orElse("");
        }
        return "/" + result + " " + order.getDirection().sql();
    }

    private String writeQueryEntry(Join entry) {
        StringBuilder builder = new StringBuilder("((");
        builder
          .append(entry.leftDataType())
          .append('(').append(entry.joinWord().trim()).append(')')
          .append(entry.rightDataType());
        if (entry.conditions().value().nonEmpty()) {
            builder.append('{');
            List<String> conjunctions =
              entry.conditions().asJava().stream()
                .map(conj -> joinOptionals(conj, this::conditionTest))
                .collect(Collectors.toList());
            builder.append(String.join("+", conjunctions));
            builder.append('}');
        }
        return builder
          .append(":[")
          .append(entry.query().getCacheKey())
          .append("]))")
          .toString();
    }

    private Optional<String> id(Id id, String suffix) {
        if (id != null) {
            return Optional.of(id.getId() + suffix);
        }
        return Optional.empty();
    }

    Enclosing PARENS = new Enclosing("(", ")");
    Enclosing BRACKETS = new Enclosing("[", "]");

    private <T> String joinOptionals(Iterable<T> items, com.google.common.base.Function<T, Optional<String>> transformer) {
        return joinOptionals("", items, transformer, "");
    }

    private <T> String joinOptionals(String delimiter, Iterable<T> items, com.google.common.base.Function<T, Optional<String>> transformer, String withSuffix) {
        return joinParts(delimiter, StreamSupport.stream(items.spliterator(), false).map(transformer::apply).flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(Collectors.toList()), withSuffix);
    }

    private <T> String join(String delimeter, Iterable<T> items, com.google.common.base.Function<T, String> transformer, String withSuffix) {
        return joinParts(delimeter, StreamSupport.stream(items.spliterator(), false).map(transformer::apply).collect(Collectors.toList()), withSuffix);
    }

    private <T> String joinParts(String delimeter, Iterable<String> parts, String withSuffix) {
        return Joiner.on(delimeter).skipNulls().join(Ordering.natural().sortedCopy(parts)) + (Iterables.isEmpty(parts) ? "" : withSuffix);
    }

    private <T> String join(Iterable<T> items, com.google.common.base.Function<T, String> transformer) {
        return join("", items, transformer, "");
    }

    private <T> String joinOrderedItems(String delimiter, Collection<T> items, com.google.common.base.Function<T, String> transformer, String withSuffix) {
        List<String> parts = items.stream()
                                  .map(transformer::apply)
                                  .collect(Collectors.toList());

        return Joiner.on(delimiter).skipNulls().join(parts) + (Iterables.isEmpty(parts) ? "" : withSuffix);
    }

    private class Enclosing {
        private String _start, _end, _prefix = "";
        public Enclosing(String start, String end) {
            _start = start;
            _end = end;
        }
        Enclosing prefixed(String prefix) {
            Enclosing e = new Enclosing(_start, _end);
            e._prefix = prefix;
            return e;
        }
        Optional<String> around(String contents) {
            if (StringUtils.isEmpty(contents)) {
                return Optional.empty();
            }
            return Optional.of(_start + _prefix + contents +  _end);
        }
        Optional<String> around(String delimeter, List<?> parts) {
            if (parts == null || parts.isEmpty()) return Optional.empty();
            return around(Joiner.on(delimeter).skipNulls().join(parts));
        }
    }

    @VisibleForTesting
    protected Object getValue(Condition condition) {
        Object value = null;
        if ((condition.getComparison() == Comparison.in) ||
                    (condition.getComparison() == Comparison.notIn)) {
            if (condition.getList() != null) {
                value = condition.getList();  //TODO this should ideally be sorted
            } else {
                value = '[' + condition.getQuery().getCacheKey() + ']';
            }
        } else {
            String dt = condition.getDataType();
            switch (BaseOntology.getOntology().getDataFormat(dt)) {
            case string:
            case path:
            case text:
            case json:
            case tsvector:
                value = '\'' + condition.getString() + '\''; // TODO: escape value?
                break;
            case number:
                value = condition.getNumber();
                break;
            case DOUBLE:
                value = condition.getDouble();
                break;
            case time:
                value = DataSupport.toNumber(condition.getTime());
                break;
            case bool:
                value = DataSupport.toNumber(condition.getBoolean());
                break;
            case item:
                value = condition.getItem();
                break;
            }
        }
        return value;
    }

    private void appendExtra(StringBuilder kb) {
        if (_description._having != null) {
            kb.append("/qb ").append(_description._havingCmp).append(' ').append(_description._havingValue);
        }
        if (_description._limit >= 0) {
            kb.append('/').append(_description._limit);
        }

        if (_description._firstResult >= 0) {
            kb.append('/').append(_description._firstResult);
        }

        kb.append('/').append(_description._projection);
        if (_description._dataProjection != null) {
            kb.append(':');
            boolean first = true;
            for (DataProjection dp : _description._dataProjection) {
                if (first) {
                    first = false;
                } else {
                    kb.append(",");
                }
                if (dp.getFunction() == null) {
                    kb.append(dp.getType());
                } else {
                    kb.append(dp.getFunction()).append('(').append(dp.getType()).append(')');
                }
            }
        } else if (_description._itemContext != null) {
            kb.append(':').append(_description._itemContext);
        } else if (_description._calendarStart != null) {
            kb.append(':').append(_description._calendarStart.getTime());
            kb.append('-').append(_description._calendarEnd.getTime());
        }
        if (_description._function != null) {
            kb.append('/').append(_description._function);
        }
        if (_description._distinct) {
            kb.append("/distinct");
        }
    }

    protected boolean isInConditionOn(Condition condition, String field) {
        return field != null && field.equals(condition.getDataType()) && condition.getComparison() == Comparison.in;
    }
}
