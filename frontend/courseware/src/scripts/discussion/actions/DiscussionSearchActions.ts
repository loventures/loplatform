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
import { batchActions } from 'redux-batched-actions';

import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';

import {
  DISCUSSION_BOARD_SEARCH_CLEAR,
  DISCUSSION_BOARD_SEARCH_START,
  DISCUSSION_BOARD_SEARCH_SUCCESS,
  DISCUSSION_BOARD_SEARCH_ERROR,
} from '../actionTypes.js';

import * as DiscussionDataActions from './DiscussionDataActions.ts';

export const makeSearchStartActionCreator = (discussionId: any) => (searchString: any) => ({
  type: DISCUSSION_BOARD_SEARCH_START,
  discussionId,
  data: {
    searchString,
  },
});

export const makeSearchSuccessActionCreator = (discussionId: any) => (searchResults: any) => ({
  type: DISCUSSION_BOARD_SEARCH_SUCCESS,
  discussionId,
  data: {
    total: searchResults.totalCount,
    ids: map(searchResults, 'id'),
  },
});

export const makeSearchErrorActionCreator = (discussionId: any) => (error: any) => ({
  type: DISCUSSION_BOARD_SEARCH_ERROR,
  discussionId,
  data: {
    error,
  },
});

export const makeClearSearchActionCreator = (discussionId: any) => () => ({
  type: DISCUSSION_BOARD_SEARCH_CLEAR,
  discussionId,
});

export const makeSearchActionCreator = (discussionId: any) => {
  const searchStart = makeSearchStartActionCreator(discussionId);
  const searchSuccess = makeSearchSuccessActionCreator(discussionId);
  const searchError = makeSearchErrorActionCreator(discussionId);
  const clearSearch = makeClearSearchActionCreator(discussionId);

  return (searchString: any) => (dispatch: any) => {
    if (!searchString) {
      dispatch(clearSearch());
      return;
    }

    dispatch(searchStart(searchString));

    discussionBoardAPI
      .searchPosts(discussionId, searchString)
      .then(
        (searchResults: any) => {
          dispatch(
            batchActions([
              DiscussionDataActions.createPostsUpdateAction(searchResults),
              searchSuccess(searchResults),
            ])
          );
        },
        (error: any) => dispatch(searchError(error))
      );
  };
};
