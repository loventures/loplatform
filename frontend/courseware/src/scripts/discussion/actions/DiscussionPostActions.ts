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

import { map } from 'lodash';

import { displayToastThunkActionCreator } from '../../directives/toast/toastActionCreators.ts';
import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';

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

import * as DiscussionDataActions from './DiscussionDataActions.ts';

export const createBatchUpdateTrackAction =
  (discussionId: any, postIds: any, config: any, secondaryAction?: any) => (dispatch: any) => {
    discussionBoardAPI
      .updateTracking(discussionId, postIds, config)
      .then((tracks: any) => {
        const posts = map(tracks, track => ({ id: track.postId, track }));
        dispatch(DiscussionDataActions.createPostsUpdateAction(posts, discussionId));
        if (secondaryAction) {
          dispatch(secondaryAction);
        }
      });
  };

export const createUpdateTrackAction =
  (discussionId: any, postId: any, config: any, secondaryAction?: any) => (dispatch: any) => {
    discussionBoardAPI
      .updateTracking(discussionId, [postId], config)
      .then((tracks: any) => {
        const posts = map(tracks, track => ({ id: track.postId, track }));
        dispatch(DiscussionDataActions.createPostsUpdateAction(posts, discussionId));
        if (secondaryAction) {
          dispatch(secondaryAction);
        }
      });
  };

export const makeSetViewedActionCreator =
  (discussionId: any, threadId: any, postId: any) => (viewed: any) => (dispatch: any) => {
    dispatch(
      createUpdateTrackAction(
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

export const makeBatchSetViewedActionCreator =
  (discussionId: any, threadId: any) => (viewed: any, posts: any) => (dispatch: any) => {
    const postIds = map(posts, 'id');
    dispatch(
      createBatchUpdateTrackAction(
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

export const createAutoSetViewedAction = (discussionId: any, postIds: any, threadIdMap: any) => {
  return createBatchUpdateTrackAction(
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

export const createAutoSetNotNewAction = (discussionId: any, postIds: any, threadIdMap: any) => ({
  type: DISCUSSION_POST_SET_NOT_NEW,
  discussionId,
  data: {
    countByThread: threadIdMap,
    postIds,
  },
});

export const makeSetBookmarkedActionCreator =
  (discussionId: any, postId: any) => (bookmarked: any) =>
    createUpdateTrackAction(discussionId, postId, { bookmarked });

export const makeSetPinnedActionCreator =
  (discussionId: any, threadId: any) => (val: any) => (dispatch: any) => {
    discussionBoardAPI
      .setPinned(discussionId, threadId, val)
      .then((post: any) => dispatch(DiscussionDataActions.createPostUpdateAction(post)));
  };

export const makeSetInappropriateActionCreator =
  (discussionId: any, postId: any) => (val: any) => (dispatch: any) => {
    discussionBoardAPI
      .setInappropriate(discussionId, postId, val)
      .then((post: any) => dispatch(DiscussionDataActions.createPostUpdateAction(post)));
  };

export const makeSetRemovedActionCreator =
  (discussionId: any, postId: any) => (val: any) => (dispatch: any) => {
    discussionBoardAPI
      .setRemoved(discussionId, postId, val)
      .then((post: any) => dispatch(DiscussionDataActions.createPostUpdateAction(post)));
  };

export const makeToggleExpandPostActionCreator = (discussionId: any, postId: any) => () => ({
  type: DISCUSSION_POST_TOGGLE_BODY_EXPANSION,
  discussionId,
  postId,
});

export const makeToggleExpandRepliesActionCreator = (discussionId: any, threadId: any) => () => ({
  type: DISCUSSION_THREAD_TOGGLE_REPLIES_EXPANSION,
  discussionId,
  threadId,
});

export const makeSetAllExpansionActionCreator =
  (discussionId: any, threadId: any) => (expansion: any, posts: any) => ({
    type: DISCUSSION_POST_BATCH_SET_BODY_EXPANSION,
    discussionId,
    threadId,
    postIds: map(posts, 'id'),
    data: {
      expansion,
    },
  });

export const makeReportInappropriateStartActionCreator =
  (discussionId: any, postId: any) => () => ({
    type: DISCUSSION_POST_REPORT_INAPPROPRIATE_START,
    discussionId,
    postId,
  });

export const makeReportInappropriateSuccessActionCreator =
  (discussionId: any, postId: any) => () => ({
    type: DISCUSSION_POST_REPORT_INAPPROPRIATE_SUCCESS,
    discussionId,
    postId,
  });

export const makeReportInappropriateErrorActionCreator =
  (discussionId: any, postId: any) => (error: any) => ({
    type: DISCUSSION_POST_REPORT_INAPPROPRIATE_FAILURE,
    discussionId,
    postId,
    data: {
      error,
    },
  });

export const makeReportInappropriateActionCreator = (discussionId: any, postId: any) => {
  const startActionCreator = makeReportInappropriateStartActionCreator(discussionId, postId);
  const successActionCreator = makeReportInappropriateSuccessActionCreator(discussionId, postId);
  const errorActionCreator = makeReportInappropriateErrorActionCreator(discussionId, postId);

  return () => (dispatch: any) => {
    dispatch(startActionCreator());
    discussionBoardAPI
      .reportInappropriate(discussionId, postId)
      .then(
        () => {
          const successToastAction = displayToastThunkActionCreator(
            'DISCUSSION_POST_REPORT_INAPPROPRIATE_SUCCESS_MESSAGE',
            5000,
            'default'
          );
          dispatch(successActionCreator());
          dispatch(successToastAction);
        },
        (error: any) => {
          const failureToastAction = displayToastThunkActionCreator(
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
