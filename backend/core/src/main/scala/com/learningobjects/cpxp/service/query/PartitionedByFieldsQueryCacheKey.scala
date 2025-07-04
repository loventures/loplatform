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

package com.learningobjects.cpxp.service.query

import com.google.common.annotations.VisibleForTesting

import java.util.Optional

/** A special cache key strategy used to partition multi-element id based queries for precaching the subsequent
  * individual detail queries, by calculating the equivalent single-element condition keys.
  */
class PartitionedByFieldsQueryCacheKey[K](description: QueryDescription, val dataType: String, val specificItem: K)
    extends QueryCacheKey(description):

  protected override def conditionTest(condition: Condition): Optional[String] =
    if isInConditionOn(condition, dataType) then
      super.conditionTest(makeCondition(dataType, Comparison.eq, specificItem))
    else super.conditionTest(condition)

  @VisibleForTesting
  def makeCondition(fieldName: String, comparison: Comparison, fieldValue: K) =
    BaseCondition.getInstance(fieldName, comparison, fieldValue)
end PartitionedByFieldsQueryCacheKey
