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

/*
 * Minimal replacement for connected-react-router's `connectRouter` reducer. The app's reselect
 * selectors (selectRouter / selectCurrentUser / discussion / quiz print) read the current location
 * from `state.router.location`, so we keep mirroring the router location into redux — fed by a
 * `history.listen` subscription wired up in loRedux/index — rather than refactoring every selector
 * to a hook. We also parse the query string onto `location.query`, which those selectors expect.
 */

import { Action, Location } from 'history';
import qs from 'qs';

export interface RouterLocation {
  pathname: string;
  search: string;
  hash: string;
  state: unknown;
  key?: string;
  query: qs.ParsedQs;
}

export interface RouterState {
  location: RouterLocation;
  action: Action | null;
}

export const LOCATION_CHANGE = '@@router/LOCATION_CHANGE';

export interface LocationChangeAction {
  type: typeof LOCATION_CHANGE;
  payload: { location: Location; action: Action };
}

export const locationChange = (location: Location, action: Action): LocationChangeAction => ({
  type: LOCATION_CHANGE,
  payload: { location, action },
});

const toRouterState = (location: Location, action: Action | null): RouterState => ({
  location: {
    pathname: location.pathname,
    search: location.search,
    hash: location.hash,
    state: location.state,
    key: location.key,
    query: location.search ? qs.parse(location.search.replace(/^\?/, '')) : {},
  },
  action,
});

/* This slice of the router is a malign pattern. It is not in sync with the delayed rendered
 * components - changes will occur in redux before the UI updates, so UIs will render
 * based on their delayed router state, where redux will be responding with future state. All
 * the selectors that rely on the router should be handed the current router state from
 * the call site, so the redux store uses the same router state as the UI. */
export const createRouterReducer = (location: Location, action: Action | null) => {
  const initialState = toRouterState(location, action);
  return (state: RouterState = initialState, action: { type: string; payload?: any }): RouterState =>
    action.type === LOCATION_CHANGE
      ? toRouterState(action.payload.location, action.payload.action)
      : state;
};
