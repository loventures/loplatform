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

import { batchActions } from 'redux-batched-actions';

import { DISCUSSION_BOARD_SORT } from '../actionTypes.js';

import DiscussionLoadingActions from './DiscussionLoadingActions.js';

export default angular
  .module('lo.discussionBoard.DiscussionSortActions', [DiscussionLoadingActions.name])
  .service('DiscussionSortActions', [
    'DiscussionLoadingActions',
    function DiscussionSortActions(DiscussionLoadingActions) {
      const service = {};

      service.makeSortStartActionCreator = (discussionId, sortConfig) => () => ({
        type: DISCUSSION_BOARD_SORT,
        discussionId,
        data: sortConfig,
      });

      service.makeSortActionCreator = (discussionId, sortConfig) => {
        const start = service.makeSortStartActionCreator(discussionId, sortConfig);
        const resetPage = DiscussionLoadingActions.makeResetPageActionCreator(discussionId);
        const loadThreads = DiscussionLoadingActions.makeLoadThreadsActionCreator(discussionId);

        return args => dispatch => {
          dispatch(batchActions([start(), resetPage()]));
          dispatch(loadThreads({ order: sortConfig, ...args }));
        };
      };

      return service;
    },
  ]);
