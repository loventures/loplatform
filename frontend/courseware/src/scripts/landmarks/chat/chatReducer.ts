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

import { AnyAction, Reducer } from 'redux';
import { createAction, isActionOf } from 'typesafe-actions';

export type chatState = {
  rooms: {
    [roomId: number]: {
      lastUpdated: number;
    };
  };
};

export const setChatRoomLastUpdatedAction = createAction('SET_CHAT_ROOM_LAST_UPDATED')<{
  roomId: number;
  lastUpdated: number;
}>();

const reducer: Reducer<chatState, AnyAction> = (state = { rooms: [] }, action) => {
  if (isActionOf(setChatRoomLastUpdatedAction, action)) {
    return {
      rooms: {
        ...state.rooms,
        [action.payload.roomId]: {
          lastUpdated: action.payload.lastUpdated,
        },
      },
    };
  } else {
    return state;
  }
};

export const chatReducer = reducer;
