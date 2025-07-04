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

import { selectOverallGradeByUser } from '../../../selectors/gradeSelectors';
import { createPropertySelector } from '../../../utilities/reduxify';
import { selectCourse, selectCurrentUser } from '../../../utilities/rootSelectors';
import { createSelector, createStructuredSelector } from 'reselect';

import { selectAssignmentsListUser } from './baseSelectors';
import { selectLearnerAssignmentsList } from './listSelectors';

const selectIsGradebookView = createSelector(
  selectAssignmentsListUser,
  selectCurrentUser,
  (viewingAs, currentUser) => {
    return viewingAs.id !== currentUser.id;
  }
);

const selectAssignmentsListUserOverallGrade = createSelector(
  selectAssignmentsListUser,
  selectOverallGradeByUser,
  (viewingAs, overallGradeByUser) => overallGradeByUser[viewingAs.id]
);

export const selectLearnerAssignmentsPageHeaderComponent = createStructuredSelector({
  course: selectCourse,
  currentUser: selectCurrentUser,
  viewingAs: selectAssignmentsListUser,
  overallGrade: selectAssignmentsListUserOverallGrade,
  numberOfAssignments: createPropertySelector(
    selectLearnerAssignmentsList,
    'listState.data.filterCount',
    undefined
  ),
});

export const selectLearnerAssignmentsPageComponent = createStructuredSelector({
  showGradebookLink: selectIsGradebookView,
  viewingAs: selectAssignmentsListUser,
});
