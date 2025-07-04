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

import {
  LearnerListAction,
  learnerListActions,
} from '../../instructorPages/learnerList/learnerListActions';
import { LearnerListState } from '../../instructorPages/learnerList/learnerListStore';

export default (state: LearnerListState, action: LearnerListAction): LearnerListState => {
  switch (action.type) {
    case learnerListActions.setAllGrades:
      return {
        ...state,
        allGrades: action.allGrades,
      };
    case learnerListActions.setAllProgress:
      return {
        ...state,
        allProgress: action.allProgress,
      };
    case learnerListActions.setFilters:
      return {
        ...state,
        filters: {
          ...state.filters,
          ...action.filters,
        },
        students: undefined,
        grades: undefined,
        progress: undefined,
        selectedStudents: new Set(),
      };
    case learnerListActions.setStudents:
      return action.filters !== state.filters
        ? state
        : {
            ...state,
            students: action.students,
            studentCount: action.students.totalCount,
          };
    case learnerListActions.setGrades:
      return action.students !== state.students ? state : { ...state, grades: action.grades };
    case learnerListActions.setProgress:
      return action.students !== state.students ? state : { ...state, progress: action.progress };
    case learnerListActions.toggleStudent: {
      const selectedStudents = new Set(state.selectedStudents);
      if (!selectedStudents.delete(action.student)) selectedStudents.add(action.student);
      return {
        ...state,
        selectedStudents,
      };
    }
    default:
      return state;
  }
};
