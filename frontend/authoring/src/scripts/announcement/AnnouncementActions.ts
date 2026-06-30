/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { Dispatch } from 'redux';

import gretchen from '../grfetchen/';

export interface Announcement {
  id: number;
  message: string;
  style: string;
  startTime: string;
  endTime: string;
}

/*
 * action types
 */

export const SET_ANNOUNCEMENTS = 'SET_ANNOUNCEMENTS';
export const ADD_ANNOUNCEMENT = 'ADD_ANNOUNCEMENT';
export const DISABLE_ANNOUNCEMENT = 'DISABLE_ANNOUNCEMENT';

export interface SetAnnouncementsAction {
  type: typeof SET_ANNOUNCEMENTS;
  announcements: Announcement[];
}

export interface AddAnnouncementAction {
  type: typeof ADD_ANNOUNCEMENT;
  announcement: Announcement;
}

export interface DisableAnnouncementAction {
  type: typeof DISABLE_ANNOUNCEMENT;
  annId: number;
}

export type AnnouncementAction =
  | SetAnnouncementsAction
  | AddAnnouncementAction
  | DisableAnnouncementAction;

/*
 * action creators
 */

export function fetchAnnouncements() {
  return function (dispatch: Dispatch) {
    gretchen
      .get('/api/v2/announcements/active')
      .exec()
      .then(res => dispatch(setAnnouncements(res.objects)));
  };
}

export function setAnnouncements(announcements: Announcement[]): SetAnnouncementsAction {
  return { type: SET_ANNOUNCEMENTS, announcements: announcements };
}

export function addAnnouncement(announcement: Announcement): AddAnnouncementAction {
  return { type: ADD_ANNOUNCEMENT, announcement: announcement };
}

export function disableAnnouncement(announcementId: number): DisableAnnouncementAction {
  return { type: DISABLE_ANNOUNCEMENT, annId: announcementId };
}
