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

import { DEFAULT_PAGE_SIZE } from '../components/PaginateWithMax.js';
import { extend, filter, flatMap, map, reduce } from 'lodash';

const srs = {
  defaultOptions: {
    pageSize: DEFAULT_PAGE_SIZE,
    pageNumber: 1,
  },

  parseSearchString(searchString) {
    const terms = reduce(
      searchString.split('"'),
      (list, string, index) => {
        if (index % 2) {
          return list.concat(string);
        } else {
          return list.concat(string.split(' '));
        }
      },
      []
    );

    return filter(terms, s => s.length);
  },

  fromListOptions(listOptions) {
    const options = extend({}, srs.defaultOptions, listOptions);

    const query = {
      limit: options.pageSize,
      offset: (options.pageNumber - 1) * options.pageSize,
      filters: [],
      prefilters: [],
      orders: [],
    };

    if (options.orders) {
      query.orders = map(options.orders, order => ({ ...order }));
    }

    if (options.search) {
      query.filterOp = options.search.operator;

      if (options.search.properties?.includes('user.all')) {
        query.filters = [
          ['fullName', 'ts', options.search.searchString],
          ['emailAddress', 'sw', options.search.searchString],
          ['externalId', 'eq', options.search.searchString],
          ['userName', 'eq', options.search.searchString],
        ];
      } else {
        const searchArray = options.search.shouldParseSearchString
          ? srs.parseSearchString(options.search.searchString)
          : [options.search.searchString];

        query.filters = flatMap(options.search.properties, prop =>
          map(searchArray, searchTerm => [prop, 'co', searchTerm])
        );
      }
    }

    return query;
  },
};

export default srs;
