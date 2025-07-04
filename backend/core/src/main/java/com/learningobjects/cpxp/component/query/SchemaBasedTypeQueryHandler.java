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

import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.registry.SchemaRegistry;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.de.web.QueryHandler;

import javax.validation.groups.Default;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A query handler for interpreting API query filters against the "_type" property
 * of a schema-mapped entity. Translates a query for _type:eq(foo) into a query
 * against componentidentifier="foo". Translates a query for _type:le(foo)
 * into a query against componentidentifier and the schemata of all subclasses of the
 * requested type.
 */
public class SchemaBasedTypeQueryHandler implements QueryHandler {
    @Override
    public void applyFilter(QueryBuilder qb, ApiFilter filter) {
        SchemaRegistry.Registration registration = ComponentSupport.lookupResource(Schema.class, SchemaRegistry.Registration.class, filter.getValue(), Default.class);
        if (registration == null) {
            throw new ValidationException("filter#value", filter.getValue(), "Unsupported filter value");
        }

        List<String> schemata =
          StreamSupport.stream(ComponentSupport.getComponentDescriptors((Class<? extends ComponentInterface>) registration.getSchemaClass())
          .spliterator(), false)
          .map(ComponentSupport::getSchemaName)
          .collect(Collectors.toList());
        if (schemata.isEmpty()) {
            throw new ValidationException("filter#value", filter.getValue(), "Unsupported filter value");
        }

        switch (filter.getOperator()) {
            case EQUALS:
                if (schemata.size() > 1) {
                    throw new ValidationException("filter#value", filter.getValue(), "Cannot be used for supertypes");
                }
                qb.addCondition(BaseCondition.getInstance(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, Comparison.eq, schemata.get(0)));
                break;
            case LESS_THAN_OR_EQUALS:
                qb.addCondition(BaseCondition.inIterable(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, schemata));
                break;
            default:
                throw new ValidationException("filter#operator", String.valueOf(filter.getOperator()), "Unsupported filter operator");
        }
    }
}
