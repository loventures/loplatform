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

import listSearchTriggered from '../../list/directives/listSearchTriggered.js';

import discussionBoardThreadsView from './discussionBoardThreadsView.html';

import discussionThread from './discussionThread.js';

import { createThreadsSelector } from '../selectors.js';

import actions from '../actions/index.js';

import discussionOrders from '../services/discussionOrders.js';

export default angular
  .module('lo.discussion.discussionBoardThreadsView', [
    listSearchTriggered.name,
    discussionThread.name,
    actions.name,
  ])
  .component('discussionBoardThreadsView', {
    template: discussionBoardThreadsView,
    bindings: {
      discussionId: '<',
      settings: '<',
    },
    controller: [
      '$ngRedux',
      'DiscussionLoadingActions',
      'DiscussionSortActions',
      '$location',
      '$anchorScroll',
      function (
        $ngRedux,
        DiscussionLoadingActions,
        DiscussionSortActions,
        $location,
        $anchorScroll
      ) {
        this.$onInit = () => {
          const actionCreators = {
            loadThreads: DiscussionLoadingActions.makeLoadThreadsActionCreator(this.discussionId),
            initalSort: DiscussionSortActions.makeSortActionCreator(
              this.discussionId,
              discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC
            ),
          };

          $ngRedux.connectToCtrl(createThreadsSelector(this.discussionId), actionCreators)(this);
        };

        this.initial = () => {
          this.initalSort({ lastVisitedTime: this.lastVisitedTime });
        };

        this.scrollTop = function () {
          $location.hash('discussion-list-top');
          $anchorScroll();
        };

        this.loadMoreThreads = () => {
          this.loadThreads({
            offset: this.threadsLoaded,
            limit: 8,
            order: this.order,
            lastVisitedTime: this.lastVisitedTime,
          });
        };
      },
    ],
  });
