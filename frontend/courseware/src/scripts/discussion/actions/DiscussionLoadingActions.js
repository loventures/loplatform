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

import { map, pick } from 'lodash';

import { batchActions } from 'redux-batched-actions';

import {
  DISCUSSION_BOARD_LOAD_THREADS_RESET,
  DISCUSSION_BOARD_LOAD_THREADS_START,
  DISCUSSION_BOARD_LOAD_THREADS_SUCCESS,
  DISCUSSION_BOARD_LOAD_THREADS_ERROR,
  DISCUSSION_THREAD_LOAD_REPLIES_START,
  DISCUSSION_THREAD_LOAD_REPLIES_SUCCESS,
  DISCUSSION_THREAD_LOAD_REPLIES_ERROR,
} from '../actionTypes.js';

import DiscussionBoardAPI from '../../services/DiscussionBoardAPI.js';

import DiscussionDataActions from './DiscussionDataActions.js';
import DiscussionPostStateActions from './DiscussionPostStateActions.js';

export default angular
  .module('lo.discussionBoard.DiscussionLoadingActions', [
    DiscussionDataActions.name,
    DiscussionPostStateActions.name,
    DiscussionBoardAPI.name,
  ])
  .service('DiscussionLoadingActions', [
    'DiscussionDataActions',
    'DiscussionPostStateActions',
    'DiscussionBoardAPI',
    function DiscussionLoadingActions(
      DiscussionDataActions,
      DiscussionPostStateActions,
      DiscussionBoardAPI
    ) {
      const service = {};

      service.makeResetPageActionCreator = discussionId => () => ({
        type: DISCUSSION_BOARD_LOAD_THREADS_RESET,
        discussionId,
      });

      service.makeLoadThreadStartActionCreator = discussionId => query => ({
        type: DISCUSSION_BOARD_LOAD_THREADS_START,
        discussionId,
        data: query,
      });

      service.makeLoadThreadSuccessActionCreator = discussionId => threads => ({
        type: DISCUSSION_BOARD_LOAD_THREADS_SUCCESS,
        discussionId,
        data: {
          // TODO: TECH-685
          list: map(threads, 'id').slice(),
          count: threads.count,
          filterCount: threads.filterCount,
        },
      });

      service.makeLoadThreadErrorActionCreator = discussionId => error => ({
        type: DISCUSSION_BOARD_LOAD_THREADS_ERROR,
        discussionId,
        data: { error },
      });

      service.makeLoadRepliesStartActionCreator = (discussionId, threadId) => () => ({
        type: DISCUSSION_THREAD_LOAD_REPLIES_START,
        discussionId,
        threadId,
      });

      service.makeLoadRepliesSuccessActionCreator = (discussionId, threadId) => posts => ({
        type: DISCUSSION_THREAD_LOAD_REPLIES_SUCCESS,
        discussionId,
        threadId,
        data: {
          // TODO: TECH-685
          list: map(posts, 'id').slice(),
        },
      });

      service.makeLoadRepliesErrorActionCreator = (discussionId, threadId) => error => ({
        type: DISCUSSION_THREAD_LOAD_REPLIES_ERROR,
        discussionId,
        threadId,
        data: { error },
      });

      const pinnedOrder = {
        property: 'pinned',
        order: 'asc',
      };

      service.makeLoadThreadsActionCreator = discussionId => {
        const loadStart = service.makeLoadThreadStartActionCreator(discussionId);
        const loadSuccess = service.makeLoadThreadSuccessActionCreator(discussionId);
        const loadError = service.makeLoadThreadErrorActionCreator(discussionId);

        const initPostState =
          DiscussionPostStateActions.makePostStateUpdateActionCreator(discussionId);

        const pickThread = thread =>
          pick(thread, [
            'id',
            'postCount',
            'unreadPostCount',
            'newPostCount',
            'availablePostCount',
          ]);

        return ({ offset = 0, limit = 5, order, lastVisitedTime }) =>
          dispatch => {
            const query = { offset, limit, orders: [pinnedOrder, order] };

            dispatch(loadStart(query));

            DiscussionBoardAPI.loadThreads(discussionId, query, lastVisitedTime).then(
              threads => {
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
              error => dispatch(loadError(error))
            );
          };
      };

      service.makeLoadRepliesActionCreator = (discussionId, threadId) => {
        const loadStart = service.makeLoadRepliesStartActionCreator(discussionId, threadId);
        const loadSuccess = service.makeLoadRepliesSuccessActionCreator(discussionId, threadId);
        const loadError = service.makeLoadRepliesErrorActionCreator(discussionId, threadId);

        const initPostState =
          DiscussionPostStateActions.makePostStateUpdateActionCreator(discussionId);

        return (offset = 0, limit = 20) =>
          dispatch => {
            dispatch(loadStart());
            DiscussionBoardAPI.loadReplies(discussionId, threadId, offset, limit).then(
              posts => {
                dispatch(
                  batchActions([
                    DiscussionDataActions.createPostsUpdateAction(posts),
                    loadSuccess(posts),
                  ])
                );
                dispatch(initPostState(posts));
              },
              error => dispatch(loadError(error))
            );
          };
      };

      return service;
    },
  ]);
