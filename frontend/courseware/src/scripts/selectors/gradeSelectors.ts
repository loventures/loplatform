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

import { Grade } from '../api/contentsApi.ts';
import { GradebookCategory, GradebookColumn } from '../api/gradebookApi.ts';
import { CourseState } from '../loRedux';
import { OverallGrade } from '../loRedux/overallGradeByUser.ts';
import { filter, map, mapValues, sum, sumBy } from 'lodash';
import { CreditTypes, isForCredit } from '../utilities/creditTypes.ts';
import { selectCurrentUser } from '../utilities/rootSelectors.ts';
import { createSelector } from 'reselect';

export const selectGradesByContentByUser = (state: CourseState) => state.api.gradeByContentByUser;

export const selectOverallGradeByUser = (state: CourseState) => state.api.overallGradeByUser;

const selectRawGradebookColumns = (state: CourseState) => state.api.gradebookColumns;

export type GradeWithDetails = Grade & {
  pointsAwarded: number;
  pointsPossible: number;
  percent: number;
};

const withGradeDetails = (
  grade: any,
  pointsAwarded = grade.grade,
  pointsPossible = grade.maximumPoints,
  percent = pointsAwarded / pointsPossible
): GradeWithDetails => ({
  ...grade,
  pointsAwarded,
  pointsPossible,
  percent,
});

export const selectCurrentUserGrades = createSelector(
  selectCurrentUser,
  selectGradesByContentByUser,
  (currentUser: any, grades) => {
    return mapValues(grades[currentUser.id], g => withGradeDetails(g));
  }
);

export const selectGradebookColumns = createSelector(selectRawGradebookColumns, columns => {
  const categories = filter(columns, c => c.type === 'Category') as unknown as GradebookCategory[];
  const totalCategoryWeights = sumBy(categories, c => c.weight);
  const sumPoints = (cols: GradebookColumn[]) =>
    sum(
      map(cols, c => {
        if (isForCredit(c.credit)) {
          return c.maximumPoints;
        } else {
          return 0;
        }
      })
    );
  if (totalCategoryWeights > 0) {
    const categoryScale: Record<string, number> = {};
    for (const category of categories) {
      const categoryPoints = sumPoints(filter(columns, col => col.category_id === category.id));
      categoryScale[category.id] = category.weight / totalCategoryWeights / categoryPoints;
    }
    return mapValues(columns, column => {
      const scale = categoryScale[column.category_id] ?? 0;
      return {
        ...column,
        weight: column.credit === CreditTypes.NoCredit ? 0 : column.maximumPoints * scale,
      };
    });
  } else {
    const totalPoints = sumPoints(Object.values(columns));
    return mapValues(columns, column => {
      return {
        ...column,
        weight: column.credit === CreditTypes.NoCredit ? 0 : column.maximumPoints / totalPoints,
      };
    });
  }
});

export const selectCurrentUserOverallGrade = createSelector(
  selectCurrentUser,
  selectOverallGradeByUser,
  (currentUser, grades) => {
    return currentUser.id ? grades[currentUser.id] : ({} as OverallGrade);
  }
);
