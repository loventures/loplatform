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

import { Loadable, errored, loaded, loading } from '../types/loadable';
import { Reducer } from 'redux';
import { ThunkAction } from 'redux-thunk';
import {
  PayloadAction,
  PayloadMetaActionCreator,
  createAction,
  isActionOf,
} from 'typesafe-actions';

type AnyError = { error: any };

export type LoadableKeyedReducer<F extends (...args: any[]) => Promise<T>, T, K extends keyof T> = {
  loadingAction: PayloadMetaActionCreator<string, undefined, string>;
  loadedAction: PayloadMetaActionCreator<string, T, string>;
  erroredAction: PayloadMetaActionCreator<string, AnyError, string>;
  fetchKeyedAction: (
    key: string
  ) => (...p: Parameters<F>) => ThunkAction<Promise<T | void>, any, void, any>;
  modifyAction: PayloadMetaActionCreator<string, Pick<T, K>, string>;
  reducer: Reducer<Record<string, Loadable<T>>>;
};

export function createLoadableKeyedReducer<
  F extends (...args: any[]) => Promise<T>,
  T,
  K extends keyof T,
>(name: string, f: F): LoadableKeyedReducer<F, T, K> {
  type ErrorObj = { error: any };

  const loadingAction = createAction(`LOADING_${name}`)<undefined, string>();
  const loadedAction = createAction(`RECEIVED_${name}`)<T, string>();
  const erroredAction = createAction(`ERRORED_${name}`)<ErrorObj, string>();

  const fetchKeyedAction: (
    key: string
  ) => (...p: Parameters<F>) => ThunkAction<Promise<T | void>, any, void, any> =
    key =>
    (...args) => {
      return dispatch => {
        dispatch(loadingAction(undefined, key));
        return f(...args)
          .then(data => {
            dispatch(loadedAction(data, key));
            return data;
          })
          .catch(error => {
            dispatch(erroredAction({ error }, key));
          });
      };
    };

  const modifyAction = createAction(`MODIFY_${name}`)<Pick<T, K>, string>();

  const reducer: Reducer<Record<string, Loadable<T>>> = (state = {}, action) => {
    if (isActionOf(loadingAction, action)) {
      return {
        ...state,
        [action.meta]: loading,
      };
    } else if (isActionOf(loadedAction, action)) {
      const a = action as PayloadAction<string, T>;
      return {
        ...state,
        [action.meta]: loaded(a.payload),
      };
    } else if (isActionOf(erroredAction, action)) {
      return {
        ...state,
        [action.meta]: errored(action.payload.error),
      };
    } else if (isActionOf(modifyAction, action)) {
      return {
        ...state,
        [action.meta]: state[action.meta].map(survey => ({ ...survey, ...action.payload })),
      };
    } else {
      return state;
    }
  };

  return {
    loadingAction,
    loadedAction,
    erroredAction,
    fetchKeyedAction,
    modifyAction,
    reducer,
  };
}
