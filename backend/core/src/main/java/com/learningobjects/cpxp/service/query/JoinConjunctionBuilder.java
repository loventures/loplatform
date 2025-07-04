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

import java.util.Collection;
import java.util.List;

/**
 * This strategy builds the actual QL for a the conditions against a series of
 * joins, one per data type. None of the conditions in the {@link Conjunction}
 * should be finderized.
 */
class JoinConjunctionBuilder extends AbstractConjunctionBuilder {
    public String build(String stub, int offset, Conjunction conjunction, QueryParameterHandler handler) {
        if (conjunction.isEmpty()) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        int typeIndex = offset;

        for (String dataType : conjunction.getDataTypes()) {
            Collection<Condition> conditions = conjunction.getConditions(dataType);

            if (conditions.isEmpty()) {
                continue;
            }

            // this is the AND that exerts the conjunction
            QuerySyntax.and(buffer);

            buffer.append(QuerySyntax.L_PAREN);

            buffer.append(applyType(conjunction, handler, stub, typeIndex, dataType,
                    conditions));

            buffer.append(QuerySyntax.R_PAREN);

            typeIndex += conditions.size();
        }

        appendEntityClause(buffer, stub, typeIndex, conjunction, handler);

        return buffer.toString();
    }

    private String applyType(Conjunction conjunction, QueryParameterHandler handler, String stub, int typeIndex,
            String dataType, Collection<Condition> conditions) {
        StringBuilder buffer = new StringBuilder();

        String alias = conjunction.getAlias(dataType);

        assert conjunction.inDataCollection(dataType) : "This implementation only handles conditions in the data collection.";

        int conditionIndex = 0;
        for (Condition condition : conditions) {
            String suffix = String.valueOf(typeIndex + conditionIndex);
            if (stub != null) {
                suffix = stub.concat(suffix);
            }

            QuerySyntax.and(buffer);

            Object value = handler.getValue(condition);

            // check value
            String lhExpression = buildLeftHand(conjunction, condition, alias,
                    value);
            String operator = buildOperator(condition, null == value);

            buffer.append(lhExpression);
            buffer.append(' ').append(operator);

            // buildOperator ensures this statement is only reached if the
            // comparison is eq or ne which get converted to the unary operators
            // is null, is not null respectively
            if (value instanceof List) {
                buildListRightHand(handler, buffer, suffix, (List) value);
            } else if (null != value) {
                // the rh expression is just the placeholder with the correctly
                // synthesized name
                buffer.append(" :").append(handler.getValueParameterName(suffix));
            }

            conditionIndex++;
        }

        return buffer.toString();
    }
}
