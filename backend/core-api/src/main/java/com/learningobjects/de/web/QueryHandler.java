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

package com.learningobjects.de.web;

import com.learningobjects.cpxp.component.query.ApiFilter;
import com.learningobjects.cpxp.component.query.ApiOrder;
import com.learningobjects.cpxp.service.query.Condition;
import com.learningobjects.cpxp.service.query.QueryBuilder;

public interface QueryHandler {
    default void applyFilter(QueryBuilder qb, ApiFilter filter) { filter.unsupported(); };

    default Condition asCondition(ApiFilter filter) { filter.unsupported(); return null; };

    default void applyOrder(QueryBuilder qb, ApiOrder order) {
        order.unsupported();
    }
}
