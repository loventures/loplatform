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
 * This strategy builds the actual QL for a single subselect per all of the
 * provided conditions. None of the conditions in the {@link Conjunction} should
 * be finderized.
 */
class SubselectConjunctionBuilder extends AbstractConjunctionBuilder {
    public String build(String stub, int offset, Conjunction conjunction,
            QueryParameterHandler handler) {
        if (conjunction.isEmpty()) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        int typeIndex = offset, subselectCount = 0;

        for (String dataType : conjunction.getDataTypes()) {
            Collection<Condition> conditions = conjunction.getConditions(dataType);

            if (conditions.isEmpty()) {
                continue;
            }

            // OR even though this is a conjunction, see the GROUP BY...HAVING
            // below
            QuerySyntax.or(buffer);

            buffer.append(QuerySyntax.L_PAREN);

            buffer.append(applyType(conjunction, handler, stub, typeIndex,
                    dataType, conditions));

            buffer.append(QuerySyntax.R_PAREN);

            typeIndex += conditions.size();

            ++ subselectCount;
        }

        if (subselectCount > 0) {
            // this is what exerts the conjunction, ensuring that the number of
            // hits, or true conditions, in the subselect matches what is
            // expected
            String entityId = (handler._description._entityDescription == null)
                ? handler._description._itemLabel.concat(".id ")
                : handler._description.itemFieldIdentity("owner");
            if (handler._description.nativeQuery()) {
                buffer.insert(0, " " + entityId + (handler._description._inclusive ? " IN" : " NOT IN") + " (SELECT d.owner_id FROM Data d WHERE ");
            } else {
                buffer.insert(0, " " + entityId + (handler._description._inclusive ? " IN" : " NOT IN") + " (SELECT d.owner.id FROM Data d WHERE ");
            }
            // the aggregation is only required for more than one condition
            if (subselectCount > 1) {
                if (handler._description.nativeQuery()) {
                    buffer.append(" GROUP BY d.owner_id");
                    buffer.append(" HAVING count(d.owner_id) = ").append(subselectCount);
                } else {
                    buffer.append(" GROUP BY d.owner.id");
                    buffer.append(" HAVING count(d.owner.id) = ").append(subselectCount);
                }
            }
            buffer.append(")");
        }

        appendEntityClause(buffer, stub, typeIndex, conjunction, handler);

        return buffer.toString();
    }

    private String applyType(Conjunction conjunction,
            QueryParameterHandler handler, String stub, int typeIndex,
            String dataType, Collection<Condition> conditions) {
        StringBuilder buffer = new StringBuilder();

        String alias = conjunction.getAlias(dataType);
        String prefix = "d";

        assert conjunction.inDataCollection(dataType) : "This implementation only handles non-finderized conditions.";

        // constrain to type
        if (handler._description.nativeQuery()) {
            buffer.append("(d.type_name = :").append(alias).append("Type").append(
                ")");
        } else {
            buffer.append("(d.type = :").append(alias).append("Type").append(
            ")");
        }

        int conditionIndex = 0;
        for (Condition condition : conditions) {
            String suffix = String.valueOf(typeIndex + conditionIndex);
            if (stub != null) {
                suffix = stub.concat(suffix);
            }

            QuerySyntax.and(buffer);

            Object value = handler.getValue(condition);

            // check value
            String lhExpression = buildLeftHand(conjunction, condition, prefix,
                    value);
            String operator = buildOperator(condition, null == value);

            buffer.append(QuerySyntax.L_PAREN).append(lhExpression);
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

            if (condition.getComparison() == Comparison.like) {
                buffer.append(" ESCAPE '\\'");
            }

            buffer.append(QuerySyntax.R_PAREN);

            // NB: This index handling must match the behaviour of
            // QueryParameterHandler.
            conditionIndex++;
        }

        return buffer.toString();
    }
}
