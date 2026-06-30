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
import { map } from 'lodash';

import { createSelector } from 'reselect';
import { DEFAULT_PAGE_SIZE } from '../components/PaginateWithMax.js';

export const selectListData = (stateSlice: any, data: any) => {
  if (stateSlice.status.loading) {
    return { list: [] };
  } else {
    const result = map(stateSlice.data.list, id => data[id]);
    return { list: result };
  }
};

export const selectListState = ({
  data = { totalCount: 0 },
  status = {},
  options = { search: {}, orders: [] },
}: any) => ({
  data,
  status,
  options,
  loadingState: status,
  activeOptions: {
    searchString: options.search.searchString,
    options: options.search.options,
    sortKey: options.orders[0],
    currentPage: options.pageNumber || 1,
    totalItems: data.filterCount || 0,
    pageSize: options.pageSize || DEFAULT_PAGE_SIZE,
    totalPages: Math.ceil((data.filterCount || 0) / (options.pageSize || DEFAULT_PAGE_SIZE)),
  },
});

export const createListDataSelector = (stateSelector: any, dataSelector: any) =>
  createSelector([stateSelector, dataSelector], selectListData);

export const createListStateSelector = (listSliceSelector: any) =>
  createSelector([listSliceSelector], selectListState);

export const createListSelectorFromSelectors = (
  selectListSlice: any,
  selectDataSlice: any
): ((state: CourseState) => any) => {
  const stateSelector = createListStateSelector(selectListSlice);
  const dataSelector = createListDataSelector(selectListSlice, selectDataSlice);

  return createSelector([stateSelector, dataSelector], (listState: any, listData: any) => ({
    listState,
    list: listData.list,
  })) as unknown as (state: CourseState) => any;
};

export const createListSelector = (sliceName: string, dataSliceName: string) => {
  const selectListSlice = (state: CourseState) => state.ui[sliceName];
  const selectDataSlice = (state: CourseState) => state.api[dataSliceName];

  return createListSelectorFromSelectors(selectListSlice, selectDataSlice);
};
