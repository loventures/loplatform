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

import { Loadable } from '../../types/loadable';

//The full path to the field you are searching for/sorting on,
//i.e. if you are searching for deeply nested information like `overview.learner.firstName`
//
//But why would you do that? and why not just store that as a string `overview.learner.firstName`
export type Path = string | string[];

//why would you use another separator?!
export const pathToString = (path: Path, separator = '.') => {
  if (typeof path === 'string') {
    return path;
  } else {
    return path.join(separator);
  }
};

export type SortConfig = {
  i18nKey: string;
  field: Path;
  direction: 'asc' | 'desc';
};

export type SearchConfig = {
  i18nKey: string;
  searchFields: Path[];
};

export type ListState<T> = {
  listLoadable: Loadable<T[]>;
  filteredCount: number;
  totalCount: number;
  searchString: string;
  setSearchString: (s: string) => void;
  activeSort: SortConfig | null;
  setActiveSort: (s: SortConfig) => void;
  pageIndex: number;
  setPageIndex: (s: number) => void;
  pageSize: number;
  setPageSize: (s: number) => void;
};
