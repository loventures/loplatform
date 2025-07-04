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

import { batchActions } from 'redux-batched-actions';

import {
  DISCUSSION_BOARD_VIEW_RETURN,
  DISCUSSION_BOARD_VIEW_CHANGE_THREAD_START,
  DISCUSSION_BOARD_VIEW_SET_CURRENT_THREAD,
  DISCUSSION_BOARD_VIEW_SET_CURRENT_POST,
  DISCUSSION_BOARD_SCROLL_TO_POST_START,
  DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS,
  DISCUSSION_BOARD_SCROLL_TO_POST_ERROR,
} from '../actionTypes.js';

import DiscussionBoardAPI from '../../services/DiscussionBoardAPI.js';

import DiscussionScrollService from '../services/DiscussionScrollService.js';

import DiscussionDataActions from './DiscussionDataActions.js';

export default angular
  .module('lo.discussionBoard.DiscussionViewActions', [
    DiscussionDataActions.name,
    DiscussionScrollService.name,
    DiscussionBoardAPI.name,
  ])
  .service('DiscussionViewActions', [
    'DiscussionScrollService',
    'DiscussionDataActions',
    'DiscussionBoardAPI',
    function DiscussionViewActions(
      DiscussionScrollService,
      DiscussionDataActions,
      DiscussionBoardAPI
    ) {
      const service = {};

      service.makeRestoreDefaultActionCreator = discussionId => () => ({
        type: DISCUSSION_BOARD_VIEW_RETURN,
        discussionId,
      });

      service.makeChangeThreadStartActionCreator = discussionId => viewInfo => ({
        type: DISCUSSION_BOARD_VIEW_CHANGE_THREAD_START,
        discussionId,
        data: { viewInfo },
      });

      service.makeSetCurrentThreadActionCreator = discussionId => (threadId, viewInfo) => ({
        type: DISCUSSION_BOARD_VIEW_SET_CURRENT_THREAD,
        discussionId,
        data: {
          viewInfo,
          threadId,
        },
      });

      service.makeSetCurrentPostActionCreator = discussionId => (postId, viewInfo) => ({
        type: DISCUSSION_BOARD_VIEW_SET_CURRENT_POST,
        discussionId,
        data: {
          viewInfo,
          postId,
        },
      });

      service.makeScrollToPostActionCreator = discussionId => (postId, info) => dispatch => {
        dispatch({
          type: DISCUSSION_BOARD_SCROLL_TO_POST_START,
          discussionId,
        });

        DiscussionScrollService.scrollToAndFlash(postId, info).then(
          () =>
            dispatch({
              type: DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS,
              discussionId,
              postId,
            }),
          error =>
            dispatch({
              type: DISCUSSION_BOARD_SCROLL_TO_POST_ERROR,
              discussionId,
              data: { error },
            })
        );
      };

      service.makeViewPostActionCreator = discussionId => {
        const scrollTo = service.makeScrollToPostActionCreator(discussionId);
        const changeThreadStart = service.makeChangeThreadStartActionCreator(discussionId);
        const setThread = service.makeSetCurrentThreadActionCreator(discussionId);
        const setPost = service.makeSetCurrentPostActionCreator(discussionId);

        return (postToSet, inViewThreadId, info) => {
          if (postToSet.threadId === inViewThreadId) {
            //same thread, avoid a full reload
            return dispatch => {
              dispatch(setPost(postToSet.id, info));
              dispatch(scrollTo(postToSet.id, info));
            };
          }

          return dispatch => {
            dispatch(changeThreadStart(info));

            DiscussionBoardAPI.getOneThread(discussionId, postToSet.threadId).then(thread => {
              dispatch(
                batchActions([
                  DiscussionDataActions.createThreadsUpdateAction([thread]),
                  DiscussionDataActions.createPostsUpdateAction([thread.rootPost]),
                  setThread(thread.id, info),
                  setPost(postToSet.id, info),
                ])
              );

              dispatch(scrollTo(postToSet.id, info));
            });
          };
        };
      };

      service.makeViewRepliedToPostActionCreator = discussionId => {
        const viewPostActionCreator = service.makeViewPostActionCreator(discussionId);

        return (postId, inViewThreadId) => {
          return dispatch => {
            DiscussionBoardAPI.getOnePost(discussionId, postId).then(post => {
              const postsUpdateAction = DiscussionDataActions.createPostsUpdateAction([post]);
              const viewPostAction = viewPostActionCreator(post, inViewThreadId);
              dispatch(postsUpdateAction);
              dispatch(viewPostAction);
            });
          };
        };
      };

      service.makeViewInappropriatePostActionCreator = discussionId => {
        const viewPostActionCreator = service.makeViewPostActionCreator(discussionId);

        return (postId, inViewThreadId) => {
          return dispatch => {
            DiscussionBoardAPI.getOnePost(discussionId, postId).then(post => {
              const postsUpdateAction = DiscussionDataActions.createPostsUpdateAction([post]);
              const viewPostAction = viewPostActionCreator(post, inViewThreadId, {
                flashType: 'reported-inappropriate-posts',
                viewType: 'reported-inappropriate-posts',
              });
              dispatch(postsUpdateAction);
              dispatch(viewPostAction);
            });
          };
        };
      };

      return service;
    },
  ]);
