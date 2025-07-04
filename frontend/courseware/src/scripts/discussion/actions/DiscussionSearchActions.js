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
import { batchActions } from 'redux-batched-actions';

import {
  DISCUSSION_BOARD_SEARCH_CLEAR,
  DISCUSSION_BOARD_SEARCH_START,
  DISCUSSION_BOARD_SEARCH_SUCCESS,
  DISCUSSION_BOARD_SEARCH_ERROR,
} from '../actionTypes.js';

import DiscussionBoardAPI from '../../services/DiscussionBoardAPI.js';

import DiscussionViewActions from './DiscussionViewActions.js';
import DiscussionDataActions from './DiscussionDataActions.js';

export default angular
  .module('lo.discussionBoard.DiscussionSearchActions', [
    DiscussionViewActions.name,
    DiscussionDataActions.name,
    DiscussionBoardAPI.name,
  ])
  .service('DiscussionSearchActions', [
    'DiscussionDataActions',
    'DiscussionBoardAPI',
    function DiscussionSearchActions(DiscussionDataActions, DiscussionBoardAPI) {
      const service = {};

      service.makeSearchStartActionCreator = discussionId => searchString => ({
        type: DISCUSSION_BOARD_SEARCH_START,
        discussionId,
        data: {
          searchString,
        },
      });

      service.makeSearchSuccessActionCreator = discussionId => searchResults => ({
        type: DISCUSSION_BOARD_SEARCH_SUCCESS,
        discussionId,
        data: {
          total: searchResults.totalCount,
          ids: map(searchResults, 'id'),
        },
      });

      service.makeSearchErrorActionCreator = discussionId => error => ({
        type: DISCUSSION_BOARD_SEARCH_ERROR,
        discussionId,
        data: {
          error,
        },
      });

      service.makeClearSearchActionCreator = discussionId => () => ({
        type: DISCUSSION_BOARD_SEARCH_CLEAR,
        discussionId,
      });

      service.makeSearchActionCreator = discussionId => {
        const searchStart = service.makeSearchStartActionCreator(discussionId);
        const searchSuccess = service.makeSearchSuccessActionCreator(discussionId);
        const searchError = service.makeSearchErrorActionCreator(discussionId);
        const clearSearch = service.makeClearSearchActionCreator(discussionId);

        return searchString => dispatch => {
          if (!searchString) {
            dispatch(clearSearch());
            return;
          }

          dispatch(searchStart(searchString));

          DiscussionBoardAPI.searchPosts(discussionId, searchString).then(
            searchResults => {
              dispatch(
                batchActions([
                  DiscussionDataActions.createPostsUpdateAction(searchResults),
                  searchSuccess(searchResults),
                ])
              );
            },
            error => dispatch(searchError(error))
          );
        };
      };

      return service;
    },
  ]);
