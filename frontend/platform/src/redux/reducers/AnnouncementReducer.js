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
  ADD_ANNOUNCEMENT,
  DISABLE_ANNOUNCEMENT,
  SET_ANNOUNCEMENTS,
} from '../actions/AnnouncementActions.js';

const initialState = {
  announcements: [],
};

export default function announcement(state = initialState, action) {
  switch (action.type) {
    case SET_ANNOUNCEMENTS:
      return {
        ...state,
        announcements: [...action.announcements],
      };
    case ADD_ANNOUNCEMENT: {
      const announcements = [...state.announcements];
      const index = announcements.findIndex(ann => ann.id === action.announcement.id);
      if (index === -1) {
        announcements.push(action.announcement);
      } else {
        announcements[index] = action.announcement;
      }
      return { ...state, announcements };
    }
    case DISABLE_ANNOUNCEMENT: {
      const announcements = [...state.announcements];
      const index = announcements.findIndex(ann => ann.id === action.annId);
      if (index === -1) {
        return state;
      } else {
        announcements.splice(index, 1);
        return { ...state, announcements };
      }
    }
    default:
      return state;
  }
}
