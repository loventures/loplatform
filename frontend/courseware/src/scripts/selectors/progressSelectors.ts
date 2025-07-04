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

import { Progress } from '../api/contentsApi.ts';
import { CourseState } from '../loRedux';
import { mapValues } from 'lodash';
import { selectCurrentUserId } from '../utilities/rootSelectors.ts';
import { createSelector } from 'reselect';

export type ProgressWithDetails = Progress & {
  isFullyCompleted: boolean;
  normalizedProgress: number;
};

export const selectProgressByContentByUser = (state: CourseState) =>
  state.api.progressByContentByUser;
const selectOverallProgressByUser = (state: CourseState) => state.api.overallProgressByUser;
const selectProgressLastActivityTimeByUser = (state: CourseState) =>
  state.api.progressLastActivityTimeByUser;

export const withProgressDetails = (progress: Progress = {} as Progress) => ({
  ...progress,
  isFullyCompleted: !!(progress.total && progress.completions === progress.total),
  normalizedProgress: progress.weightedPercentage / 100,
});

export const selectCurrentUserProgress = createSelector(
  [selectCurrentUserId, selectProgressByContentByUser],
  (userId, progressByContentByUser) => {
    return mapValues<Record<string, Progress>, ProgressWithDetails>(
      progressByContentByUser[userId],
      withProgressDetails
    );
  }
);

export const selectCurrentUserOverallProgress = createSelector(
  [selectCurrentUserId, selectOverallProgressByUser],
  (userId, overallProgressByUser) => {
    return withProgressDetails(overallProgressByUser[userId]);
  }
);

export const selectOverallProgressWithLastActivityByUser = createSelector(
  [selectOverallProgressByUser, selectProgressLastActivityTimeByUser],
  (overallProgressByUser, lastActivityTimeByUser) => {
    return mapValues(overallProgressByUser, (progress, userId) => {
      return {
        ...withProgressDetails(progress),
        lastActivityTime: lastActivityTimeByUser[userId],
      };
    });
  }
);
