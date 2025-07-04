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

import {
  DISCUSSION_BOARD_SEARCH_CLEAR,
  DISCUSSION_BOARD_SEARCH_START,
  DISCUSSION_BOARD_SEARCH_SUCCESS,
  DISCUSSION_BOARD_SEARCH_ERROR,
} from '../actionTypes.js';

const searchReducer = (search = {}, action) => {
  switch (action.type) {
    case DISCUSSION_BOARD_SEARCH_CLEAR:
      return {
        ...search,
        loading: false,
        loaded: false,
        searchString: '',
        searchResultIds: [],
        error: null,
      };

    case DISCUSSION_BOARD_SEARCH_START:
      return {
        ...search,
        loading: true,
        loaded: false,
        searchString: action.data.searchString,
      };

    case DISCUSSION_BOARD_SEARCH_SUCCESS:
      return {
        ...search,
        loading: false,
        loaded: true,
        searchResultTotal: action.data.total,
        searchResultIds: action.data.ids,
      };

    case DISCUSSION_BOARD_SEARCH_ERROR:
      return {
        ...search,
        loading: false,
        loaded: false,
        error: action.data.error,
      };

    default:
      return search;
  }
};

const discussionsSearchReducer = (views = {}, action) => {
  if (!action.discussionId) {
    return views;
  }

  switch (action.type) {
    case DISCUSSION_BOARD_SEARCH_CLEAR:
    case DISCUSSION_BOARD_SEARCH_START:
    case DISCUSSION_BOARD_SEARCH_SUCCESS:
    case DISCUSSION_BOARD_SEARCH_ERROR:
      return {
        ...views,
        [action.discussionId]: searchReducer(views[action.discussionId], action),
      };
    default:
      return views;
  }
};

export default discussionsSearchReducer;
