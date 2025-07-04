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

import { Either, left, right } from 'fp-ts/es6/Either';
import { Loadable, errored, loaded, loading } from '../types/loadable';
import { Reducer } from 'redux';
import { ThunkAction } from 'redux-thunk';
import {
  EmptyActionCreator,
  PayloadActionCreator,
  createAction,
  isActionOf,
} from 'typesafe-actions';

type AnyError = { error: any };

export type LoadableReducer<F extends (...args: any[]) => Promise<T>, T> = {
  loadingAction: EmptyActionCreator<string>;
  loadedAction: PayloadActionCreator<string, T> | EmptyActionCreator<string>;
  erroredAction: PayloadActionCreator<string, AnyError> | EmptyActionCreator<string>;
  fetchAction: (...p: Parameters<F>) => ThunkAction<Promise<Either<any, T>>, any, void, any>;
  reducer: Reducer<Loadable<T>>;
};

export function createLoadableReducer<F extends (...args: any[]) => Promise<T>, T>(
  name: string,
  f: F
): LoadableReducer<F, T> {
  const loadingAction = createAction(`LOADING_${name}`)();
  const loadedAction = createAction(`RECEIVED_${name}`)<T>();
  const erroredAction = createAction(`ERRORED_${name}`)<AnyError>();

  const fetchAction: (
    ...p: Parameters<F>
  ) => ThunkAction<Promise<Either<any, T>>, any, void, any> = (...args) => {
    return dispatch => {
      dispatch(loadingAction());
      return f(...args)
        .then(data => {
          dispatch(loadedAction(data));
          return right<any, T>(data);
        })
        .catch(error => {
          dispatch(erroredAction({ error }));
          return left<any, T>(error);
        });
    };
  };

  const reducer: Reducer<Loadable<T>> = (state = loading, action) => {
    if (isActionOf(loadingAction, action)) {
      return loading;
    } else if (isActionOf(loadedAction, action)) {
      // NOTE: because action ids are dynamic with ${name} type narrowing cannot happen in this case.
      return loaded((action as any).payload);
    } else if (isActionOf(erroredAction, action)) {
      return errored(action.payload.error);
    } else {
      return state;
    }
  };

  return {
    loadingAction,
    loadedAction,
    erroredAction,
    fetchAction,
    reducer,
  };
}
