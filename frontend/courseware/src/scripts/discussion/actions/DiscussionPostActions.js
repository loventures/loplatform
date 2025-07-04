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

import { map } from 'lodash';

import {
  DISCUSSION_POST_TOGGLE_BODY_EXPANSION,
  DISCUSSION_POST_BATCH_SET_BODY_EXPANSION,
  DISCUSSION_THREAD_TOGGLE_REPLIES_EXPANSION,
  DISCUSSION_POST_VIEWED_AUTO_TOGGLED,
  DISCUSSION_POST_VIEWED_MANUAL_TOGGLED,
  DISCUSSION_POST_SET_NOT_NEW,
  DISCUSSION_POST_REPORT_INAPPROPRIATE_START,
  DISCUSSION_POST_REPORT_INAPPROPRIATE_SUCCESS,
  DISCUSSION_POST_REPORT_INAPPROPRIATE_FAILURE,
} from '../actionTypes.js';

import DiscussionBoardAPI from '../../services/DiscussionBoardAPI.js';

import DiscussionDataActions from './DiscussionDataActions.js';
import DiscussionBoardActions from './DiscussionBoardActions.js';
import ToastActions from '../../directives/toast/ToastActions.js';

export default angular
  .module('lo.discussionBoard.DiscussionPostActions', [
    DiscussionBoardAPI.name,
    DiscussionDataActions.name,
    DiscussionBoardActions.name,
    ToastActions.name,
  ])
  .service('DiscussionPostActions', [
    'DiscussionBoardAPI',
    'DiscussionDataActions',
    'ToastActions',
    function DiscussionPostActions(DiscussionBoardAPI, DiscussionDataActions, ToastActions) {
      const service = {};

      service.createBatchUpdateTrackAction =
        (discussionId, postIds, config, secondaryAction) => dispatch => {
          DiscussionBoardAPI.updateTracking(discussionId, postIds, config).then(tracks => {
            const posts = map(tracks, track => ({ id: track.postId, track }));
            dispatch(DiscussionDataActions.createPostsUpdateAction(posts, discussionId));
            if (secondaryAction) {
              dispatch(secondaryAction);
            }
          });
        };

      service.createUpdateTrackAction =
        (discussionId, postId, config, secondaryAction) => dispatch => {
          DiscussionBoardAPI.updateTracking(discussionId, [postId], config).then(tracks => {
            const posts = map(tracks, track => ({ id: track.postId, track }));
            dispatch(DiscussionDataActions.createPostsUpdateAction(posts, discussionId));
            if (secondaryAction) {
              dispatch(secondaryAction);
            }
          });
        };

      service.makeSetViewedActionCreator =
        (discussionId, threadId, postId) => viewed => dispatch => {
          dispatch(
            service.createUpdateTrackAction(
              discussionId,
              postId,
              { viewed },
              {
                type: DISCUSSION_POST_VIEWED_MANUAL_TOGGLED,
                discussionId,
                threadId,
                postIds: [postId],
                data: { viewed },
              }
            )
          );
        };

      service.makeBatchSetViewedActionCreator =
        (discussionId, threadId) => (viewed, posts) => dispatch => {
          const postIds = map(posts, 'id');
          dispatch(
            service.createBatchUpdateTrackAction(
              discussionId,
              postIds,
              { viewed },
              {
                type: DISCUSSION_POST_VIEWED_MANUAL_TOGGLED,
                discussionId,
                threadId,
                postIds,
                data: { viewed },
              }
            )
          );
        };

      service.createAutoSetViewedAction = (discussionId, postIds, threadIdMap) => {
        return service.createBatchUpdateTrackAction(
          discussionId,
          postIds,
          { viewed: true },
          {
            type: DISCUSSION_POST_VIEWED_AUTO_TOGGLED,
            discussionId,
            postIds,
            data: threadIdMap,
          }
        );
      };

      service.createAutoSetNotNewAction = (discussionId, postIds, threadIdMap) => ({
        type: DISCUSSION_POST_SET_NOT_NEW,
        discussionId,
        data: {
          countByThread: threadIdMap,
          postIds,
        },
      });

      service.makeSetBookmarkedActionCreator = (discussionId, postId) => bookmarked =>
        service.createUpdateTrackAction(discussionId, postId, { bookmarked });

      service.makeSetPinnedActionCreator = (discussionId, threadId) => val => dispatch => {
        DiscussionBoardAPI.setPinned(discussionId, threadId, val).then(post =>
          dispatch(DiscussionDataActions.createPostUpdateAction(post))
        );
      };

      service.makeSetInappropriateActionCreator = (discussionId, postId) => val => dispatch => {
        DiscussionBoardAPI.setInappropriate(discussionId, postId, val).then(post =>
          dispatch(DiscussionDataActions.createPostUpdateAction(post))
        );
      };

      service.makeSetRemovedActionCreator = (discussionId, postId) => val => dispatch => {
        DiscussionBoardAPI.setRemoved(discussionId, postId, val).then(post =>
          dispatch(DiscussionDataActions.createPostUpdateAction(post))
        );
      };

      service.makeToggleExpandPostActionCreator = (discussionId, postId) => () => ({
        type: DISCUSSION_POST_TOGGLE_BODY_EXPANSION,
        discussionId,
        postId,
      });

      service.makeToggleExpandRepliesActionCreator = (discussionId, threadId) => () => ({
        type: DISCUSSION_THREAD_TOGGLE_REPLIES_EXPANSION,
        discussionId,
        threadId,
      });

      service.makeSetAllExpansionActionCreator =
        (discussionId, threadId) => (expansion, posts) => ({
          type: DISCUSSION_POST_BATCH_SET_BODY_EXPANSION,
          discussionId,
          threadId,
          postIds: map(posts, 'id'),
          data: {
            expansion,
          },
        });

      service.makeReportInappropriateStartActionCreator = (discussionId, postId) => () => ({
        type: DISCUSSION_POST_REPORT_INAPPROPRIATE_START,
        discussionId,
        postId,
      });

      service.makeReportInappropriateSuccessActionCreator = (discussionId, postId) => () => ({
        type: DISCUSSION_POST_REPORT_INAPPROPRIATE_SUCCESS,
        discussionId,
        postId,
      });

      service.makeReportInappropriateErrorActionCreator = (discussionId, postId) => error => ({
        type: DISCUSSION_POST_REPORT_INAPPROPRIATE_FAILURE,
        discussionId,
        postId,
        data: {
          error,
        },
      });

      service.makeReportInappropriateActionCreator = (discussionId, postId) => {
        const startActionCreator = service.makeReportInappropriateStartActionCreator(
          discussionId,
          postId
        );
        const successActionCreator = service.makeReportInappropriateSuccessActionCreator(
          discussionId,
          postId
        );
        const errorActionCreator = service.makeReportInappropriateErrorActionCreator(
          discussionId,
          postId
        );

        return () => dispatch => {
          dispatch(startActionCreator());
          DiscussionBoardAPI.reportInappropriate(discussionId, postId).then(
            () => {
              const successToastAction = ToastActions.displayToastThunkActionCreator(
                'DISCUSSION_POST_REPORT_INAPPROPRIATE_SUCCESS_MESSAGE',
                5000,
                'default'
              );
              dispatch(successActionCreator());
              dispatch(successToastAction);
            },
            error => {
              const failureToastAction = ToastActions.displayToastThunkActionCreator(
                'DISCUSSION_POST_REPORT_INAPPROPRIATE_FAILURE_MESSAGE',
                null,
                'danger'
              );
              dispatch(errorActionCreator(error || {}));
              dispatch(failureToastAction);
            }
          );
        };
      };

      return service;
    },
  ]);
