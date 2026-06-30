/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import type { CourseState } from '../../../loRedux';
import { filter, groupBy, map, orderBy, sumBy } from 'lodash';
import { createListDataSelector, createListStateSelector } from '../../../list/createListSelectors';
import { Credit } from '../../../utilities/creditTypes';
import { createPropertySelector } from '../../../utilities/reduxify';
import { createSelector, createStructuredSelector } from 'reselect';

import { moduleConfig } from '../config';

export const selectGradeByContentByUser = (state: CourseState) => state.api.gradeByContentByUser;

export const selectGradebookTableOptions = (state: CourseState) => state.ui.gradebookTableOptions;

export const selectGradebookTableStructure = createSelector(
  (state: CourseState) => state.api.contentItems,
  (state: CourseState) => state.api.gradebookColumns,
  (state: CourseState) => state.ui.gradebookTableOptions.showForCreditOnly,
  (state: CourseState) => state.ui.gradebookTableOptions.collapsedTables,
  (contents: any, columns: any, forCreditOnly: boolean, collapsedTables: boolean[]) => {
    // I do believe these are already ordered.
    const orderedCategories = filter(columns, (column: any) => column.type === 'Category');
    const totalWeight = sumBy(orderedCategories, 'weight');
    const columnsByCategory = groupBy(
      filter(columns, (c: any) => c.id !== c.category_id),
      'category_id'
    );
    const gradebookStructure = map(orderedCategories, (cat: any, index: number) => {
      const columnIds = map(
        orderBy(
          filter(columnsByCategory[cat.id], (column: any) => {
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
      (state: CourseState) => state.ui[moduleConfig.sliceName],
      (state: CourseState) => state.api.users
    ),
    'list',
    undefined
  ),
  showExternalIds: (state: CourseState) => state.ui.gradebookTableOptions.showExternalIds,
  courseId: (state: CourseState) => state.course.id,
});

export const selectGradebookGradeBody = createStructuredSelector({
  learnerIds: (state: CourseState) => {
    if (state.ui[moduleConfig.sliceName].status.loading) {
      return [];
    } else {
      return state.ui[moduleConfig.sliceName].data.list;
    }
  },
});

export const selectTableDataLoaderComponent = createStructuredSelector({
  loadingState: (state: CourseState) => state.ui[moduleConfig.sliceName].status,
});

export const selectTableListControlComponent = createStructuredSelector({
  listState: createListStateSelector((state: CourseState) => state.ui[moduleConfig.sliceName]),
  showExternalIds: (state: CourseState) => state.ui.gradebookTableOptions.showExternalIds,
  showForCreditOnly: (state: CourseState) => state.ui.gradebookTableOptions.showForCreditOnly,
  gradeDisplayMethod: (state: CourseState) => state.ui.gradebookTableOptions.gradeDisplayMethod,
});
