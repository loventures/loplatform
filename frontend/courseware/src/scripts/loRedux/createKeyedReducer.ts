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

import { extend, isArray, mergeWith, omit } from 'lodash';
import { Action, AnyAction, Reducer } from 'redux';
import { PayloadActionCreator, createAction, isActionOf, isOfType } from 'typesafe-actions';

/**
 * Represents an object where the values are T
 */
export type StringMap<T> = { [key: string]: T };

const mergeArrayStrategy = (obj: any, toMerge: any): any => {
  if (isArray(obj)) {
    return toMerge;
  }
};

export function createKeyedReducer<T>(
  name: string
): [
  Reducer<StringMap<T>>,
  PayloadActionCreator<string, StringMap<T>>,
  PayloadActionCreator<string, StringMap<T>>,
  PayloadActionCreator<string, string>,
] {
  const updateMerge = createAction('DATA_LIST_UPDATE_MERGE_' + name)<StringMap<T>>();
  const updateReplace = createAction('DATA_LIST_UPDATE_REPLACE_' + name)<StringMap<T>>();
  const invalidate = createAction('DATA_LIST_INVALIDATE_' + name)<string>();

  const reducer: Reducer<StringMap<T>> = (state = {}, action) => {
    if (isActionOf(updateMerge, action)) {
      return {
        ...mergeWith({}, state, action.payload, mergeArrayStrategy),
      };
    } else if (isActionOf(updateReplace, action)) {
      return {
        ...extend({}, state, action.payload),
      };
    } else if (isActionOf(invalidate, action)) {
      return omit(state, action.payload);
    } else {
      return state;
    }
  };

  return [reducer, updateMerge, updateReplace, invalidate];
}

type IdentifiablePayload<T extends string> = Action & { [K in T]: string };

export const createMapReducer =
  <I extends string>(idProp: I) =>
  <S, A extends IdentifiablePayload<I>>(
    singleReducer: Reducer<S>,
    actionTypes: string[]
  ): Reducer<StringMap<S>> => {
    function isActionType(action: AnyAction): action is A {
      return isOfType(actionTypes, action);
    }

    const reducer = (state: StringMap<S> | undefined = {}, action: AnyAction) => {
      const id: string = action[idProp];
      if (id && isActionType(action)) {
        const nextState = singleReducer(state[id], action);
        if (nextState !== state[id]) {
          return {
            ...state,
            [id]: nextState,
          };
        }
      }
      return state;
    };

    return reducer;
  };
