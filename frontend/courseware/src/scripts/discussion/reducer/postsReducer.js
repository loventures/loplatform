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

import {
  DISCUSSION_POST_SET_INITIAL_STATE,
  DISCUSSION_WRITING_START,
  DISCUSSION_WRITING_SAVE_START,
  DISCUSSION_WRITING_SAVE_ERROR,
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_WRITING_DISCARD,
  DISCUSSION_POST_TOGGLE_BODY_EXPANSION,
  DISCUSSION_POST_BATCH_SET_BODY_EXPANSION,
  DISCUSSION_POST_VIEWED_MANUAL_TOGGLED,
  DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS,
  DISCUSSION_POST_SET_NOT_NEW,
  DISCUSSION_POST_REPORT_INAPPROPRIATE_START,
  DISCUSSION_POST_REPORT_INAPPROPRIATE_SUCCESS,
  DISCUSSION_POST_REPORT_INAPPROPRIATE_FAILURE,
} from '../actionTypes.js';

import writingReducer from './writingReducer.js';

const postReducer = (post = { edit: {} }, action) => {
  switch (action.type) {
    case DISCUSSION_WRITING_START:
    case DISCUSSION_WRITING_SAVE_START:
    case DISCUSSION_WRITING_SAVE_ERROR:
    case DISCUSSION_WRITING_SAVE_SUCCESS:
    case DISCUSSION_WRITING_DISCARD:
      return {
        ...post,
        edit: writingReducer(post.edit, action),
      };

    case DISCUSSION_POST_TOGGLE_BODY_EXPANSION:
      return {
        ...post,
        expandBody: !post.expandBody,
      };
    case DISCUSSION_POST_BATCH_SET_BODY_EXPANSION:
      return {
        ...post,
        expandBody: action.data.expansion,
      };

    case DISCUSSION_POST_REPORT_INAPPROPRIATE_START:
      return {
        ...post,
        reporting: true,
      };
    case DISCUSSION_POST_REPORT_INAPPROPRIATE_SUCCESS:
      return {
        ...post,
        reporting: false,
      };
    case DISCUSSION_POST_REPORT_INAPPROPRIATE_FAILURE:
      return {
        ...post,
        reporting: false,
        reportingError: action.data.error,
      };

    case DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS:
      return {
        ...post,
        expandBody: true,
      };

    case DISCUSSION_POST_VIEWED_MANUAL_TOGGLED:
      return {
        ...post,
        viewedManuallyToggled: true,
      };

    default:
      return post;
  }
};

const postsReducer = (posts = {}, action) => {
  switch (action.type) {
    case DISCUSSION_POST_SET_INITIAL_STATE:
      forEach(action.data.posts, post => {
        posts = {
          ...posts,
          [post.id]: {
            ...posts[post.id],
            ...post,
          },
        };
      });
      return posts;

    case DISCUSSION_POST_SET_NOT_NEW:
      forEach(action.data.postIds, postId => {
        if (posts[postId].isNew) {
          posts = {
            ...posts,
            [postId]: {
              ...posts[postId],
              isNew: false,
            },
          };
        }
      });
      return posts;

    case DISCUSSION_POST_BATCH_SET_BODY_EXPANSION:
      forEach(action.postIds, postId => {
        posts = {
          ...posts,
          [postId]: postReducer(posts[action.postId], action),
        };
      });
      return posts;

    case DISCUSSION_POST_VIEWED_MANUAL_TOGGLED:
      forEach(action.postIds, postId => {
        posts = {
          ...posts,
          [postId]: postReducer(posts[action.postId], action),
        };
      });
      return posts;

    default:
      if (action.postId) {
        return {
          ...posts,
          [action.postId]: postReducer(posts[action.postId], action),
        };
      } else {
        return posts;
      }
  }
};

export default postsReducer;
