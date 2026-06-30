/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';
import { discussionScrollService } from '../services/pure/discussionScrollService.ts';

import {
  DISCUSSION_BOARD_VIEW_RETURN,
  DISCUSSION_BOARD_VIEW_CHANGE_THREAD_START,
  DISCUSSION_BOARD_VIEW_SET_CURRENT_THREAD,
  DISCUSSION_BOARD_VIEW_SET_CURRENT_POST,
  DISCUSSION_BOARD_SCROLL_TO_POST_START,
  DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS,
  DISCUSSION_BOARD_SCROLL_TO_POST_ERROR,
} from '../actionTypes.js';

import * as DiscussionDataActions from './DiscussionDataActions.ts';

export const makeRestoreDefaultActionCreator = (discussionId: any) => () => ({
  type: DISCUSSION_BOARD_VIEW_RETURN,
  discussionId,
});

export const makeChangeThreadStartActionCreator = (discussionId: any) => (viewInfo: any) => ({
  type: DISCUSSION_BOARD_VIEW_CHANGE_THREAD_START,
  discussionId,
  data: { viewInfo },
});

export const makeSetCurrentThreadActionCreator =
  (discussionId: any) => (threadId: any, viewInfo: any) => ({
    type: DISCUSSION_BOARD_VIEW_SET_CURRENT_THREAD,
    discussionId,
    data: {
      viewInfo,
      threadId,
    },
  });

export const makeSetCurrentPostActionCreator =
  (discussionId: any) => (postId: any, viewInfo: any) => ({
    type: DISCUSSION_BOARD_VIEW_SET_CURRENT_POST,
    discussionId,
    data: {
      viewInfo,
      postId,
    },
  });

export const makeScrollToPostActionCreator =
  (discussionId: any) => (postId: any, info: any) => (dispatch: any) => {
    dispatch({
      type: DISCUSSION_BOARD_SCROLL_TO_POST_START,
      discussionId,
    });

    discussionScrollService
      .scrollToAndFlash(postId, info)
      .then(
        () =>
          dispatch({
            type: DISCUSSION_BOARD_SCROLL_TO_POST_SUCCESS,
            discussionId,
            postId,
          }),
        (error: any) =>
          dispatch({
            type: DISCUSSION_BOARD_SCROLL_TO_POST_ERROR,
            discussionId,
            data: { error },
          })
      );
  };

export const makeViewPostActionCreator = (discussionId: any) => {
  const scrollTo = makeScrollToPostActionCreator(discussionId);
  const changeThreadStart = makeChangeThreadStartActionCreator(discussionId);
  const setThread = makeSetCurrentThreadActionCreator(discussionId);
  const setPost = makeSetCurrentPostActionCreator(discussionId);

  return (postToSet: any, inViewThreadId: any, info?: any) => {
    if (postToSet.threadId === inViewThreadId) {
      //same thread, avoid a full reload
      return (dispatch: any) => {
        dispatch(setPost(postToSet.id, info));
        dispatch(scrollTo(postToSet.id, info));
      };
    }

    return (dispatch: any) => {
      dispatch(changeThreadStart(info));

      discussionBoardAPI
        .getOneThread(discussionId, postToSet.threadId)
        .then((thread: any) => {
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

export const makeViewRepliedToPostActionCreator = (discussionId: any) => {
  const viewPostActionCreator = makeViewPostActionCreator(discussionId);

  return (postId: any, inViewThreadId: any) => {
    return (dispatch: any) => {
      discussionBoardAPI
        .getOnePost(discussionId, postId)
        .then((post: any) => {
          const postsUpdateAction = DiscussionDataActions.createPostsUpdateAction([post]);
          const viewPostAction = viewPostActionCreator(post, inViewThreadId);
          dispatch(postsUpdateAction);
          dispatch(viewPostAction);
        });
    };
  };
};

export const makeViewInappropriatePostActionCreator = (discussionId: any) => {
  const viewPostActionCreator = makeViewPostActionCreator(discussionId);

  return (postId: any, inViewThreadId: any) => {
    return (dispatch: any) => {
      discussionBoardAPI
        .getOnePost(discussionId, postId)
        .then((post: any) => {
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
