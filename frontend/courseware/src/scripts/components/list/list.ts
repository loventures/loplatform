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

import { ApiQueryResults } from '../../api/commonTypes';
import { isLoaded } from '../../types/loadable';
import { useLoadable } from '../../utils/loaderHooks';
import { useState } from 'react';

import { DEFAULT_PAGE_SIZE } from '../PaginateWithMax';
import { ListState, SortConfig } from './listTypes';

export const useList = <T>(
  loadFn: (
    searchString: string,
    activeSort: SortConfig | null,
    pageIndex: number,
    pageSize: number
  ) => Promise<ApiQueryResults<T>>,
  defaultSearchString = '',
  defaultSort: SortConfig | null = null,
  defaultPageSize: number = DEFAULT_PAGE_SIZE
): ListState<T> => {
  const [searchString, setSearchString] = useState(defaultSearchString);
  const [activeSort, setActiveSort] = useState(defaultSort);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(defaultPageSize);
  const [loadableQueryResults, doLoad] = useLoadable(() =>
    loadFn(searchString, activeSort, pageIndex, pageSize)
  );

  let filteredCount = 0;
  let totalCount = 0;
  if (isLoaded(loadableQueryResults)) {
    filteredCount = loadableQueryResults.data.filterCount;
    totalCount = loadableQueryResults.data.totalCount;
  }

  return {
    listLoadable: loadableQueryResults.map(r => r.objects),
    filteredCount,
    totalCount,
    searchString,
    setSearchString: str => {
      setPageIndex(0);
      setSearchString(str);
      doLoad(() => {
        return loadFn(str, activeSort, 0, pageSize);
      });
    },
    activeSort,
    setActiveSort: sort => {
      setPageIndex(0);
      setActiveSort(sort);
      doLoad(() => {
        return loadFn(searchString, sort, 0, pageSize);
      });
    },
    pageIndex,
    setPageIndex: index => {
      setPageIndex(index);
      doLoad(() => {
        return loadFn(searchString, activeSort, index, pageSize);
      });
    },
    pageSize,
    setPageSize: size => {
      setPageIndex(0);
      setPageSize(size);
      doLoad(() => {
        return loadFn(searchString, activeSort, 0, size);
      });
    },
  };
};
