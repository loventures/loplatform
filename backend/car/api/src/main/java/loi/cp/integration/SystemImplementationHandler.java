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

package loi.cp.integration;

import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.query.ApiFilter;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.de.web.QueryHandler;

// TODO: make this an interface and lookup the sole implementation
public class SystemImplementationHandler implements QueryHandler {
    @Override
    public void applyFilter(QueryBuilder qb, ApiFilter filter) {
        // TODO: explicit null filter
        switch (filter.getOperator()) {
          case EQUALS:
              qb.addCondition(BaseCondition.getInstance(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, Comparison.eq, filter.getValue())); // TODO: should this consider aliases?
              break;
          case CONTAINS:
              qb.addCondition(BaseCondition.getInstance(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, Comparison.like, "%" + filter.getValue() + "%"));
              break;
          case LESS_THAN_OR_EQUALS: // this is a bit artificial, abusing le for instanceof
              final Class<? extends ComponentInterface> iface;
              try {
                  iface = (Class<? extends ComponentInterface>) ComponentSupport.loadClass(filter.getValue());
              } catch (Throwable th) {
                  throw new ValidationException("filter#value", filter.getValue(), "Unsupported filter operator");
              }
              Iterable<String> implementations = Iterables.transform(ComponentSupport.getComponentDescriptors(iface), ComponentDescriptor::getIdentifier);
              qb.addCondition(BaseCondition.inIterable(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER, implementations));
              break;

          default:
              throw new ValidationException("filter#operator", String.valueOf(filter.getOperator()), "Unsupported filter operator");
        }
    }
}
