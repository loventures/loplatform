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

import com.learningobjects.cpxp.service.ServiceContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This implementation uses subselects with having to try to match conditions
 * more efficiently than a series of joins.
 */
public class SubselectQueryBuilder extends AbstractQueryBuilder {
    private static final ConjunctionBuilder CONJUNCTION_BUILDER = new SubselectConjunctionBuilder();

    public SubselectQueryBuilder(ServiceContext serviceContext, QueryCache queryCache) {
        super(serviceContext, queryCache);
    }

    @Override
    ConjunctionBuilder getConjunctionBuilder() {
        return CONJUNCTION_BUILDER;
    }

    @Override
    void appendDataJoins(StringBuilder sqlBuffer, DataProjection[] projections) {
        Set<String> appendedDataTypes = new HashSet<String>();

        for (Order order : _description._orders) {
            if (order.getQuery() == null) {
                appendJoin(sqlBuffer, order.getType(), appendedDataTypes);
            }
        }

        if (_description._groupType != null) {
            for (String groupType : _description._groupType) {
                appendJoin(sqlBuffer, groupType, appendedDataTypes);
            }
        }

        if (projections != null) {
            for (DataProjection projection : projections) {
                appendJoin(sqlBuffer, projection.getType(), appendedDataTypes);
            }
        }
    }

    // Appends a join if it has not already been appended using
    // the supplied data appended type set
    private void appendJoin(StringBuilder sqlBuffer, String dataType,
            Set<String> appendedDataTypes) {
        if (appendedDataTypes.contains(dataType)) {
            return;
        }
        appendedDataTypes.add(dataType);
        appendJoin(sqlBuffer, dataType);
    }

    @Override
    void appendDataTypeTests(StringBuilder sqlBuffer, DataProjection[] projections) {
        Set<String> appendedDataTypes = new HashSet<String>();

        for (Order order : _description._orders) {
            if (order.getQuery() == null) {
                appendType(sqlBuffer, order.getType(), appendedDataTypes);
            }
        }

        if (projections != null) {
            for (DataProjection projection : projections) {
                appendType(sqlBuffer, projection.getType(), appendedDataTypes);
            }
        }

        if (_description._groupType != null) {
            for (String groupType : _description._groupType) {
                appendType(sqlBuffer, groupType, appendedDataTypes);
            }
        }
    }

    // Appends a type test if it has not already been appended using
    // the supplied data appended type set
    private void appendType(StringBuilder sqlBuffer, String dataType,
            Set<String> appendedDataTypes) {
        if (appendedDataTypes.contains(dataType)) {
            return;
        }
        appendedDataTypes.add(dataType);
        appendType(sqlBuffer, dataType);
    }

    @Override
    void appendConditionTests(StringBuilder sqlBuffer) {
        _description.applyConditions(sqlBuffer, CONJUNCTION_BUILDER, _handler);
    }

    @Override
    void appendDisjunctionTests(StringBuilder sqlBuffer,
                                DisjunctionSeries disjunctions,
                                String prefix) {
        disjunctions.preprocessDisjunctions();
        for (int index = 0; index < disjunctions.size(); ++index) {
            List<Conjunction> disjunction = disjunctions
                    .get(index);
            if (disjunction.isEmpty()) {
                continue;
            }
            sqlBuffer.append(" AND ((");
            for (int jndex = 0; jndex < disjunction.size(); ++jndex) {
                QuerySyntax.append(jndex > 0, sqlBuffer, ") OR (");

                Conjunction conjunction = disjunction.get(jndex);

                String stub = (prefix == null ? "" : prefix + "_") + index + "_" + jndex + "_";

                _description.applyConjunction(conjunction, sqlBuffer, stub,
                        CONJUNCTION_BUILDER, _handler);
            }
            sqlBuffer.append("))");
        }
    }
}
