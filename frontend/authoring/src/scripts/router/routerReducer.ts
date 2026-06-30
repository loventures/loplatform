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

import { Action as HistoryAction, Location } from 'history';

/*
 * Minimal in-repo replacement for connected-react-router's `connectRouter` reducer (which only
 * supports react-router v4/v5). It keeps `state.router.location` in sync with the history singleton
 * via `history.listen` (wired up in dcmStore), exposing the same `{ location, action }` shape the
 * old reducer did so the existing `state.router.location.pathname`/`.search` readers keep working.
 *
 * Note: history v5's createBrowserHistory has no basename, so the wiring in dcmStore strips the
 * router basename from `location.pathname` before it lands here — matching what history v4 +
 * connected-react-router used to store (a basename-relative pathname the route patterns match).
 */
export const LOCATION_CHANGE = '@@router/LOCATION_CHANGE';

export interface RouterState {
  location: Location;
  action: HistoryAction;
}

export const locationChange = (location: Location, action: HistoryAction) => ({
  type: LOCATION_CHANGE as typeof LOCATION_CHANGE,
  payload: { location, action },
});

export const createRouterReducer =
  (initialLocation: Location, initialAction: HistoryAction) =>
  (
    state: RouterState = { location: initialLocation, action: initialAction },
    action: ReturnType<typeof locationChange>
  ): RouterState =>
    action.type === LOCATION_CHANGE
      ? { location: action.payload.location, action: action.payload.action }
      : state;
