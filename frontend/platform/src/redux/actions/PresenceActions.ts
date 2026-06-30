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

import { PresenceState } from '../reducers/PresenceReducer';

/*
 * action types
 */

export const SET_PRESENCE_STATE = 'SET_PRESENCE_STATE';
export const SET_IDLE_STATE = 'SET_IDLE_STATE';
export const SET_PRESENCE_ID = 'SET_PRESENCE_ID';

/*
 * action creators
 */

export function setPresenceState(state: Partial<PresenceState>) {
  return { type: SET_PRESENCE_STATE, state };
}

export function setIdleState(idling: boolean) {
  return { type: SET_IDLE_STATE, idling: idling };
}

export function setPresenceId(presenceId: string | null) {
  return { type: SET_PRESENCE_ID, presenceId: presenceId };
}
