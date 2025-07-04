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
import { ContentIdentifier, ContextId, EdgePath, SrsList } from '../api/commonTypes';
import Course from '../bootstrap/course';
import { createUrl, loConfig } from '../bootstrap/loConfig';
import { orderBy } from 'lodash';
import { useMemo } from 'react';

export type GradingUserOverview = {
  learner: {
    id: number;
    userName: string;
    familyName: string;
    givenName: string;
    middleName: string;
  };
  mostRecentSubmission: string;
  gradeableAttempts: number[];
  attemptCount: number;
  invalidAttemptCount: number;
  hasViewableAttempts: boolean;
  hasValidViewableAttempts: boolean;
  grade: {
    scoreType: string;
    pointsAwarded: number;
    pointsPossible: number;
  };
};

export type GradingApi = {
  loadUsersOverviews: (contentId: string) => Promise<GradingUserOverview[]>;
};

export const useGradingApi = (): GradingApi => {
  const gretchen = axios; //useContext(gretchen);
  const contextId = Course.id;
  return useMemo(
    () => ({
      loadUsersOverviews: (contentId: string) => {
        // loConfig.quiz.overview
        return gretchen
          .get<
            SrsList<GradingUserOverview>
          >(`/api/v2/assessment/${contextId}.${contentId}/gradingOverview`)
          .then(response => {
            return orderBy(response.data.objects, go => {
              return go.learner.givenName;
            });
          });
      },
    }),
    [gretchen]
  );
};

export type ParticipationOverview = {
  contentId: ContentIdentifier;
  participantCount: number;
  actionItemCount: number;
  mostRecentInteraction: string | null;
};

export type GradingQueue = {
  context: ContextId;
  edgePath: EdgePath;
  overview: ParticipationOverview;
};

export const getGradingQueue = (contextId = Course.id): Promise<Array<GradingQueue>> => {
  const url = createUrl(loConfig.overview.gradingQueue, {
    contextId,
  });
  return axios.get<Array<GradingQueue>>(url).then(res => res.data);
};
