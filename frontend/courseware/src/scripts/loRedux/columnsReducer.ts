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

import { Column, fetchColumns } from '../api/gradebookApi';
import { Loadable, errored, loaded, loading } from '../types/loadable';
import { AnyAction, Reducer } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { createAction, isActionOf } from 'typesafe-actions';

export type ColumnsState = Loadable<Column[]>;

export const loadingAction = createAction('LOADING_COLUMNS')();
export const loadedAction = createAction('RECEIVED_COLUMNS')<Column[]>();
export const erroredAction = createAction('RECEIVED_COLUMNS_ERROR')<{
  error: any;
}>();
export const updatePointsPossibleAction = createAction('UPDATE_COLUMN_POINTS_POSSIBLE')<{
  columnId: string;
  points: number;
}>();

export const columnsReducer: Reducer<ColumnsState, AnyAction> = (state = loading, action) => {
  if (isActionOf(loadingAction, action)) {
    return loading;
  } else if (isActionOf(loadedAction, action)) {
    return loaded(action.payload);
  } else if (isActionOf(erroredAction, action)) {
    return errored(action.payload.error);
  } else if (isActionOf(updatePointsPossibleAction, action)) {
    return state.map(columns =>
      columns.map(column => {
        if (column.id === action.payload.columnId) {
          return { ...column, maximumPoints: action.payload.points };
        } else {
          return column;
        }
      })
    );
  } else {
    return state;
  }
};

export const fetchColumnsAction: (courseId: number) => ThunkAction<any, any, void, any> =
  (courseId: number) => dispatch => {
    dispatch(loadingAction());
    fetchColumns(courseId)
      .then(columns => {
        dispatch(loadedAction(columns.objects));
      })
      .catch(({ response }: any) => {
        dispatch(erroredAction({ error: response.data }));
      });
  };
