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

import { OverallGrade } from '../loRedux/overallGradeByUser';
import { find, get, isEqual, isFinite } from 'lodash';

import { CourseState, courseReduxStore } from '../loRedux';
import { postScormMessage } from './scorm';

type CompletionState = {
  overallGrade?: OverallGrade; // last posted overall grade
  incomplete?: boolean; // whether incompleteness has been posted
};

const completion: CompletionState = {};

/** Check global redux state for completion. */
export const checkCourseCompletionState = (): void => {
  const state: CourseState = courseReduxStore.getState();
  const userId = state.actualUser.id;
  const overallGrade = state.api.overallGradeByUser[userId];
  if (
    overallGrade &&
    isFinite(overallGrade.grade) &&
    !isEqual(overallGrade, completion.overallGrade)
  ) {
    const getGrade = (cid: string) => get(state.api.gradeByContentByUser, [userId, cid], {});
    const ungraded = find(
      state.api.contentItems,
      ci => ci.hasGradebookEntry && !isFinite(getGrade(ci.id).grade)
    );
    if (ungraded) {
      if (!completion.incomplete) {
        completion.incomplete = true;
        postScormMessage({ _type: 'INCOMPLETE' });
      }
    } else {
      completion.overallGrade = overallGrade;
      postScormMessage({ ...overallGrade, _type: 'COMPLETE' });
    }
  }
};
