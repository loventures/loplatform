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

/*
    This module provides utility for fully loaded lists to appear as a paginated list
    similar to what LocalResourceStore provides for ResourceStore
*/

import dayjs from 'dayjs';
import {
  every,
  filter,
  get,
  includes,
  startsWith,
  isFunction,
  isNil,
  map,
  orderBy,
  some,
  values,
} from 'lodash';
import srs from '../utilities/srs.js';
import naturalCompare from 'natural-compare';

import { selectListState } from './createListSelectors.js';

const standardOps = {
  eq: (val, comp) => val === comp,
  ne: (val, comp) => val !== comp,
  co: (val, comp) => includes(val, comp),
  nc: (val, comp) => !includes(val, comp),
  sw: (val, comp) => startsWith(val, comp),
  ts: (val, comp) =>
    comp
      .split(/\s+/)
      .filter(s => s)
      .every(s => includes(val, s)), // kinda
};

const transforms = {
  time: val => dayjs(val).valueOf(),
};

const fixFilterValue = val => {
  if (typeof val === 'string') {
    return val.toLowerCase();
  } else {
    return val;
  }
};

const fixOrderValue = (item, order) => {
  const val = get(item, order.property);
  if (order.transform) {
    if (transforms[order.transform]) {
      return transforms[order.transform](val);
    } else if (isFunction(order.transform)) {
      return order.transform(val);
    }
  }

  if (typeof val === 'string') {
    return val.toLowerCase();
  } else if (isNil(val)) {
    return -Infinity;
  } else {
    return val;
  }
};

/*
    this selects a sub list of data from a full list based on standard list options.
    @param listState
        the list state from the standard list shape.
        This method only uses the options slice
    @param allData
        the entire list of data.
        This can be either an object (as most data slices are) or an array.
    @param customOps
        if you need to use something outside of standardOps,
        use customOps to pass in the handler and designate it in the options.
*/
export const selectLocalListData = ({ options }, allData, customOps = {}) => {
  const query = srs.fromListOptions(options);
  return applyQuery(query, allData, customOps);
};

export const applyQuery = (query, allData, customOps = {}) => {
  const opsMap = {
    ...standardOps,
    ...customOps,
  };

  const allDataArr = values(allData);
  const filteredData = filter(allDataArr, item => {
    const filtersOp = query.filterOp === 'or' ? some : every;
    return filtersOp(query.filters, ([path, filterOp, opValue]) => {
      const opMethod = opsMap[filterOp];
      if (!opMethod) {
        console.error('unsupported filter op', filterOp);
      }
      return opMethod(fixFilterValue(get(item, path)), fixFilterValue(opValue));
    });
  });

  const iteratees = map(query.orders, order => item => fixOrderValue(item, order));
  const directions = map(query.orders, 'order');
  let orderedData;
  // In principle we could support a mix of naturally and unnaturally sorted properties but in practice
  // there is only a single use case where there is a single naturally sorted property and so this.
  if (query.orders.length === 1 && query.orders[0].naturally) {
    const iteratee = iteratees[0];
    const direction = directions[0] !== 'asc' ? -1 : 1;
    orderedData = filteredData.sort(
      (a0, a1) => direction * naturalCompare(iteratee(a0), iteratee(a1))
    );
  } else {
    orderedData = orderBy(filteredData, iteratees, directions);
  }
  const results = orderedData.slice(query.offset, query.offset + query.limit);

  results.count = results.length;
  results.filterCount = filteredData.length;
  results.totalCount = allDataArr.length;

  return results;
};

/*
    this selects a sub list of data from a full list based on standard list options,
    and will also modify list state to reflect the data selected.
    the returned state has the same shape as returned by the selector
    created from "createListSelector" in the createListSelector.js file.

    @param listState
        the list state from the standard list shape.
        This method only uses the options slice
    @param allData
        the entire list of data.
        This can be either an object (as most data slices are) or an array.
    @param customOps
        if you need to use something outside of standardOps,
        use customOps to pass in the handler and designate it in the options.
*/
export const selectLocalListDataAndState = (rawState, allData) => {
  const list = selectLocalListData(rawState, allData);

  const listState = selectListState({
    ...rawState,
    data: {
      count: list.count,
      filterCount: list.filterCount,
      totalCount: list.totalCount,
    },
    status: {
      loaded: true,
    },
  });
  return {
    list,
    listState,
  };
};
