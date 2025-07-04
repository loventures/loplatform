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

import discussionBoardSearch from './discussionBoardSearch.html';

import { createSearchSelector } from '../selectors.js';

import actions from '../actions/index.js';

export default angular
  .module('lo.discussion.discussionBoardSearch', [listSearchTriggered.name, actions.name])
  .component('discussionBoardSearch', {
    template: discussionBoardSearch,
    bindings: {
      discussionId: '<',
      setInView: '<',
    },
    controller: [
      '$scope',
      '$ngRedux',
      'DiscussionSearchActions',
      function ($scope, $ngRedux, DiscussionSearchActions) {
        this.$onInit = () => {
          const actions = {
            search: DiscussionSearchActions.makeSearchActionCreator(this.discussionId),
          };

          $ngRedux.connectToCtrl(createSearchSelector(this.discussionId), actions)(this);
        };

        this.viewPost = post => this.setInView(post, { viewType: 'search', flashType: 'search' });
      },
    ],
  });
