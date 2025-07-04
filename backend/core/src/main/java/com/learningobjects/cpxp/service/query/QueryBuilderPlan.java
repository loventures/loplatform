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
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.util.StringUtils;

import java.util.List;

/**
 * This code renders a query description into a more human readable format, to
 * help with instrumentation for purposes of load, performance and capacity planning.
 */
class QueryBuilderPlan {

    private static final QueryBuilderPlan SINGLETON = new QueryBuilderPlan();

    private QueryBuilderPlan() {
        // enforce Singleton
    }

    static String explain(QueryDescription description) {
        return SINGLETON.explainPlan(description);
    }

    private String explainPlan(QueryDescription description) {
        StringBuilder kb = new StringBuilder();

        if (!description._inclusive) {
            kb.append("\nnot inclusive");
        }
        if (!description._initialQueries.isEmpty()) {
            kb.append("\ninitial queries");
            for (QueryDescription.SubQuery qb : description._initialQueries) {
                kb.append("\n\t").append(executeNested((QueryBuilder) qb));
            }
        } else if (description._parent != null) {
            kb.append("\nparent = <parent>");
        } else if (description._root != null) {
            kb.append("\nroot = <root>");
        } else {
            kb.append("\nignore tree");
        }
        if (description._itemType != null) {
            kb.append("\nitem type = ").append(description._itemType);
        } else {
            kb.append("\nitem type = any");
        }

        if (description._disjunctions.size() > 0) {
            kb.append("\nwith disjunctions");
        }
        for (int index = 0; index < description._disjunctions.size(); ++index) {
            List<Conjunction> disjunction = description._disjunctions.get(index);
            if (disjunction.isEmpty()) {
                continue;
            }
            kb.append("\n\t(");
            for (int jndex = 0; jndex < disjunction.size(); ++jndex) {
                if (jndex > 0) {
                    kb.append("\n\t OR ");
                }
                Conjunction conditions = disjunction.get(jndex);
                for (Condition condition : conditions) {
                    kb.append("\n\tAND ");
                    appendConditionTest(condition, kb);
                }
            }
            kb.append(')');
        }

        // iterate conjunctions
        if (!description._conditions.isEmpty()) {
            kb.append("\nwith conditions");
        }
        appendConjunction(description._conditions, kb);

        if (description._groupType != null) {
            kb.append("\ngroup by ").append(description._groupType);
        }

        if (!description._orders.isEmpty()) {
            kb.append("\norder by");
        }
        for (int i = 0; i < description._orders.size(); ++i) {
            Order order = description._orders.get(i);
            if (order.getQuery() != null) {
                kb.append(executeNested((QueryBuilder) order.getQuery()));
                if (order.getType() != null) {
                    kb.append(' ').append(order.getType());
                }
            } else if (order.getType() == null) {
                kb.append("\n\t").append(order.getFunction()).append("(i)");
            } else if (order.getFunction() == null) {
                kb.append("\n\t").append(order.getType());
            } else {
                kb.append("\n\t").append(order.getFunction()).append('(').append(
                        order.getType()).append(')');
            }
            kb.append(' ').append(order.getDirection().sql());
        }

        appendExtra(description, kb);

        return kb.toString();
    }

    private void appendConjunction(Conjunction conjunction, StringBuilder kb) {
        for (String dataType : conjunction.getTypes()) {
            for (Condition condition : conjunction.getConditions(dataType)) {
                kb.append("\n\tAND ");
                appendConditionTest(condition, kb);
            }
        }
    }

    private void appendConditionTest(Condition condition, StringBuilder kb) {
        if (condition.getDataType() == null) {
            QueryBuilder query = condition.getQuery();
            kb.append("in ").append(executeNested(query));
        } else {
            String function = condition.getFunction();

            if (function == null) {
                kb.append(condition.getDataType());
            } else {
                kb.append(function).append(':').append(
                        condition.getDataType());
            }
            kb.append(' ').append(condition.getComparison()).append(' ').append(getValue(
                    condition));
        }
    }

    private String getValue(Condition condition) {
        String value = null;
        if ((condition.getComparison() == Comparison.in) || (condition.getComparison() == Comparison.notIn)) {
            if (condition.getList() != null) {
                value = "<list value>";
            } else {
                value = executeNested(condition.getQuery());
            }
        } else if (condition.getDataType().startsWith("#")) {
            value = "<meta value>";
        } else {
            String dtName = condition.getDataType();
            DataTypedef dt = BaseOntology.getOntology().getDataType(dtName);
            switch (dt.value()) {
                case string:
                    value = "<string value>";
                    break;
                case number:
                    value = "<number value>";
                    break;
                case time:
                    value = "<time value>";
                    break;
                case bool:
                    value = "<boolean value>";
                    break;
                case item:
                    value = "<item value>";
                    break;
            }
        }
        return value;
    }

    private void appendExtra(QueryDescription description, StringBuilder kb) {
        if (description._having == null && description._limit < 0 && description._firstResult < 0 && description._dataProjection == null
                && description._itemContext == null && description._calendarStart == null && description._function == null && !description._distinct) {
            return;
        }
        kb.append("\nqualifiers on results");
        if (description._having != null) {
            kb.append("\n\thaving ").append(description._havingCmp).append(
                    " <value>");
        }
        if (description._limit >= 0) {
            kb.append("\n\tlimit <limit>");
        }

        if (description._firstResult >= 0) {
            kb.append("\n\tfirst <first>");
        }

        if (description._dataProjection != null || description._itemContext != null || description._calendarStart != null) {
            kb.append("\n\tprojection ");
        }
        if (description._dataProjection != null) {
            kb.append(StringUtils.join(description._dataProjection, ","));
        } else if (description._itemContext != null) {
            kb.append(description._itemContext);
        } else if (description._calendarStart != null) {
            kb.append(description._calendarStart.getTime());
            kb.append('-').append(description._calendarEnd.getTime());
        }
        if (description._function != null) {
            kb.append("\nfunction ").append(description._function);
        }
        if (description._distinct) {
            kb.append("\ndistinct");
        }
    }

    private String executeNested(QueryBuilder query) {
        return executeNested((QueryDescription.SubQuery) query);
    }

    private String executeNested(QueryDescription.SubQuery impl) {
        String inner = explain(impl.getDescription());
        inner = inner.replaceAll("^\n", "");
        inner = inner.replaceAll("\n", "\n\t\t");
        return "[".concat(inner).concat("]");
    }
}
