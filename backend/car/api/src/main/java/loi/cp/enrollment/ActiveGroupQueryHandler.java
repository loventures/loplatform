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

package loi.cp.enrollment;

import com.learningobjects.cpxp.component.query.ApiFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.service.query.BaseCondition;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.de.web.QueryHandler;

import java.util.Arrays;

/**
 * Filter enrollments to eliminate enrollments that are inactive and courses that are inactive.
 */
public class ActiveGroupQueryHandler implements QueryHandler {
    @Override
    public void applyFilter(QueryBuilder qb, ApiFilter filter) {
        QueryBuilder groupQb =
          qb.getOrCreateJoinQuery(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, GroupConstants.ITEM_TYPE_GROUP)
            .addCondition(
              BaseCondition.getSimpleInstance(DataTypes.DATA_TYPE_DISABLED, Comparison.eq, false))
            .addDisjunction0(Arrays.asList(
              BaseCondition.getSimpleInstance(GroupConstants.DATA_TYPE_START_DATE, Comparison.lt, Current.getTime()),
              BaseCondition.getSimpleInstance(GroupConstants.DATA_TYPE_START_DATE, Comparison.eq, null)
            ))
            .addDisjunction0(Arrays.asList(
              BaseCondition.getSimpleInstance(GroupConstants.DATA_TYPE_END_DATE, Comparison.gt, Current.getTime()),
              BaseCondition.getSimpleInstance(GroupConstants.DATA_TYPE_END_DATE, Comparison.eq, null)
            ));

        qb.addCondition(BaseCondition.getSimpleInstance(DataTypes.DATA_TYPE_DISABLED, Comparison.eq, false))
          .addCondition(BaseCondition.getSimpleInstance(DataTypes.DATA_TYPE_START_TIME, Comparison.lt, Current.getTime()))
          .addCondition(BaseCondition.getSimpleInstance(DataTypes.DATA_TYPE_STOP_TIME, Comparison.gt, Current.getTime()));
    }
}
