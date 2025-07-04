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

export type AttemptsOverview = {
  context: number;
  edgePath: string;
  studentAttempts: {
    [studentId: string]: {
      Open: number;
      Submitted: number;
      Finalized: number;
    };
  };
};

export function fetchAttemptOverviews(
  courseId: number,
  contentIds: string[]
): Promise<AttemptsOverview[]> {
  const contents = contentIds.join(',');
  // loConfig.overview.instructorAttemptsOverview
  return axios
    .get(`/api/v2/assessment/instructorAttemptsOverview;context=${courseId};paths=${contents}`)
    .then(({ data }) => data);
}

export function updatePointsPossible(
  courseId: number,
  columnId: string,
  pointsPossible: number
): Promise<{ pointsPossible: number }> {
  // loConfig.instructorCustomization.customizePoints
  return axios
    .post(`/api/v2/contentConfig/pointsPossible/${columnId};context=${courseId}`, {
      pointsPossible,
    })
    .then(({ data }) => data);
}
