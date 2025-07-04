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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ProjectionResults extends ArrayList<ProjectionResult> {
    private final Map<String, Integer> columnIndexes = new HashMap<>();

    ProjectionResults(AbstractQueryBuilder aqb) {
        accumulateQueryBuilders(aqb, 0);
        for (Object[] o : aqb.<Object[]>getResultList()) {
            add(new ProjectionResultImpl(o));
        }
    }

    private int accumulateQueryBuilders(AbstractQueryBuilder aqb, int index) {
        DataProjection[] dps = aqb._description._dataProjection;
        if (dps != null) {
            for (DataProjection dp : dps) {
                int dpi = index ++;
                columnIndexes.put(aqb._description._itemType + ":" + dp, dpi);
                columnIndexes.put(System.identityHashCode(aqb) + ":" + dp, dpi);
            }
        }
        for (Join join : aqb._description._joinQueries) {
            index = accumulateQueryBuilders(((AbstractQueryBuilder) join.query()), index);
        }
        return index;
    }

    class ProjectionResultImpl implements ProjectionResult {
        private final Object[] row;

        ProjectionResultImpl(final Object[] row) {
            this.row = row;
        }

        // TODO: Support aliases on projections and then get(alias)

        @Override
        public <T> T get(String itemType, String dataType, Class<T> t) {
            return get0(itemType + ":" + BaseDataProjection.ofDatum(dataType), t);
        }

        @Override
        public <T> T get(String itemType, String dataType, String jsonField, Class<T> t) {
            return get0(itemType + ":" + BaseDataProjection.ofJsonAttribute(dataType, jsonField), t);
        }

        @Override
        public <T> T get(String itemType, String dataType, Function function, Class<T> t) {
            return get0(itemType + ":" +  BaseDataProjection.ofAggregateData(dataType, function), t);
        }

        @Override
        public <T> T get(QueryBuilder qb, DataProjection projection, Class<T> t) {
            return get0(System.identityHashCode(qb) + ":" + projection, t);
        }

        private <T> T get0(String projection, Class<T> t) {
            Integer index = columnIndexes.get(projection);
            if (index == null) {
                throw new IllegalArgumentException("Could not find the data projection: " + projection);
            }
            Object o = row[index];
            if ((o == null) || t.isInstance(o)) {
                return t.cast(o);
            } else if (Long.class.equals(t) && (o instanceof Number)) { // handle BigInteger -> Long
                return t.cast(((Number) o).longValue());
            } else {
                throw new IllegalArgumentException("Data projection: " + projection + " is not a " + t.getSimpleName() + ": " + o);
            }
        }
    }
}

