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
  DISCUSSION_UPDATE_SETTINGS,
  DISCUSSION_BOARD_LOAD_THREADS_RESET,
  DISCUSSION_BOARD_LOAD_THREADS_START,
  DISCUSSION_BOARD_LOAD_THREADS_SUCCESS,
  DISCUSSION_BOARD_LOAD_THREADS_ERROR,
  DISCUSSION_WRITING_START,
  DISCUSSION_WRITING_SAVE_START,
  DISCUSSION_WRITING_SAVE_ERROR,
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_WRITING_DISCARD,
  DISCUSSION_WRITING_KEEP_WORKING,
  DISCUSSION_UPDATE_LAST_VISITED,
  DISCUSSION_UPDATE_LAST_VISITED_ERROR,
  DISCUSSION_BOARD_SORT,
} from '../actionTypes.js';

import boardThreadsReducer from './boardThreadsReducer.js';
import writingReducer from './writingReducer.js';

const defaultBoard = {
  settings: {},
  threads: boardThreadsReducer(void 0, {}),
  order: {},
  views: {},
  newThread: {},
};

const boardReducer = (board = defaultBoard, action) => {
  switch (action.type) {
    case DISCUSSION_UPDATE_LAST_VISITED:
      return {
        ...board,
        lastVisitedTime: action.data.lastVisitedTime,
        lastVisitedError: null,
      };

    case DISCUSSION_UPDATE_LAST_VISITED_ERROR:
      return {
        ...board,
        lastVisitedError: action.data.error,
        lastVisitedTime: null,
      };

    case DISCUSSION_UPDATE_SETTINGS:
      return {
        ...board,
        settings: {
          ...board.settings,
          ...action.data,
        },
      };

    case DISCUSSION_BOARD_SORT:
      return {
        ...board,
        order: {
          ...action.data,
        },
      };

    case DISCUSSION_BOARD_LOAD_THREADS_RESET:
    case DISCUSSION_BOARD_LOAD_THREADS_START:
    case DISCUSSION_BOARD_LOAD_THREADS_SUCCESS:
    case DISCUSSION_BOARD_LOAD_THREADS_ERROR:
      return {
        ...board,
        threads: boardThreadsReducer(board.threads, action),
      };

    case DISCUSSION_WRITING_START:
    case DISCUSSION_WRITING_SAVE_START:
    case DISCUSSION_WRITING_SAVE_ERROR:
    case DISCUSSION_WRITING_SAVE_SUCCESS:
    case DISCUSSION_WRITING_DISCARD:
    case DISCUSSION_WRITING_KEEP_WORKING:
      return {
        ...board,
        newThread: writingReducer(board.newThread, action),
        threads: boardThreadsReducer(board.threads, action),
      };
    default:
      return board;
  }
};

const boardsReducer = (boards = {}, action) => {
  if (action.discussionId) {
    return {
      ...boards,
      [action.discussionId]: boardReducer(boards[action.discussionId], action),
    };
  } else {
    return boards;
  }
};

export default boardsReducer;
