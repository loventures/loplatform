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
  DISCUSSION_WRITING_START,
  DISCUSSION_WRITING_SAVE_START,
  DISCUSSION_WRITING_SAVE_ERROR,
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_WRITING_DISCARD,
  DISCUSSION_THREAD_LOAD_REPLIES_START,
  DISCUSSION_THREAD_LOAD_REPLIES_SUCCESS,
  DISCUSSION_THREAD_LOAD_REPLIES_ERROR,
  DISCUSSION_THREAD_TOGGLE_REPLIES_EXPANSION,
  DISCUSSION_POST_BATCH_SET_BODY_EXPANSION,
} from '../actionTypes.js';

import writingReducer from './writingReducer.js';

const threadReducer = (thread = { replies: [], reply: {}, expandReplies: true }, action) => {
  switch (action.type) {
    case DISCUSSION_THREAD_TOGGLE_REPLIES_EXPANSION:
      return {
        ...thread,
        expandReplies: !thread.expandReplies,
      };

    case DISCUSSION_POST_BATCH_SET_BODY_EXPANSION:
      return {
        ...thread,
        expandReplies: action.data.expansion || thread.expandReplies,
      };

    case DISCUSSION_THREAD_LOAD_REPLIES_START:
      return {
        ...thread,
        loadingReplies: true,
      };
    case DISCUSSION_THREAD_LOAD_REPLIES_SUCCESS:
      return {
        ...thread,
        loadingReplies: false,
        replies: uniq(concat(thread.replies, action.data.list)),
      };
    case DISCUSSION_THREAD_LOAD_REPLIES_ERROR:
      return {
        ...thread,
        loadingReplies: false,
        error: action.data.error,
      };

    case DISCUSSION_WRITING_START:
    case DISCUSSION_WRITING_SAVE_START:
    case DISCUSSION_WRITING_SAVE_ERROR:
    case DISCUSSION_WRITING_DISCARD:
      return {
        ...thread,
        reply: writingReducer(thread.reply, action),
      };
    case DISCUSSION_WRITING_SAVE_SUCCESS:
      return {
        ...thread,
        reply: writingReducer(thread.reply, action),
        replies: concat([action.data.newItemId], thread.replies),
      };

    default:
      return thread;
  }
};

const threadsReducer = (threads = {}, action) => {
  switch (action.type) {
    default:
      if (action.threadId) {
        return {
          ...threads,
          [action.threadId]: threadReducer(threads[action.threadId], action),
        };
      } else {
        return threads;
      }
  }
};

export default threadsReducer;
