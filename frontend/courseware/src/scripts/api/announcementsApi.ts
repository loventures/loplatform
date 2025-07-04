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

import { ApiQueryResults } from './commonTypes';

export type Announcement = {
  id: number;
  startTime: string;
  endTime: string;
  message: string;
  style: string;
  active: boolean;
};

export const getActiveAnnouncements = (): Promise<Announcement[]> =>
  axios
    .get<
      ApiQueryResults<Announcement>
    >(createUrl(loConfig.announcements.active, { context: Course.id }))
    .then(({ data }) => data.objects);

export const hideAnnouncement = (announcementId: number): Promise<void> =>
  axios
    .post<void>(
      createUrl(loConfig.announcements.hide, {
        context: Course.id,
        id: announcementId,
      })
    )
    .then(({ data }) => data);
