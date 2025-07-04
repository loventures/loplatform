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

import { createSelector } from 'reselect';
import { selectLocalListDataAndState } from '../../../list/createLocalListSelectors';

import { selectAssignmentsListUser, selectPageState } from './baseSelectors';

import { selectAssignmentListRows } from './dataSelectors';

export const selectLearnerAssignmentsList = createSelector(
  selectPageState,
  selectAssignmentListRows,
  (pageState, data) => {
    const { list, listState } = selectLocalListDataAndState(pageState.listState, data);
    return {
      list,
      listState: {
        ...listState,
        //using the separate loadingState slice which is for attemptOverview
        //since the final loading state depends on those
        loadingState: pageState.loadingState,
      },
    };
  }
);

export const selectLearnerAssignmentsRowsComponent = createSelector(
  selectLearnerAssignmentsList,
  selectAssignmentsListUser,
  ({ list }, viewingAs) => {
    return {
      rows: list,
      viewingAs,
    };
  }
);
