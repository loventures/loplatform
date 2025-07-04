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

import { uniq, concat } from 'lodash';

import {
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_BOARD_LOAD_THREADS_RESET,
  DISCUSSION_BOARD_LOAD_THREADS_START,
  DISCUSSION_BOARD_LOAD_THREADS_SUCCESS,
  DISCUSSION_BOARD_LOAD_THREADS_ERROR,
} from '../actionTypes.js';

const boardThreadsReducer = (threads = { list: [] }, action) => {
  switch (action.type) {
    case DISCUSSION_WRITING_SAVE_SUCCESS:
      return {
        ...threads,
        list: concat([action.data.newItemId], threads.list),
        filterCount: threads.filterCount + 1,
      };

    case DISCUSSION_BOARD_LOAD_THREADS_RESET:
      return {
        ...threads,
        list: [],
      };
    case DISCUSSION_BOARD_LOAD_THREADS_START:
      return {
        ...threads,
        loading: true,
      };

    case DISCUSSION_BOARD_LOAD_THREADS_SUCCESS:
      return {
        ...threads,
        list: uniq(concat(threads.list, action.data.list)),
        filterCount: action.data.filterCount,
        loading: false,
      };

    case DISCUSSION_BOARD_LOAD_THREADS_ERROR:
      return {
        ...threads,
        loading: false,
        error: action.data.error,
      };

    default:
      return threads;
  }
};

export default boardThreadsReducer;
