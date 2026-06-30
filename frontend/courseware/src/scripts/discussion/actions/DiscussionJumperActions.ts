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

import { map, flatMap, filter } from 'lodash';
import { batchActions } from 'redux-batched-actions';

import {
  DISCUSSION_BOARD_JUMPER_SET_JUMPER,
  DISCUSSION_BOARD_JUMPER_SET_POST,
  DISCUSSION_BOARD_JUMPER_SET_USER,
  DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_START,
  DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_SUCCESS,
  DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_ERROR,
  DISCUSSION_BOARD_JUMPER_LOAD_START,
  DISCUSSION_BOARD_JUMPER_LOAD_SUCCESS,
  DISCUSSION_BOARD_JUMPER_LOAD_ERROR,
} from '../actionTypes.js';

import * as DiscussionDataActions from './DiscussionDataActions.ts';
import * as DiscussionPostActions from './DiscussionPostActions.ts';
import { discussionJumperLoaders } from '../services/DiscussionJumperLoaders.ts';

export const makeSetJumperActionCreator = (discussionId: any, jumperType: any) => () => ({
  type: DISCUSSION_BOARD_JUMPER_SET_JUMPER,
  discussionId,
  data: {
    jumperType,
  },
});

export const makeSetUserActionCreator =
  (discussionId: any, jumperType: any) => (user: any) => ({
    type: DISCUSSION_BOARD_JUMPER_SET_USER,
    discussionId,
    jumperType,
    data: {
      user,
    },
  });

export const makeSetPostActionCreator =
  (discussionId: any, jumperType: any) => (postId: any) => ({
    type: DISCUSSION_BOARD_JUMPER_SET_POST,
    discussionId,
    jumperType,
    data: {
      postId,
    },
  });

export const makeViewPostActionCreator = (discussionId: any, jumperType: any, viewingAction: any) => {
  const setPost = makeSetPostActionCreator(discussionId, jumperType);

  return (post: any) => (dispatch: any) => {
    dispatch(setPost(post.id));
    if (!post.track.viewed)
      dispatch(
        DiscussionPostActions.makeSetViewedActionCreator(
          discussionId,
          post.threadId,
          post.id
        )(true)
      );
    viewingAction(post, { viewType: jumperType, flashType: jumperType });
  };
};

export const makeViewJumperActionCreator =
  (discussionId: any, jumperType: any, viewingAction: any) => {
    const setJumper = makeSetJumperActionCreator(discussionId, jumperType);
    const setPost = makeSetPostActionCreator(discussionId, jumperType);

    return (post: any) => (dispatch: any) => {
      dispatch(batchActions([setJumper(), setPost(post.id)]));
      viewingAction(post, { viewType: jumperType, flashType: jumperType });
    };
  };

export const makeLoadSummaryStartActionCreator =
  (discussionId: any, viewToDataTypes: any) => () => ({
    type: DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_START,
    viewToDataTypes,
    discussionId,
  });

export const makeLoadSummarySuccessActionCreator =
  (discussionId: any, viewToDataTypes: any) => (data: any) => ({
    type: DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_SUCCESS,
    discussionId,
    viewToDataTypes,
    data,
  });

export const makeLoadSummaryErrorActionCreator =
  (discussionId: any, viewToDataTypes: any) => (error: any) => ({
    type: DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_ERROR,
    discussionId,
    viewToDataTypes,
    data: { error },
  });

export const makeLoadStartActionCreator = (discussionId: any, jumperType: any) => () => ({
  type: DISCUSSION_BOARD_JUMPER_LOAD_START,
  discussionId,
  jumperType,
});

export const makeLoadSuccessActionCreator =
  (discussionId: any, jumperType: any) => (data: any) => ({
    type: DISCUSSION_BOARD_JUMPER_LOAD_SUCCESS,
    discussionId,
    jumperType,
    data: {
      list: filter(map(data, 'id')), //filter removes any undefined elements
      totalCount: data.filterCount,
    },
  });

export const makeLoadErrorActionCreator =
  (discussionId: any, jumperType: any) => (error: any) => ({
    type: DISCUSSION_BOARD_JUMPER_LOAD_ERROR,
    discussionId,
    jumperType,
    data: { error },
  });

const postTypeByJumper: any = {
  'user-posts': 'userPosts',
  new: 'newPosts',
  unread: 'unreadPosts',
  bookmarked: 'bookmarkedPosts',
  unresponded: 'unrespondedThreads',
};

/***
 * Batch together multiple initial post calls into a single summary call
 *
 * @param discussionId - the discussion we're querying posts from
 * @param jumperTypes - the view types - any of ['user-posts', 'new', 'unread', 'bookmarked', 'unresponded']
 * @param lastVisitedTime - Optional (needed for 'new') - the last time this discussion was visited
 * @returns an action creator for this summary call
 */
export const makeSummaryLoadActionCreator =
  (discussionId: any, jumperTypes: any, lastVisitedTime: any) => {
    const postViewToDataTypes = map(jumperTypes, jumperType => ({
      jumperType,
      postType: postTypeByJumper[jumperType],
    }));

    const loader = discussionJumperLoaders.getSummaryLoader(postViewToDataTypes);

    const summaryLoadStart = makeLoadSummaryStartActionCreator(discussionId, postViewToDataTypes);
    const summaryLoadSuccess = makeLoadSummarySuccessActionCreator(
      discussionId,
      postViewToDataTypes
    );
    const summaryLoadError = makeLoadSummaryErrorActionCreator(discussionId, postViewToDataTypes);

    return (userHandle: any) => (dispatch: any) => {
      dispatch(summaryLoadStart());
      loader(discussionId, lastVisitedTime, userHandle).then(
        (summary: any) => {
          const allPosts = flatMap(
            postViewToDataTypes,
            viewToDataType => summary[viewToDataType.postType].partialResults
          );
          dispatch(
            batchActions([
              DiscussionDataActions.createPostsUpdateAction(allPosts),
              summaryLoadSuccess(summary),
            ])
          );
        },
        (error: any) => dispatch(summaryLoadError(error))
      );
    };
  };

export const makeLoadActionCreator = (discussionId: any, jumperType: any) => {
  const loader = discussionJumperLoaders.getLoader(jumperType);

  const loadStart = makeLoadStartActionCreator(discussionId, jumperType);
  const loadSuccess = makeLoadSuccessActionCreator(discussionId, jumperType);
  const loadError = makeLoadErrorActionCreator(discussionId, jumperType);

  return (limit = 5, offset = 0, conf?: any) =>
    (dispatch: any) => {
      dispatch(loadStart());
      loader(discussionId, limit, offset, conf).then(
        (posts: any) => {
          dispatch(
            batchActions([
              DiscussionDataActions.createPostsUpdateAction(posts),
              loadSuccess(posts),
            ])
          );
        },
        (error: any) => dispatch(loadError(error))
      );
    };
};
