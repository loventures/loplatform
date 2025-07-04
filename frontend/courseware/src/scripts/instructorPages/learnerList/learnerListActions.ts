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
  LearnerListState,
  OverallProgress,
  SrsArray,
} from '../../instructorPages/learnerList/learnerListStore';
import { OverallGrade } from '../../loRedux/overallGradeByUser';
import { Dispatch } from 'react';

export const learnerListActions = {
  setAllGrades: 'SET_ALL_GRADES',
  setAllProgress: 'SET_ALL_PROGRESS',
  setFilters: 'SET_FILTERS',
  setGrades: 'SET_GRADES',
  setProgress: 'SET_PROGRESS',
  setStudents: 'SET_STUDENTS',
  toggleStudent: 'TOGGLE_STUDENT',
} as const;

export type LearnerListAction = {
  type: (typeof learnerListActions)[keyof typeof learnerListActions];
} & Record<string, any>;

export type LearnerListComponent = {
  state: LearnerListState;
  _dispatch: Dispatch<LearnerListAction>;
};

export const setAllGrades = (allGrades: Record<number, OverallGrade>): LearnerListAction => ({
  type: learnerListActions.setAllGrades,
  allGrades,
});

export const setAllProgress = (
  allProgress: Record<number, OverallProgress>
): LearnerListAction => ({
  type: learnerListActions.setAllProgress,
  allProgress,
});

export const setFilters = (filters: Partial<LearnerListFilters>): LearnerListAction => {
  if (filters.searchBy) localStorage.setItem('searchBy', filters.searchBy);
  if (filters.nameFormat) localStorage.setItem('nameFormat', filters.nameFormat);
  return {
    type: learnerListActions.setFilters,
    filters,
  };
};

export const setGrades = (
  students: SrsArray<UserInfo>,
  grades: Record<number, OverallGrade>
): LearnerListAction => ({
  type: learnerListActions.setGrades,
  students,
  grades,
});

export const setProgress = (
  students: SrsArray<UserInfo>,
  progress: Record<number, OverallProgress>
): LearnerListAction => ({
  type: learnerListActions.setProgress,
  students,
  progress,
});

export const setStudents = (
  filters: LearnerListFilters,
  students: SrsArray<UserInfo>
): LearnerListAction => ({
  type: learnerListActions.setStudents,
  filters,
  students,
});

export const toggleStudent = (student: number): LearnerListAction => ({
  type: learnerListActions.toggleStudent,
  student,
});
