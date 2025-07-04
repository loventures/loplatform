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

import DiscussionScrollService from '../services/DiscussionScrollService.js';
import DiscussionJumperLoaders from '../services/DiscussionJumperLoaders.js';

import DiscussionDataActions from './DiscussionDataActions.js';
import DiscussionPostActions from './DiscussionPostActions.js';

export default angular
  .module('lo.discussionBoard.DiscussionJumperActions', [
    DiscussionDataActions.name,
    DiscussionJumperLoaders.name,
    DiscussionScrollService.name,
    DiscussionPostActions.name,
  ])
  .service('DiscussionJumperActions', [
    'DiscussionDataActions',
    'DiscussionJumperLoaders',
    'DiscussionPostActions',
    function DiscussionJumperActions(
      DiscussionDataActions,
      DiscussionJumperLoaders,
      DiscussionPostActions
    ) {
      const service = {};

      service.makeSetJumperActionCreator = (discussionId, jumperType) => () => ({
        type: DISCUSSION_BOARD_JUMPER_SET_JUMPER,
        discussionId,
        data: {
          jumperType,
        },
      });

      service.makeSetUserActionCreator = (discussionId, jumperType) => user => ({
        type: DISCUSSION_BOARD_JUMPER_SET_USER,
        discussionId,
        jumperType,
        data: {
          user,
        },
      });

      service.makeSetPostActionCreator = (discussionId, jumperType) => postId => ({
        type: DISCUSSION_BOARD_JUMPER_SET_POST,
        discussionId,
        jumperType,
        data: {
          postId,
        },
      });

      service.makeViewPostActionCreator = (discussionId, jumperType, viewingAction) => {
        const setPost = service.makeSetPostActionCreator(discussionId, jumperType);

        return post => dispatch => {
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

      service.makeViewJumperActionCreator = (discussionId, jumperType, viewingAction) => {
        const setJumper = service.makeSetJumperActionCreator(discussionId, jumperType);
        const setPost = service.makeSetPostActionCreator(discussionId, jumperType);

        return post => dispatch => {
          dispatch(batchActions([setJumper(), setPost(post.id)]));
          viewingAction(post, { viewType: jumperType, flashType: jumperType });
        };
      };

      service.makeLoadSummaryStartActionCreator = (discussionId, viewToDataTypes) => () => ({
        type: DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_START,
        viewToDataTypes,
        discussionId,
      });

      service.makeLoadSummarySuccessActionCreator = (discussionId, viewToDataTypes) => data => ({
        type: DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_SUCCESS,
        discussionId,
        viewToDataTypes,
        data,
      });

      service.makeLoadSummaryErrorActionCreator = (discussionId, viewToDataTypes) => error => ({
        type: DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_ERROR,
        discussionId,
        viewToDataTypes,
        data: { error },
      });

      service.makeLoadStartActionCreator = (discussionId, jumperType) => () => ({
        type: DISCUSSION_BOARD_JUMPER_LOAD_START,
        discussionId,
        jumperType,
      });

      service.makeLoadSuccessActionCreator = (discussionId, jumperType) => data => ({
        type: DISCUSSION_BOARD_JUMPER_LOAD_SUCCESS,
        discussionId,
        jumperType,
        data: {
          list: filter(map(data, 'id')), //filter removes any undefined elements
          totalCount: data.filterCount,
        },
      });

      service.makeLoadErrorActionCreator = (discussionId, jumperType) => error => ({
        type: DISCUSSION_BOARD_JUMPER_LOAD_ERROR,
        discussionId,
        jumperType,
        data: { error },
      });

      const postTypeByJumper = {
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
      service.makeSummaryLoadActionCreator = (discussionId, jumperTypes, lastVisitedTime) => {
        const postViewToDataTypes = map(jumperTypes, jumperType => ({
          jumperType,
          postType: postTypeByJumper[jumperType],
        }));

        const loader = DiscussionJumperLoaders.getSummaryLoader(postViewToDataTypes);

        const summaryLoadStart = service.makeLoadSummaryStartActionCreator(
          discussionId,
          postViewToDataTypes
        );
        const summaryLoadSuccess = service.makeLoadSummarySuccessActionCreator(
          discussionId,
          postViewToDataTypes
        );
        const summaryLoadError = service.makeLoadSummaryErrorActionCreator(
          discussionId,
          postViewToDataTypes
        );

        return userHandle => dispatch => {
          dispatch(summaryLoadStart());
          loader(discussionId, lastVisitedTime, userHandle).then(
            summary => {
              const allPosts = flatMap(
                postViewToDataTypes,
                viewToDataType => summary[viewToDataType.postType].partialResults
              );
              dispatch(
                batchActions([
                  DiscussionDataActions.createPostsUpdateAction(allPosts),
                  summaryLoadSuccess(summary, postViewToDataTypes),
                ])
              );
            },
            error => dispatch(summaryLoadError(error))
          );
        };
      };

      service.makeLoadActionCreator = (discussionId, jumperType) => {
        const loader = DiscussionJumperLoaders.getLoader(jumperType);

        const loadStart = service.makeLoadStartActionCreator(discussionId, jumperType);
        const loadSuccess = service.makeLoadSuccessActionCreator(discussionId, jumperType);
        const loadError = service.makeLoadErrorActionCreator(discussionId, jumperType);

        return (limit = 5, offset = 0, conf) =>
          dispatch => {
            dispatch(loadStart());
            loader(discussionId, limit, offset, conf).then(
              posts => {
                dispatch(
                  batchActions([
                    DiscussionDataActions.createPostsUpdateAction(posts),
                    loadSuccess(posts),
                  ])
                );
              },
              error => dispatch(loadError(error))
            );
          };
      };

      return service;
    },
  ]);
