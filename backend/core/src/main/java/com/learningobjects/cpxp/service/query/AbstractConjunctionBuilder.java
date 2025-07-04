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

import java.util.List;

/**
 * Common base class for building conjunction clauses, provides some conveninece
 * methods in common for handling specific conditions.
 */
abstract class AbstractConjunctionBuilder implements ConjunctionBuilder {
    private static final EntityConjunctionBuilder ENTITY_BUILDER = new EntityConjunctionBuilder();

    // this is what the finder support boils down to, see how
    // FinderConjuctionBuilder uses itself to get finder only conditions from
    // the conjunction argument
    void appendEntityClause(StringBuilder buffer, String stub, int typeIndex,
            Conjunction conjunction, QueryParameterHandler handler) {
        String entityClause = ENTITY_BUILDER.build(stub, typeIndex,
                conjunction, handler);

        if (entityClause.length() > 0) {
            QuerySyntax.and(buffer);

            buffer.append(entityClause);
        }
    }

    // specifically re-writes field names on entities for a handful of very
    // specific special cases
    String buildLeftHand(Conjunction conjunction, Condition condition,
            String alias, Object value) {
        StringBuilder lhBuilder = new StringBuilder();

        // build the field de-reference
        String fieldType = condition.getDataType();
        String field = conjunction.getField(fieldType, value == null);

        boolean isJson = (condition.getField() != null && !condition.getField().isEmpty());

        if (!isJson) {
            if (!fieldType.startsWith("#")) {
                lhBuilder.append(alias).append('.');
            }
            lhBuilder.append(field);
        } else {
            // using the ->>'xxx' operator breaks for null attributes
            lhBuilder.append("jsonb_extract_path_text(")
                    .append(alias)
                    .append('.')
                    .append(field);
            for (PathElem elt : condition.getField()) {
                lhBuilder
                  .append(',')
                  .append(elt.toString());
            }
            lhBuilder.append(')');
        }

        String function = condition.getFunction();

        if (null == function) {
            return lhBuilder.toString();
        }

        String argument = lhBuilder.toString();

        lhBuilder = new StringBuilder(function);
        lhBuilder.append('(');
        lhBuilder.append(argument);
        lhBuilder.append(')');

        return lhBuilder.toString();
    }

    // specifically to detect null for equivalence based comparisons and
    // re-write appropriately
    String buildOperator(Condition condition, boolean isNull) {
        if (isNull) {
            String test = null;
            switch (condition.getComparison()) {
            case eq:
                test = "IS NULL";
                break;
            case ne:
                test = "IS NOT NULL";
                break;
            case in: {
                QueryBuilder query = condition.getQuery();
                test = "IN (" + query.buildSubquery() + ")";
                break;
            }
            case notIn: {
                QueryBuilder query = condition.getQuery();
                test = "NOT IN (" + query.buildSubquery() + ")";
                break;
            }
            default:
                throw new IllegalStateException(
                        "Cannot use a comparison other than eq or ne with a null value.");
            }

            return test;
        }

        return condition.getComparison().getQueryOperator();
    }

    protected void buildListRightHand(QueryParameterHandler handler, StringBuilder buffer, String suffix, List list) {
        /* This is to work around a bug in Hibernate where X IN :list breaks with..
         * org.hibernate.hql.ast.QuerySyntaxException: unexpected AST node: {vector}
         * See QueryParameterHandler#setValueParameter */
        String prefix = handler.getValueParameterName(suffix);
        buffer.append(" (");
        for (int index = 0; index < list.size(); ++ index) {
            if (index > 0) buffer.append(", ");
            buffer.append(':').append(prefix).append('_').append(index);
        }
        buffer.append(')');
    }
}
