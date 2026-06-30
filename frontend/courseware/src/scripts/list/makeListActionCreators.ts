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

import type { CourseState } from '../loRedux';
import { flatMap, map } from 'lodash';
import { batchActions } from 'redux-batched-actions';

import {
  LIST_APPEND_DATA,
  LIST_LOAD_FAILED,
  LIST_LOAD_STARTED,
  LIST_SEARCH,
  LIST_SET_PAGE,
  LIST_SET_PAGE_SIZE,
  LIST_SORT,
  LIST_UPDATED_DATA,
} from './actionTypes.js';

const actionOrThunk = (action: any, loadingActionCreator: any) => {
  if (loadingActionCreator) {
    return (dispatch: any, getState: () => CourseState) => {
      dispatch(action);
      dispatch(loadingActionCreator(getState));
    };
  } else {
    return action;
  }
};

export const makeSortActionCreator = (config: any, data: any, loadingActionCreator?: any) => {
  return () =>
    actionOrThunk(
      {
        ...config,
        type: LIST_SORT,
        data,
      },
      loadingActionCreator
    );
};

export const makeVerbatimSearchActionCreator = (
  config: any,
  { properties, operator = 'or', options = undefined }: any,
  loadingActionCreator?: any
) => {
  return (searchString: any) =>
    actionOrThunk(
      {
        ...config,
        type: LIST_SEARCH,
        data: {
          operator,
          properties,
          searchString,
          shouldParseSearchString: false,
          options,
        },
      },
      loadingActionCreator
    );
};

export const makeQuotedSearchActionCreator = (
  config: any,
  { properties, operator = 'and' }: any,
  loadingActionCreator?: any
) => {
  if (properties.length > 1) {
    console.warn('only the first property is considered in this search', properties);
  }

  properties = [properties[0]];

  return (searchString: any) =>
    actionOrThunk(
      {
        ...config,
        type: LIST_SEARCH,
        data: {
          operator,
          properties,
          searchString,
          shouldParseSearchString: true,
        },
      },
      loadingActionCreator
    );
};

export const makeSetPageSizeActionCreator = (config: any, loadingActionCreator?: any) => {
  return (pageSize: any) =>
    actionOrThunk(
      {
        ...config,
        type: LIST_SET_PAGE_SIZE,
        data: {
          pageSize,
        },
      },
      loadingActionCreator
    );
};

export const makePagingActionCreator = (config: any, loadingActionCreator?: any) => {
  return (pageNumber: any) =>
    actionOrThunk(
      {
        ...config,
        type: LIST_SET_PAGE,
        data: {
          pageNumber,
        },
      },
      loadingActionCreator
    );
};

export const makeAppendActionCreator = (config: any) => {
  return ({ list, count, filterCount, totalCount }: any) => ({
    ...config,
    type: LIST_APPEND_DATA,
    data: {
      list,
      count,
      filterCount,
      totalCount,
    },
  });
};

export const makeUpdateActionCreator = (config: any) => {
  return ({ list, count, filterCount, totalCount }: any) => {
    const action = {
      ...config,
      type: LIST_UPDATED_DATA,
      data: {
        list: map(list, 'id'),
        count,
        filterCount,
        totalCount,
      },
    };
    return action;
  };
};

export const makeLoadStartActionCreator = (config: any) => {
  return () => ({
    ...config,
    type: LIST_LOAD_STARTED,
  });
};

export const makeLoadFailedActionCreator = (config: any) => {
  return (error: any) => ({
    ...config,
    type: LIST_LOAD_FAILED,
    data: {
      error,
    },
  });
};

export const makeListLoadActionCreator = (
  config: any,
  loadFn: any,
  isAppend: any,
  additionalSuccessActionCreators: any[] = [],
  resultsParser = (data: any) => data.slice()
) => {
  const createLoadStartAction = makeLoadStartActionCreator(config);
  const createLoadFailedAction = makeLoadFailedActionCreator(config);

  const createLoadSuccessAction = isAppend
    ? makeAppendActionCreator(config)
    : makeUpdateActionCreator(config);

  return (...args: any[]) =>
    (dispatch: any) => {
      dispatch(createLoadStartAction());

      loadFn(...args).then(
        (data: any) => {
          return dispatch(
            batchActions([
              ...flatMap(map(additionalSuccessActionCreators, ac => ac(data))),
              createLoadSuccessAction({
                list: resultsParser(data),
                count: data.count,
                filterCount: data.filterCount,
                totalCount: data.totalCount,
              }),
            ])
          );
        },
        (error: any) => dispatch(createLoadFailedAction(error))
      );
    };
};
