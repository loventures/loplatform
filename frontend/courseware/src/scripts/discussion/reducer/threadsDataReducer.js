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

import { forEach } from 'lodash';

import { createNamedReducer } from '../../utilities/reduxify.js';

import apiDataReducer from '../../utilities/apiDataReducer.js';

import {
  DISCUSSION_POST_VIEWED_AUTO_TOGGLED,
  DISCUSSION_POST_VIEWED_MANUAL_TOGGLED,
  DISCUSSION_POST_SET_NOT_NEW,
  DISCUSSION_WRITING_SAVE_SUCCESS,
} from '../actionTypes.js';

import { threadsDataSlice } from '../sliceNames.js';

const namedThreadsReducer = createNamedReducer(threadsDataSlice, apiDataReducer);

const threadsDataReducer = function (threads = {}, action) {
  switch (action.type) {
    case DISCUSSION_WRITING_SAVE_SUCCESS:
      if (!action.threadId) {
        return threads;
      }

      return {
        ...threads,
        [action.threadId]: {
          ...threads[action.threadId],
          postCount: threads[action.threadId].postCount + 1,
        },
      };

    case DISCUSSION_POST_VIEWED_MANUAL_TOGGLED:
      return {
        ...threads,
        [action.threadId]: {
          ...threads[action.threadId],
          unreadPostCount:
            threads[action.threadId].unreadPostCount +
            action.postIds.length * (action.data.viewed ? -1 : 1),
        },
      };

    case DISCUSSION_POST_VIEWED_AUTO_TOGGLED:
      forEach(action.data, (count, threadId) => {
        threads = {
          ...threads,
          [threadId]: {
            ...threads[threadId],
            unreadPostCount: threads[threadId].unreadPostCount - count,
          },
        };
      });
      return threads;

    case DISCUSSION_POST_SET_NOT_NEW:
      forEach(action.data.countByThread, (count, threadId) => {
        threads = {
          ...threads,
          [threadId]: {
            ...threads[threadId],
            newPostCount: threads[threadId].newPostCount - count,
          },
        };
      });
      return threads;

    default:
      return namedThreadsReducer(threads, action);
  }
};

export default threadsDataReducer;
