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
import { ApiQueryResults } from '../../api/commonTypes';
import { OverallGrade } from '../../loRedux/overallGradeByUser';

export type NameFormatter = (learner: UserInfo) => string;

export type SearchBy = 'NAME' | 'EMAIL' | 'EXTERNAL_ID';

export type SortColumn = 'NAME' | 'ACTIVITY' | 'PROGRESS' | 'GRADE';

export type SortOrder = {
  column: SortColumn;
  asc: boolean;
};

export type LearnerListFilters = {
  nameFormat: NameFormat;
  search: string;
  searchBy: SearchBy;
  sort: SortOrder;
  page: number;
  perPage: number;
  inactive: boolean;
};

// What light through yonder window breaks? It is the east, and course-lw is the sun.
// Arise, fair sun, and kill the envious moon.
export type SrsArray<T> = Array<T> & Omit<ApiQueryResults<T>, 'objects'>;

export type NameFormat = 'FIRST_LAST' | 'LAST_FIRST';

export type OverallProgress = {
  userId: number;
  lastModified: string;
  progress: Record<
    '_root_',
    {
      completions: number;
      total: number;
      weightedPercentage: number;
      progressTypes: any[];
    }
  >;
};

export type LearnerListState = {
  filters: LearnerListFilters;
  students?: SrsArray<UserInfo>;
  grades?: Record<number, OverallGrade>;
  progress?: Record<number, OverallProgress>;
  studentCount?: number; // total students to decide whether total progress ordering is available
  selectedStudents: Set<number>;
  allGrades?: Record<number, OverallGrade>;
  allProgress?: Record<number, OverallProgress>;
};

const locallyStored = <A>(key: string, a: A): A => (localStorage.getItem(key) as A | null) ?? a;

export const learnerListInitialState = (): LearnerListState => ({
  filters: {
    nameFormat: locallyStored<NameFormat>('nameFormat', 'LAST_FIRST'),
    search: '',
    searchBy: locallyStored<SearchBy>('searchBy', 'NAME'),
    sort: {
      column: 'NAME',
      asc: true,
    },
    page: 1,
    perPage: 10,
    inactive: false,
  },
  selectedStudents: new Set(),
});

export type LearnerTableRecord = {
  learner: UserInfo;
  grade?: OverallGrade;
  progress?: OverallProgress;
};
