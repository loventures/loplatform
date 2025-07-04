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
import com.learningobjects.cpxp.service.data.DataFormat;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * This strategy builds the actual QL for evaluating against generated entities.
 */
class EntityConjunctionBuilder extends AbstractConjunctionBuilder {
    public String build(String stub, int offset, Conjunction conjunction,
            QueryParameterHandler handler) {
        if (conjunction.isEmpty()) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        int typeIndex = offset;

        for (String dataType : conjunction.getEntityTypes()) {
            Collection<Condition> conditions = conjunction.getConditions(dataType);

            if (conditions.isEmpty()) {
                continue;
            }

            // exerts the conjunction as an AND, similar to
            // JoinConjunctionBuilder except doesn't have to noodle around with
            // type conditions
            QuerySyntax.and(buffer);

            buffer.append(QuerySyntax.L_PAREN);

            buffer.append(applyType(conjunction, handler, stub, typeIndex,
                    dataType, conditions));

            buffer.append(QuerySyntax.R_PAREN);

            typeIndex += conditions.size();
        }

        for (Condition condition: conjunction.getNonDataConditions()) {
            assert (condition.getComparison() == Comparison.in) ||
                    (condition.getComparison() == Comparison.notIn)
                : "Non-data conditions must use in operator";

            QuerySyntax.and(buffer);

            buffer.append(QuerySyntax.L_PAREN);

            buffer.append(conjunction.getEntityAlias());
            if (handler._description.nativeQuery()) {
                buffer.append("_id");
            }

            buffer.append(" ").append(condition.getComparison().getQueryOperator()).append(" ");

            buffer.append(QuerySyntax.L_PAREN);

            QueryBuilder subquery = condition.getQuery();
            buffer.append(subquery.buildSubquery());

            buffer.append(QuerySyntax.R_PAREN);

            buffer.append(QuerySyntax.R_PAREN);
        }

        return buffer.toString();
    }

    private String applyType(Conjunction conjunction,
            QueryParameterHandler handler, String stub, int typeIndex,
            String dataType, Collection<Condition> conditions) {
        StringBuilder buffer = new StringBuilder();

        String alias = conjunction.getAlias(dataType);

        assert !conjunction.inDataCollection(dataType) : String
                .format(
                        "Data type, %1$s, on item type, %2$s, should be in the data collection, not handled by entity code.",
                        dataType, handler._description._itemType);

        int conditionIndex = 0;
        for (Condition condition : conditions) {
            String suffix = String.valueOf(typeIndex + conditionIndex);
            if (stub != null) {
                suffix = stub.concat(suffix);
            }

            QuerySyntax.and(buffer);

            Object value = handler.getValue(condition);

            if ((value instanceof List) && ((List) value).isEmpty()) {
                buffer.append("FALSE");
                continue;
            }

            boolean isSearch = condition.getComparison() == Comparison.search;

            // check value
            String lhExpression = buildLeftHand(conjunction, condition, alias, value);
            String operator = buildOperator(condition, null == value);

            buffer.append(QuerySyntax.L_PAREN);

            final String language = StringUtils.defaultString(condition.getLanguage(), "english");

            if (isSearch && (BaseOntology.getOntology().getDataFormat(dataType) != DataFormat.tsvector)) {
                buffer.append("to_tsvector('").append(language).append("',LOWER(").append(lhExpression).append("))");
            } else {
                buffer.append(lhExpression);
            }
            buffer.append(' ').append(operator);

            // buildOperator ensures value is only null if the
            // comparison is eq or ne which get converted to the unary operators
            // is null, is not null respectively

            // the rh expression is just the placeholder with the correctly
            // synthesized name
            if (value instanceof List) {
                buildListRightHand(handler, buffer, suffix, (List) value);
            } else if (value != null) {
                String rhParameter = ":" + handler.getValueParameterName(suffix);
                if (isSearch) {
                    buffer.append(" plainto_tsquery('").append(language).append("', LOWER(").append(rhParameter).append("))");
                } else if (condition.getComparison().expectsArray()) {
                    // this should just be " (:p1)" but JPA cannot accept an actual array parameter
                    buffer.append(" (cast(").append(rhParameter).append(" as bigint[]))");
                } else {
                    buffer.append(' ').append(rhParameter);
                }
            }

            if (condition.getComparison() == Comparison.like) {
                buffer.append(" ESCAPE '\\'");
            }

            buffer.append(QuerySyntax.R_PAREN);

            conditionIndex++;
        }

        return buffer.toString();
    }

}
