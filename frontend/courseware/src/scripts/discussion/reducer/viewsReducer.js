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
  DISCUSSION_BOARD_VIEW_RETURN,
  DISCUSSION_BOARD_VIEW_CHANGE_THREAD_START,
  DISCUSSION_BOARD_VIEW_SET_CURRENT_THREAD,
  DISCUSSION_BOARD_VIEW_SET_CURRENT_POST,
  DISCUSSION_BOARD_SCROLL_TO_POST_START,
  DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS,
  DISCUSSION_BOARD_SCROLL_TO_POST_ERROR,
} from '../actionTypes.js';

const viewReducer = (discussionView = {}, action) => {
  switch (action.type) {
    case DISCUSSION_BOARD_VIEW_RETURN:
      return {
        ...discussionView,
        displayView: false,
        changingThread: false,
        scrolling: false,
        inViewThreadId: null,
      };

    case DISCUSSION_BOARD_VIEW_CHANGE_THREAD_START:
      return {
        ...discussionView,
        viewInfo: action.data.viewInfo,
        changingThread: true,
        inViewThreadId: null,
      };

    case DISCUSSION_BOARD_VIEW_SET_CURRENT_THREAD:
      return {
        ...discussionView,
        inViewThreadId: action.data.threadId,
        displayView: true,
        changingThread: false,
      };

    case DISCUSSION_BOARD_VIEW_SET_CURRENT_POST:
      return {
        ...discussionView,
        inViewPostId: action.data.postId,
        viewInfo: action.data.viewInfo,
        displayView: true,
      };

    case DISCUSSION_BOARD_SCROLL_TO_POST_START:
      return {
        ...discussionView,
        scrolling: true,
      };
    case DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS:
      return {
        ...discussionView,
        scrolling: false,
      };
    case DISCUSSION_BOARD_SCROLL_TO_POST_ERROR:
      return {
        ...discussionView,
        scrolling: false,
        error: action.data.error,
      };

    default:
  }
};

const discussionsViewReducer = (views = {}, action) => {
  if (!action.discussionId) {
    return views;
  }

  switch (action.type) {
    case DISCUSSION_BOARD_VIEW_RETURN:
    case DISCUSSION_BOARD_VIEW_CHANGE_THREAD_START:
    case DISCUSSION_BOARD_VIEW_SET_CURRENT_THREAD:
    case DISCUSSION_BOARD_VIEW_SET_CURRENT_POST:
    case DISCUSSION_BOARD_SCROLL_TO_POST_START:
    case DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS:
    case DISCUSSION_BOARD_SCROLL_TO_POST_ERROR:
      return {
        ...views,
        [action.discussionId]: viewReducer(views[action.discussionId], action),
      };
    default:
      return views;
  }
};

export default discussionsViewReducer;
