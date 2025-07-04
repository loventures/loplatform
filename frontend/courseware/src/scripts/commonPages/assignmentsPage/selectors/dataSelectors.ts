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

import { CONTENT_TYPE_CHECKPOINT } from '../../../utilities/contentTypes';
import { get, mapValues, pickBy } from 'lodash';
import { selectActivityByContentByUser } from '../../../selectors/activitySelectors';
import {
  selectGradebookColumns,
  selectGradesByContentByUser,
} from '../../../selectors/gradeSelectors';
import { selectContentItemsWithNebulousDetails } from '../../../selectors/selectContentItemsWithNebulousDetails';
import { Credit } from '../../../utilities/creditTypes';
import { createSelector } from 'reselect';

import {
  selectAssignmentsListUser,
  selectLearnerAssignmentsForCreditOnlyStatus,
} from './baseSelectors';

const selectAllAssignmentListRows = createSelector(
  selectContentItemsWithNebulousDetails,
  selectGradebookColumns,
  (contents, gradebookColumnById) => {
    return mapValues(contents, content => {
      return {
        content,
        gradebookColumn: gradebookColumnById[content.id],
      };
    });
  }
);

/*
  Because we have this special view for assignments
  that is viewing for a student but not entering preview mode
  so we need to bring in student specific data
  without setting viewingAs globally
*/
const selectAllAssignmentListRowsModifiedForSpecialView = createSelector(
  selectAllAssignmentListRows,
  selectAssignmentsListUser,
  selectActivityByContentByUser,
  selectGradesByContentByUser,
  (rows, activeStudent, actvityByStudent, gradeByStudent) => {
    return mapValues(rows, row => {
      return {
        ...row,
        content: {
          ...row.content,
          activity: {
            ...row.content.activity,
            ...get(actvityByStudent, [activeStudent.id, row.content.id]),
          },
          grade: get(gradeByStudent, [activeStudent.id, row.content.id]),
        },
      };
    });
  }
);

export const selectAssignmentListRows = createSelector(
  selectAllAssignmentListRowsModifiedForSpecialView,
  selectLearnerAssignmentsForCreditOnlyStatus,
  (rows, forCreditOnly) =>
    pickBy(rows, row => {
      if (!row.gradebookColumn || row.gradebookColumn.id === row.gradebookColumn.Category.id) {
        return false;
      } else if (row.content.typeId === CONTENT_TYPE_CHECKPOINT) {
        return false;
      } else if (forCreditOnly) {
        return row.gradebookColumn.credit === Credit;
      } else {
        return true;
      }
    })
);
