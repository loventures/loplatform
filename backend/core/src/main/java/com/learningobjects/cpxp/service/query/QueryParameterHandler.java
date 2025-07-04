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
import com.learningobjects.cpxp.service.data.DataSupport;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.persistence.Query;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Helper class containing all of the logic to bind in parameter values against
 * a live {@link Query} produced by {@link AbstractQueryBuilder}.
 */
class QueryParameterHandler {
    private static final Logger logger = Logger
            .getLogger(QueryParameterHandler.class.getName());

    final QueryDescription _description;

    QueryParameterHandler(QueryDescription description) {
        _description = description;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> setParameters(Query query) {
        QueryWrapper wrapper = new QueryWrapper(query);
        setItemParameters(wrapper);
        setDataTypeParameters(wrapper);
        setDisjunctionValueParameters(wrapper, _description._disjunctions, null);
        setJoinParameters(wrapper);
        setConditionValueParameters(wrapper, _description._conditions, null);
        if (!com.learningobjects.cpxp.service.query.Function.COUNT.equals(_description._function)) {
            if (_description._limit >= 0) {
                query.setMaxResults(_description._limit);
                wrapper.setParameter("#limit", _description._limit);
            }
            if (_description._firstResult >= 0) {
                query.setFirstResult(_description._firstResult);
                wrapper.setParameter("#offset", _description._firstResult);
            }
        }
        switch (_description._projection) {
          case ITEM_CONTEXT:
              wrapper.setParameter("itemContext", _description._itemContext);
              break;
          case CALENDAR_INFO:
              wrapper.setParameter("calendarStart", _description._calendarStart);
              wrapper.setParameter("calendarEnd", _description._calendarEnd);
              break;
        }
        Map<String, Object> parameters = wrapper.getParameters();
        if (!_description._subqueries.isEmpty()) {
            parameters = new TreeMap<>(parameters);
            for (QueryDescription.SubQuery qb: _description._subqueries) {
                Map<String, Object> subParams = qb.setParameters(query);
                parameters.putAll(subParams);
            }
            // CompositeMap doesn't toString()
         }

        return parameters;
    }

    @SuppressWarnings("unchecked")
    private void setJoinParameters(QueryWrapper wrapper) {
        int index = 0;
        for (Join join : _description._joinQueries) {
            final QueryDescription outerDescription;
            final QueryParameterHandler outerHandler;
            if (join instanceof Join.Left) {
                outerDescription = ((AbstractQueryBuilder) join.query())._description;
                outerHandler = ((AbstractQueryBuilder) join.query())._handler;
            } else {
                outerDescription = _description;
                outerHandler = this;
            }
            if (join.conditions().value().size() == 1) {
                /* also sad, see relevant hack in AQB */
                Conjunction conj = new Conjunction(outerDescription);
                join.conditions().asJava().get(0).forEach(conj::addCondition);
                outerHandler.setConditionValueParameters(wrapper, conj, "j_0_0");
            } else {
                outerHandler.setDisjunctionValueParameters(
                  wrapper,
                  new DisjunctionSeries(outerDescription, join.conditions().asJava()),
                  "j_" + index);
            }
            if (outerDescription._joinRoot
                    && !outerDescription.nativeQuery()
                    && !wrapper.getParameters().containsKey("root_id")) {
                wrapper.setParameter("root_id", _description._root.getId());
            }
            index++;
        }
    }

    Object getValue(Condition condition) {
        Object value = null;
        if ((condition.getComparison() != Comparison.in) &&
                   (condition.getComparison() != Comparison.notIn) &&
                   (condition.getComparison() != Comparison.equalsAny) &&
                   (condition.getComparison() != Comparison.unequalAll)) {
            String dt = condition.getDataType();
            switch (BaseOntology.getOntology().getDataFormat(dt)) {
            case string:
            case path:
            case text:
            case json:
            case tsvector:
                boolean lower = com.learningobjects.cpxp.service.query.Function.LOWER.name().equalsIgnoreCase(condition.getFunction());
                value = lower ? StringUtils.lowerCase(condition.getString()) : condition.getString();
                break;
            case uuid:
                value = UUID.fromString(condition.getString());
                break;
            case number:
                value = condition.getNumber();
                break;
            case DOUBLE:
                value = condition.getDouble();
                break;
            case time:
                if (_description.inDataCollection(dt)) {
                    value = DataSupport.toNumber(condition.getTime());
                } else {
                    value = condition.getTime();
                }
                break;
            case bool:
                if (_description.inDataCollection(dt)) {
                    value = DataSupport.toNumber(condition.getBoolean());
                } else {
                    value = condition.getBoolean();
                }
                break;
            case item:
                value = condition.getItem();
                break;
            }
        } else {
            value = condition.getList(); // this will be null, as desired, if it in inQuery
            if ((value != null) && com.learningobjects.cpxp.service.query.Function.LOWER.name().equalsIgnoreCase(condition.getFunction())) {
                value = ((List<String>) value).stream().map(String::toLowerCase).collect(Collectors.toList());
            } else if ((value != null) && condition.getComparison().expectsArray()) {
                List<Long> numbers = (List<Long>) value;
                // sadly JPA accepts neither a Long[] nor a postgresql java.sql.Array of longs
                value = "{" + StringUtils.join(numbers, ',') + "}";
            }
        }
        return value;
    }

    String getValueParameterName(String suffix) {
        return _description._valueLabel.concat(suffix);
    }

    private void setItemParameters(QueryWrapper query) {
        if ((_description._root != null)
                && (!_description._rootImplicit || _description._joinRoot)
                && !(_description._joinRoot && !_description.nativeQuery())) {
            query.setParameter("root", _description.getRootForBinding());
        }
        if ((_description._parent != null) && !_description._parentImplicit) {
            query.setParameter(_description._parentLabel, _description.getParentforBinding());
        }
        if ((_description._item != null)) {
            query.setParameter(_description._degenerateItemLabel, _description.getItemForBinding());
        }
        if ((_description._itemType != null) && !_description._itemTypeImplicit && (_description._entityDescription == null)) {
            query.setParameter(_description._typeLabel, _description._itemType);
        }
        if (_description._path != null) {
            query.setParameter(_description._pathLabel, _description._path);
        }
    }

    private void setDataTypeParameters(QueryWrapper query) {
        for (String dataType: _description.getDataTypes()) {
            String alias = _description.getAlias(dataType);
            query.setParameter(alias + "Type", dataType);
        }
    }

    private void setDisjunctionValueParameters(QueryWrapper query,
                                               DisjunctionSeries disjunctions,
                                               String prefix) {
        for (int index = 0; index < disjunctions.size(); ++index) {
            List<Conjunction> disjunction = disjunctions.get(index);
            for (int jndex = 0; jndex < disjunction.size(); ++jndex) {
                Conjunction conditions = disjunction.get(jndex);
                int kndex = 0;
                for (Condition condition: conditions) {
                    Object value = getValue(condition);
                    String name = (prefix == null ? "" : prefix + "_") + index + "_" + jndex + "_" + kndex;
                    setValueParameter(query, getValueParameterName(name), value);
                    ++ kndex;
                }
            }
        }
    }

    private void setConditionValueParameters(QueryWrapper query,
                                             Conjunction conditions,
                                             String prefix) {
        if (_description._having != null) {
            query.setParameter(_description._having._description._valueLabel.concat("Having"), _description._havingValue);
        }
        int index = 0;
        for (String dataType : conditions.getTypes()) {
            for (Condition condition : conditions.getConditions(dataType)) {
                Object value = getValue(condition);
                String name = getValueParameterName((prefix == null ? "" : prefix + "_") + index);
                setValueParameter(query, name, value);
                ++ index;
            }
        }
    }

    private void setValueParameter(QueryWrapper query, String prefix, Object value) {
        if (value instanceof List) {
            /* This is to work around a bug in Hibernate where X IN :list breaks with..
             * org.hibernate.hql.ast.QuerySyntaxException: unexpected AST node: {vector}
             * See AbstractConjunctionBuilder#buildListRightHand */
            List list = (List) value;
            for (int index = 0; index < list.size(); ++ index) {
                query.setParameter(prefix + "_" + index, list.get(index));
            }
            /* End workaround. */
        } else if (value != null) {
            query.setParameter(prefix, value);
        }
    }

    private class QueryWrapper {
        private final Query _wrapped;

        private final Map<String, Object> _parameters = new TreeMap<String, Object>();

        private QueryWrapper(Query wrapped) {
            _wrapped = wrapped;
        }

        void setParameter(String name, Object value) {
            _parameters.put(name, value);
            if (!name.startsWith("#"))
                _wrapped.setParameter(name, value);
        }

        Map<String, Object> getParameters() {
            return Collections.unmodifiableMap(_parameters);
        }
    }
}
