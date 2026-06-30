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

import { DISCUSSION_BOARD_SORT } from '../actionTypes.js';

import * as DiscussionLoadingActions from './DiscussionLoadingActions.ts';

export const makeSortStartActionCreator = (discussionId: any, sortConfig: any) => () => ({
  type: DISCUSSION_BOARD_SORT,
  discussionId,
  data: sortConfig,
});

export const makeSortActionCreator = (discussionId: any, sortConfig: any) => {
  const start = makeSortStartActionCreator(discussionId, sortConfig);
  const resetPage = DiscussionLoadingActions.makeResetPageActionCreator(discussionId);
  const loadThreads = DiscussionLoadingActions.makeLoadThreadsActionCreator(discussionId);

  return (args: any) => (dispatch: any) => {
    dispatch(batchActions([start(), resetPage()]));
    dispatch(loadThreads({ order: sortConfig, ...args }));
  };
};
