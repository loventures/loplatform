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

import { CourseState } from '../loRedux';
import { clone, get, isFunction, reduce, setWith, toPairs, unzip } from 'lodash';
import { Action, AnyAction, Reducer, combineReducers } from 'redux';
import { Selector, createSelector } from 'reselect';

type SliceAction = Action & { sliceName: string; data: any; fullscreen: boolean };

function isSliceAction(action: Action): action is SliceAction {
  return action.hasOwnProperty('sliceName');
}

//normal combineReducers is not composable
//https://github.com/reactjs/redux/pull/2059
export const composableCombineReducers = (reducerMap: Record<string, Reducer>) => {
  const combined = combineReducers(reducerMap);

  return (state = {}, action: Action) => {
    const nextState = combined(state, action);

    if (nextState === state) {
      return state;
    }

    return {
      ...state,
      ...nextState,
    };
  };
};

export const createNamedReducer =
  <T>(sliceName: string, reducer: Reducer<T>): Reducer<T> =>
  (state: T | undefined, action: AnyAction) => {
    if (isSliceAction(action) && action.sliceName === sliceName) {
      return reducer(state, action);
    } else {
      //we still want to maintain shapes
      return reducer(state, {} as AnyAction);
    }
  };

/*
    This method creates a reducer meant for a slice that is an id map

    For example, if we want to dispatch an action that
    only affects a single discussion post, we dispatch something like
        {
            type: 'Some discussion post action type',
            id: [the discussion post id],
            data: { ... }
        }
    the id denotes the targeted discussion post.

    This method provides a reusable wrapper for that behavior.
    @param singleReducer: Reducer
        This is the reducer for the single item. For the above example, this would be
        a reducer for a single discusison post that handles the action type as dispatched
    @param idProp: string
        the name of the field to use as an id. defaults to 'id'.
    @return Reducer
        a reducer that applies singleReducer to one entry in a map of id to object

*/
export const createIdMapReducer =
  <T = any, A extends AnyAction = AnyAction>(singleReducer: Reducer<T, A>, idProp = 'id') =>
  (state: Record<string, T> = {}, action: any) => {
    const id = action[idProp];
    if (id) {
      const nextState = singleReducer(state[id], action);
      //extra care taken to avoid uneccessary updates to the object reference
      if (nextState !== state[id]) {
        return {
          ...state,
          [id]: nextState,
        };
      }
    }
    return state;
  };

/*
    Utility to create a selector for selecting a single slice by a key from a list.
    Can specify a default object if not found

    Examples:
    const fooForCurrentUserSelector =
        createInstanceSelector(
            mapOfUserIdToFooSelector,
            selectCurrentUserId.
            defaultFoo
        );
    this will use the value from `selectCurrentUserId` to select from `mapOfUserIdToFooSelector`
*/

type IdOrIdSelector = string | number | Selector<CourseState, string | number>;

type ListSelector<A> = Selector<CourseState, Record<string, A>>;

export const createInstanceSelector = <T>(
  listSelector: ListSelector<T>,
  listKeyOrListKeySelector: IdOrIdSelector,
  defaultObj: any = {}
) => {
  const listKeySelector = isFunction(listKeyOrListKeySelector)
    ? listKeyOrListKeySelector
    : () => listKeyOrListKeySelector;

  return createSelector(
    [listSelector, listKeySelector],
    (list = {}, key) => list[key] || defaultObj
  );
};

export const combineSelectors = (selectorsConfig: Record<string, any>) => {
  const [props, selectors] = unzip(toPairs(selectorsConfig));
  return createSelector([...selectors], (...selected) => {
    return reduce(
      props,
      (data: Record<string, any>, prop, index) => {
        data[prop] = selected[index];
        return data;
      },
      {}
    );
  });
};

export const createPropertySelector = (
  selector: Selector<any, any>,
  path: string | string[],
  defaultObj: any
) => createSelector(selector, selected => get(selected, path, defaultObj));

export const setReduxState = (state: Record<string, any>, path: string | string[], value: any) =>
  setWith(clone(state), path, value, src => {
    if (src === null) {
      return {};
    } else {
      return clone(src);
    }
  });
