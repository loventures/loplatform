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

import axios from 'axios';
import Course from '../bootstrap/course';
import { createUrl, loConfig } from '../bootstrap/loConfig';
import { createDataListUpdateMergeAction } from '../utilities/apiDataActions';
import { AnyAction } from 'redux';

export type AttemptOverview = {
  allAttempts: number;
  latestSubmissionTime: Date;
  maxAttempts: null | number;
  edgePath: string;
  userId: number;
  context: number;
  openAttempts: number;
};

export const getAttemptOverviews = (
  ids: string[],
  userId: number,
  contextId = Course.id
): Promise<AttemptOverview[]> => {
  const url = createUrl(loConfig.overview.attemptOverviews, {
    contextId,
    userId,
    paths: ids.join(','),
  });

  return axios.get<AttemptOverview[]>(url).then(res => res.data);
};

// todo: no need to curry studentId right?
export const getActivitiesOnlyResponse = (studentId: number) => (overviews: AttemptOverview[]) => {
  const activities = overviews.reduce<Record<string, { attemptOverview: AttemptOverview }>>(
    (acc, overview) => ({
      ...acc,
      [overview.edgePath]: { attemptOverview: overview },
    }),
    {}
  );
  return {
    studentId,
    contentUserData: {
      activities,
    },
  };
};

// TODO: unify this with contentActivityReducer.js
export const activityLoadedActionCreator = (activityByContent: any, userId: number): AnyAction =>
  createDataListUpdateMergeAction('activityByContentByUser', {
    [userId]: activityByContent,
  });
