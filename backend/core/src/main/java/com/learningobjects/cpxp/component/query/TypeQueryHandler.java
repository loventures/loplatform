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

import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.component.ComponentDescriptor;
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
import org.apache.commons.collections4.IterableUtils;

import javax.validation.groups.Default;
import java.util.List;

/**
 * A query handler for interpreting API query filters against the "_type" property
 * of a non-schema-mapped entity. Translates a query for _type:eq(foo) into a query
 * against componentidentifier="loi.cp.foo.FooImpl". Translates a query for _type:le(foo)
 * into a query against componentidentifier and the identifiers of all subclasses of the
 * requested type.
 */
public class TypeQueryHandler implements QueryHandler {
    @Override
    public void applyFilter(QueryBuilder qb, ApiFilter filter) {
        SchemaRegistry.Registration registration = ComponentSupport.lookupResource(Schema.class, SchemaRegistry.Registration.class, filter.getValue(), Default.class);
        if (registration == null) {
            throw new ValidationException("filter#value", filter.getValue(), "Unsupported filter value");
        }

        Iterable<ComponentDescriptor> descriptors = ComponentSupport.getComponentDescriptors((Class<? extends ComponentInterface>) registration.getSchemaClass());
        List<ComponentDescriptor> descList = IterableUtils.toList(descriptors);
        if (descList.isEmpty()) {
            throw new ValidationException("filter#value", filter.getValue(), "Unsupported filter value");
        }

        switch (filter.getOperator()) {
            case EQUALS:
                if (descList.size() > 1) {
                    throw new ValidationException("filter#value", filter.getValue(), "Cannot be used for supertypes");
                }
                qb.addCondition(BaseCondition.getInstance(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, Comparison.eq, descList.get(0).getIdentifier()));
                break;
            case LESS_THAN_OR_EQUALS:
                qb.addCondition(BaseCondition.inIterable(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, Iterables.transform(descList, ComponentDescriptor::getIdentifier)));
                break;
            default:
                throw new ValidationException("filter#operator", String.valueOf(filter.getOperator()), "Unsupported filter operator");
        }
    }
}
