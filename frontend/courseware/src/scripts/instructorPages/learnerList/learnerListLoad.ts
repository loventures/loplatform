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

import { UserInfo } from '../../../loPlatform';
import {
  LearnerListFilters,
  OverallProgress,
  SortColumn,
  SrsArray,
} from '../../instructorPages/learnerList/learnerListStore';
import { OverallGrade } from '../../loRedux/overallGradeByUser';
import * as preferences from '../../utilities/preferences';
import { lojector } from '../../loject';

const cmpGrade = (g1?: OverallGrade, g2?: OverallGrade) =>
  (g1?.grade ?? Number.MIN_VALUE) - (g2?.grade ?? Number.MIN_VALUE);

const overallGradeComparator = (asc: boolean) => (g1: OverallGrade, g2: OverallGrade) =>
  (cmpGrade(g1, g2) || g1.user_id - g2.user_id) * (asc ? 1 : -1);

const cmpProgress = (p1?: OverallProgress, p2?: OverallProgress) =>
  (p1?.progress._root_.weightedPercentage ?? Number.MIN_VALUE) -
  (p2?.progress._root_.weightedPercentage ?? Number.MIN_VALUE);

const overallProgressComparator = (asc: boolean) => (p1: OverallProgress, p2: OverallProgress) =>
  (cmpProgress(p1, p2) || p1.userId - p2.userId) * (asc ? 1 : -1);

const cmpLastActivity = (p1?: OverallProgress, p2?: OverallProgress) =>
  (p1?.lastModified ?? '').localeCompare(p2?.lastModified ?? '');

const lastActivityComparator = (asc: boolean) => (p1: OverallProgress, p2: OverallProgress) =>
  (cmpLastActivity(p1, p2) || p1.userId - p2.userId) * (asc ? 1 : -1);

export const needsManualSort = (column: SortColumn) =>
  column === 'PROGRESS' ||
  column === 'ACTIVITY' ||
  (column === 'GRADE' && preferences.useProjectedGrade);

const getLearnerListFilters = (
  { nameFormat, search, page, perPage, sort: { column, asc }, inactive }: LearnerListFilters,
  allGrades?: Record<number, OverallGrade>,
  allProgress?: Record<number, OverallProgress>
) => {
  let offset = (page - 1) * perPage;
  const limit = perPage;
  // if we are sorting by grade or progress we pull a list of student ids from
  // the overall grade/progress data. this is incompatible with filtering by
  // name, so we just drop sorting in that case.
  let filters: [string, string, string][];
  if (search !== '') {
    filters = [
      ['fullName', 'ts', search],
      ['emailAddress', 'sw', search],
      ['externalId', 'eq', search],
      ['userName', 'eq', search],
    ];
  } else if (needsManualSort(column)) {
    let ids: number[];
    if (column === 'GRADE') {
      const byGrade = allGrades == null ? [] : Object.values(allGrades);
      byGrade.sort(overallGradeComparator(asc));
      ids = byGrade.slice(offset, offset + limit).map(g => g.user_id);
    } else {
      const byProgress = allProgress == null ? [] : Object.values(allProgress);
      byProgress.sort(
        column === 'PROGRESS' ? overallProgressComparator(asc) : lastActivityComparator(asc)
      );
      ids = byProgress.slice(offset, offset + limit).map(g => g.userId);
    }
    offset = 0; // offset taken care of by slice
    filters = [['id', 'in', ids.join(',')]];
  } else {
    filters = [];
  }
  const order = asc ? 'ascNullsFirst' : 'descNullsLast';
  const nameOrder =
    nameFormat === 'FIRST_LAST'
      ? ['givenName', 'middleName', 'familyName']
      : ['familyName', 'givenName', 'middleName'];
  const properties =
    column === 'NAME'
      ? nameOrder
      : column === 'GRADE' && !preferences.useProjectedGrade
        ? ['overallGrade', ...nameOrder]
        : [];
  const orders = properties.map(property => ({ property, order }));
  return {
    prefilters: inactive ? [['', 'includeInactive', '']] : undefined,
    filters,
    filterOp: 'or',
    embed: inactive ? 'roles' : undefined,
    orders,
    offset,
    limit,
  };
};

export const loadLearnerListStudents = (
  filters: LearnerListFilters,
  allGrades?: Record<number, OverallGrade>,
  allProgress?: Record<number, OverallProgress>
): Promise<SrsArray<UserInfo>> => {
  return (lojector.get('enrolledUserService') as any)
    .getStudents(getLearnerListFilters(filters, allGrades, allProgress), undefined, true)
    .then((students: SrsArray<UserInfo>) => {
      // if we fetched students by PKs because sorting by overall grade/progress
      // then we need to resort them.
      if (needsManualSort(filters.sort.column)) {
        const baseCmp: (s1: UserInfo, s2: UserInfo) => number =
          filters.sort.column === 'GRADE'
            ? (s1, s2) => cmpGrade(allGrades?.[s1.id], allGrades?.[s2.id])
            : filters.sort.column === 'PROGRESS'
              ? (s1, s2) => cmpProgress(allProgress?.[s1.id], allProgress?.[s2.id])
              : (s1, s2) => cmpLastActivity(allProgress?.[s1.id], allProgress?.[s2.id]);
        students.sort((s1, s2) => {
          return (baseCmp(s1, s2) || s1.id - s2.id) * (filters.sort.asc ? 1 : -1);
        });
      }
      return students;
    });
};
