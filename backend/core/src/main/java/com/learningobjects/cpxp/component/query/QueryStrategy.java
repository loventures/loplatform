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

import com.learningobjects.cpxp.service.query.*;


/**
 * All the different ways to build {@link Condition}s and {@link Order}s for purposes
 * of API querying.
 */
public enum QueryStrategy {

    SIMPLE {
        @Override
        protected Order getOrder(final String dataType, final Function function, final ApiOrder order) {
            return BaseOrder.byData(dataType, function, order.getQbDirection());
        }

        @Override
        protected Condition getCondition(final String dataType, final Comparison cmp, final String propertyName, final Object value) {
            return BaseCondition.getInstance(dataType, cmp, value);
        }
    },

    JSON {
        @Override
        protected Order getOrder(final String dataType, final Function function, final ApiOrder order) {
            return BaseOrder.byJsonField(dataType, order.getProperty(), function, order.getQbDirection());
        }

        @Override
        protected Condition getCondition(final String dataType, final Comparison cmp, final String propertyName, final Object value) {
            return BaseCondition.jsonInstance(dataType, propertyName, cmp, (String) value);
        }
    },

    TSVECTOR {
        @Override
        protected Order getOrder(final String dataType, final Function function, final ApiOrder order) {
            throw new IllegalStateException("Unorderable tsvector");
        }

        @Override
        protected Condition getCondition(final String dataType, final Comparison cmp, final String propertyName, final Object value) {
            if (cmp != null) {
                throw new IllegalStateException("Unsupported tsvector comparison: " + cmp);
            }
            return BaseCondition.getInstance(dataType, Comparison.search, value);
        }
    };


    protected abstract Condition getCondition(final String dataType, Comparison cmp, final String propertyName, Object value);

    protected abstract Order getOrder(final String dataType, final Function function, ApiOrder order);

}

