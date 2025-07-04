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

import { extend, isArray, keyBy, mergeWith } from 'lodash';
import { AnyAction, Reducer } from 'redux';

/**
 * The default merge strategy provided by lodash for arrays is closer to an object update:
 * merge( [1,2,3], []  ) => [1,2,3]
 * merge( [1,2,3], [4] ) => [4,2,3]
 *
 * We don't really treat arrays as objects so what we really want is a replace operation:
 * merge( [1,2,3], [] ) => []
 * merge( [1,2,3], [4] ) => [4]
 **/
const mergeArrayStrategy = (obj: any, toMerge: any) => {
  if (isArray(obj)) {
    return toMerge;
  }
};

function dataItemReducer(item: Record<string, any> = {}, action: AnyAction) {
  switch (action.type) {
    case 'DATA_ITEM_UPDATE':
      return {
        ...mergeWith({}, item, action.data.item, mergeArrayStrategy),
      };
    case 'DATA_ITEM_EXTEND':
      return {
        ...extend({}, item, action.data.item),
      };
    case 'DATA_ITEM_REPLACE':
      return {
        ...action.data.item,
      };
    default:
      return item;
  }
}

function dataListReducer<T extends Record<string, any>>(
  list: Record<string, T> = {} as Record<string, T>,
  action: AnyAction
) {
  //TODO deprecate this. calling actions should parse data correctly.
  let updatedList;
  if (action.data && Array.isArray(action.data.list)) {
    updatedList = keyBy(action.data.list, 'id');
  } else if (action.data) {
    updatedList = action.data.list;
  }

  switch (action.type) {
    case 'DATA_LIST_UPDATE_MERGE':
      return {
        ...mergeWith({}, list, updatedList, mergeArrayStrategy),
      };

    case 'DATA_LIST_UPDATE_REPLACE':
      return {
        ...updatedList,
      };

    case 'DATA_ITEM_INVALIDATE':
      delete list[action.id];
      return { ...list };

    case 'DATA_ITEM_UPDATE':
    case 'DATA_ITEM_EXTEND':
    case 'DATA_ITEM_REPLACE':
      return {
        ...list,
        [action.id]: dataItemReducer(list[action.id], action),
      };

    default:
      return list;
  }
}

/* This just wraps the annoying Record<string, ThingWeCareBout> */
export type DLR<T> = Reducer<Record<string, T>>;
export default dataListReducer as DLR<any>;
