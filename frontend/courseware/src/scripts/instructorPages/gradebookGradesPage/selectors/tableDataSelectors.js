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

import { filter, groupBy, map, orderBy, sumBy } from 'lodash';
import { createListDataSelector, createListStateSelector } from '../../../list/createListSelectors';
import { Credit } from '../../../utilities/creditTypes';
import { createPropertySelector } from '../../../utilities/reduxify';
import { createSelector, createStructuredSelector } from 'reselect';

import { moduleConfig } from '../config';

export const selectGradeByContentByUser = state => state.api.gradeByContentByUser;

export const selectGradebookTableOptions = state => state.ui.gradebookTableOptions;

export const selectGradebookTableStructure = createSelector(
  state => state.api.contentItems,
  state => state.api.gradebookColumns,
  state => state.ui.gradebookTableOptions.showForCreditOnly,
  state => state.ui.gradebookTableOptions.collapsedTables,
  (contents, columns, forCreditOnly, collapsedTables) => {
    // I do believe these are already ordered.
    const orderedCategories = filter(columns, column => column.type === 'Category');
    const totalWeight = sumBy(orderedCategories, 'weight');
    const columnsByCategory = groupBy(
      filter(columns, c => c.id !== c.category_id),
      'category_id'
    );
    const gradebookStructure = map(orderedCategories, (cat, index) => {
      const columnIds = map(
        orderBy(
          filter(columnsByCategory[cat.id], column => {
            return !forCreditOnly || column.credit === Credit;
          }),
          'index'
        ),
        'id'
      );
      const collapsed = collapsedTables[index];
      return {
        categoryId: cat.id,
        categoryTitle: cat.name,
        columnIds,
        collapsed,
        visible: !collapsed && columnIds.length > 0,
        weight: cat.weight,
      };
    });
    return {
      gradebookStructure,
      totalWeight,
    };
  }
);

export const selectGradebookLearners = createStructuredSelector({
  learners: createPropertySelector(
    createListDataSelector(
      state => state.ui[moduleConfig.sliceName],
      state => state.api.users
    ),
    'list'
  ),
  showExternalIds: state => state.ui.gradebookTableOptions.showExternalIds,
  courseId: state => state.course.id,
});

export const selectGradebookGradeBody = createStructuredSelector({
  learnerIds: state => {
    if (state.ui[moduleConfig.sliceName].status.loading) {
      return [];
    } else {
      return state.ui[moduleConfig.sliceName].data.list;
    }
  },
});

export const selectTableDataLoaderComponent = createStructuredSelector({
  loadingState: state => state.ui[moduleConfig.sliceName].status,
});

export const selectTableListControlComponent = createStructuredSelector({
  listState: createListStateSelector(state => state.ui[moduleConfig.sliceName]),
  showExternalIds: state => state.ui.gradebookTableOptions.showExternalIds,
  showForCreditOnly: state => state.ui.gradebookTableOptions.showForCreditOnly,
  gradeDisplayMethod: state => state.ui.gradebookTableOptions.gradeDisplayMethod,
});
