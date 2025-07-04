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

import { SET_IDLE_STATE, SET_PRESENCE_ID, SET_PRESENCE_STATE } from '../actions/PresenceActions.js';

let initialState = {
  started: false, // Has this service been started; if so, it may connect to the server
  online: false, // Is presence online
  offline: false, // Is presence offline (broken),
  idling: false,
  presenceId: null,
};

export default function presence(state = initialState, action) {
  switch (action.type) {
    case SET_PRESENCE_STATE:
      return Object.assign({}, state, {
        started: action.state.started,
        online: action.state.online,
        offline: action.state.offline,
        idling: action.state.idling,
      });
    case SET_IDLE_STATE:
      return Object.assign({}, state, {
        idling: action.idling,
      });
    case SET_PRESENCE_ID:
      return Object.assign({}, state, {
        presenceId: action.presenceId,
      });
    default:
      return state;
  }
}
