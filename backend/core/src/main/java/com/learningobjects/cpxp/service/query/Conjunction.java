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

import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Sets;

import java.util.*;


/**
 * Data structure to accumulate conditions that should be evaluated with ANDs
 * between them, coalesces multiple conditions against the same data type.
 * Provides a facade back through to the {@link AbstractQueryBuilder},
 * {@link QueryDescription} and {@link QueryParameterHandler} instances for the
 * implementations of {@link ConjunctionBuilder} to access for the handful of
 * methods they need.
 *
 * This groups primarily by whether data types are non-normalized
 * or normalized, and secondarily by data type, but otherwise
 * preserves order.
 */
class Conjunction implements Iterable<Condition> {
    private final QueryDescription _description;

    // group conditions that are applied to the same datatype
    private final LinkedHashMultimap<String, Condition> _dataConditions = LinkedHashMultimap.create();

    // group conditions that are applied to the same datatype
    private final LinkedHashMultimap<String, Condition> _entityConditions = LinkedHashMultimap.create();

    // conditions that are not applied to a datatype
    private final List<Condition> _nonDataConditions = new ArrayList<>();

    Conjunction(QueryDescription description) {
        _description = description;
    }

    void addCondition(String dataType, String comparison, Object value) {
        addCondition(BaseCondition.getInstance(dataType, comparison, value));
    }

    void addCondition(Condition condition) {
        String dataType = condition.getDataType();
        if (dataType == null) { // Condition.inQuery can have null
            _nonDataConditions.add(condition);
        } else if (inDataCollection(dataType)) {
            _dataConditions.put(dataType, condition);
        } else {
            _entityConditions.put(dataType, condition);
        }
    }

    boolean isEmpty() {
        return _dataConditions.isEmpty() && _entityConditions.isEmpty() && _nonDataConditions.isEmpty();
    }

    Set<String> getTypes() {
        return Sets.union(_dataConditions.keySet(), _entityConditions.keySet());
    }

    Set<String> getDataTypes() {
        return _dataConditions.keySet();
    }

    Set<String> getEntityTypes() {
        return _entityConditions.keySet();
    }

    public Iterator<Condition> iterator() {
        return Iterators.concat(_dataConditions.values().iterator(),
                _entityConditions.values().iterator(),
                _nonDataConditions.iterator());
    }

    public List<Condition> conjoinedConditions() {
        List<Condition> result = new LinkedList<>();
        for (Iterator<Condition> i = iterator(); i.hasNext();) {
            result.add(i.next());
        }
        return result;
    }

    Collection<Condition> getNonDataConditions() {
        return _nonDataConditions;
    }

    Collection<Condition> getConditions(String dataType) {
        return _dataConditions.containsKey(dataType)
                ? _dataConditions.get(dataType)
                : _entityConditions.get(dataType);
    }

    String getEntityAlias() {
        return _description._itemLabel;
    }

    String getAlias(String dataType) {
        return _description.getAlias(dataType);
    }

    boolean inDataCollection(String dataType) {
        return !dataType.startsWith("#") && _description.inDataCollection(dataType);
    }

    String getField(String dataType, boolean valueIsNull) {
        return _description.getField(dataType, valueIsNull);
    }
}
