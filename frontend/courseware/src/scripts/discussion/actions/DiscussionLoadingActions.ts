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

import { map, pick } from 'lodash';
import { batchActions } from 'redux-batched-actions';

import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';

import {
  DISCUSSION_BOARD_LOAD_THREADS_RESET,
  DISCUSSION_BOARD_LOAD_THREADS_START,
  DISCUSSION_BOARD_LOAD_THREADS_SUCCESS,
  DISCUSSION_BOARD_LOAD_THREADS_ERROR,
  DISCUSSION_THREAD_LOAD_REPLIES_START,
  DISCUSSION_THREAD_LOAD_REPLIES_SUCCESS,
  DISCUSSION_THREAD_LOAD_REPLIES_ERROR,
} from '../actionTypes.js';

import * as DiscussionDataActions from './DiscussionDataActions.ts';
import * as DiscussionPostStateActions from './DiscussionPostStateActions.ts';

export const makeResetPageActionCreator = (discussionId: any) => () => ({
  type: DISCUSSION_BOARD_LOAD_THREADS_RESET,
  discussionId,
});

export const makeLoadThreadStartActionCreator = (discussionId: any) => (query: any) => ({
  type: DISCUSSION_BOARD_LOAD_THREADS_START,
  discussionId,
  data: query,
});

export const makeLoadThreadSuccessActionCreator = (discussionId: any) => (threads: any) => ({
  type: DISCUSSION_BOARD_LOAD_THREADS_SUCCESS,
  discussionId,
  data: {
    // TODO: TECH-685
    list: map(threads, 'id').slice(),
    count: threads.count,
    filterCount: threads.filterCount,
  },
});

export const makeLoadThreadErrorActionCreator = (discussionId: any) => (error: any) => ({
  type: DISCUSSION_BOARD_LOAD_THREADS_ERROR,
  discussionId,
  data: { error },
});

export const makeLoadRepliesStartActionCreator =
  (discussionId: any, threadId: any) => () => ({
    type: DISCUSSION_THREAD_LOAD_REPLIES_START,
    discussionId,
    threadId,
  });

export const makeLoadRepliesSuccessActionCreator =
  (discussionId: any, threadId: any) => (posts: any) => ({
    type: DISCUSSION_THREAD_LOAD_REPLIES_SUCCESS,
    discussionId,
    threadId,
    data: {
      // TODO: TECH-685
      list: map(posts, 'id').slice(),
    },
  });

export const makeLoadRepliesErrorActionCreator =
  (discussionId: any, threadId: any) => (error: any) => ({
    type: DISCUSSION_THREAD_LOAD_REPLIES_ERROR,
    discussionId,
    threadId,
    data: { error },
  });

const pinnedOrder = {
  property: 'pinned',
  order: 'asc',
};

export const makeLoadThreadsActionCreator = (discussionId: any) => {
  const loadStart = makeLoadThreadStartActionCreator(discussionId);
  const loadSuccess = makeLoadThreadSuccessActionCreator(discussionId);
  const loadError = makeLoadThreadErrorActionCreator(discussionId);

  const initPostState =
    DiscussionPostStateActions.makePostStateUpdateActionCreator(discussionId);

  const pickThread = (thread: any) =>
    pick(thread, [
      'id',
      'postCount',
      'unreadPostCount',
      'newPostCount',
      'availablePostCount',
    ]);

  return ({ offset = 0, limit = 5, order, lastVisitedTime }: any) =>
    (dispatch: any) => {
      const query = { offset, limit, orders: [pinnedOrder, order] };

      dispatch(loadStart(query));

      discussionBoardAPI
        .loadThreads(discussionId, query, lastVisitedTime)
        .then(
          (threads: any) => {
            const posts = map(threads, 'rootPost');
            dispatch(
              batchActions([
                DiscussionDataActions.createThreadsUpdateAction(map(threads, pickThread)),
                DiscussionDataActions.createPostsUpdateAction(posts),
                loadSuccess(threads),
              ])
            );
            dispatch(initPostState(posts));
          },
          (error: any) => dispatch(loadError(error))
        );
    };
};

export const makeLoadRepliesActionCreator = (discussionId: any, threadId: any, _isInstructor?: any) => {
  const loadStart = makeLoadRepliesStartActionCreator(discussionId, threadId);
  const loadSuccess = makeLoadRepliesSuccessActionCreator(discussionId, threadId);
  const loadError = makeLoadRepliesErrorActionCreator(discussionId, threadId);

  const initPostState =
    DiscussionPostStateActions.makePostStateUpdateActionCreator(discussionId);

  return (offset = 0, limit = 20) =>
    (dispatch: any) => {
      dispatch(loadStart());
      discussionBoardAPI
        .loadReplies(discussionId, threadId, offset, limit)
        .then(
          (posts: any) => {
            dispatch(
              batchActions([
                DiscussionDataActions.createPostsUpdateAction(posts),
                loadSuccess(posts),
              ])
            );
            dispatch(initPostState(posts));
          },
          (error: any) => dispatch(loadError(error))
        );
    };
};
