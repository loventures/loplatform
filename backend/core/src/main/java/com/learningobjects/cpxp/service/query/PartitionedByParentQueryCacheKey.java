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

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.service.data.DataTypes;

import java.util.Optional;

/**
 * A special cache key strategy used to partition mutliple-parent queries for precaching
 * the subsequent individual parent detail queries, by calculating the equivalent single parent keys.
 * We do so by asserting a specific parent, and ignoring multiparent conditions.
 */
class PartitionedByParentQueryCacheKey extends QueryCacheKey {
    Id _parentId;

    public PartitionedByParentQueryCacheKey(QueryDescription description, Id parentId) {
        super(description);
        this._parentId = parentId;
    }

    @Override
    protected Id getParent() {
        return _parentId;
    }

    @Override
    protected Optional<String> conditionTest(Condition condition) {
        final boolean isMultiParentCondition = isInConditionOn(condition, DataTypes.META_DATA_TYPE_PARENT_ID);
        return isMultiParentCondition ? Optional.empty() : super.conditionTest(condition);
    }
}
