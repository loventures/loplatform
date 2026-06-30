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

import { uniqBy } from 'lodash';
import { composableCombineReducers } from '../utilities/reduxify.js';

import {
  LIST_APPEND_DATA,
  LIST_CLEAR_SEARCH,
  LIST_CLEAR_SORT,
  LIST_LOAD_FAILED,
  LIST_LOAD_STARTED,
  LIST_SEARCH,
  LIST_SET_PAGE,
  LIST_SET_PAGE_SIZE,
  LIST_SORT,
  LIST_UPDATED_DATA,
} from './actionTypes.js';

const handleOrders = (orders: any, newOrder: any) => {
  const newOrders = Array.isArray(newOrder) ? newOrder : [newOrder];
  if (newOrders.some((order: any) => order.startNew)) {
    return [...newOrders];
  } else {
    return uniqBy([...newOrders, ...orders], (f: any) => f.property);
  }
};

const handleSearch = (searchConfig: any) => {
  if (typeof searchConfig.searchString !== 'string' || searchConfig.searchString.length === 0) {
    return { options: searchConfig.options };
  }
  return {
    ...searchConfig,
  };
};

export const optionsReducer = (state: any = { orders: [], search: {} }, action: any) => {
  switch (action.type) {
    case LIST_SORT:
      return {
        ...state,
        orders: handleOrders(state.orders, action.data),
        pageNumber: 1,
      };

    case LIST_CLEAR_SORT:
      return {
        ...state,
        orders: [],
        pageNumber: 1,
      };

    case LIST_SEARCH:
      return {
        ...state,
        search: handleSearch(action.data),
        pageNumber: 1,
      };

    case LIST_CLEAR_SEARCH:
      return {
        ...state,
        search: {},
        pageNumber: 1,
      };

    case LIST_SET_PAGE:
      return {
        ...state,
        pageNumber: action.data.pageNumber,
      };

    case LIST_SET_PAGE_SIZE:
      return {
        ...state,
        pageNumber: 1,
        pageSize: action.data.pageSize,
      };

    default:
      return state;
  }
};

export const dataReducer = (state: any = { list: [] }, action: any) => {
  switch (action.type) {
    case LIST_APPEND_DATA:
      return {
        list: action.data.list.concat(state.list),
        count: action.data.count + state.count,
        filterCount: action.data.filterCount,
        totalCount: action.data.totalCount,
      };

    case LIST_UPDATED_DATA:
      return action.data;

    default:
      return state;
  }
};

export const statusReducer = (state: any = {}, action: any) => {
  switch (action.type) {
    case LIST_SORT:
    case LIST_CLEAR_SORT:
    case LIST_SEARCH:
    case LIST_SET_PAGE:
      return {
        ...state,
        loaded: false,
      };

    case LIST_LOAD_STARTED:
      return {
        ...state,
        loading: true,
      };

    case LIST_LOAD_FAILED:
      return {
        ...state,
        loading: false,
        loaded: false,
        error: action.data.error,
      };

    case LIST_APPEND_DATA:
    case LIST_UPDATED_DATA:
      return {
        ...state,
        loading: false,
        loaded: true,
        error: action.data.error || null,
      };

    default:
      return state;
  }
};

export default composableCombineReducers({
  options: optionsReducer,
  data: dataReducer,
  status: statusReducer,
});
