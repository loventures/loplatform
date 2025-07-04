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

import { searchByProps, sortByProps } from '../../users/usersConfig.js';

import { map } from 'lodash';

import listDirective from '../../srs/directives/index.js';

import template from './discussionStudentPickerModal.html';

import DiscussionSummaryAPI from '../../services/DiscussionSummaryAPI.js';

import enrolledUserService from '../../services/enrolledUserService.js';

export default angular
  .module('lo.discussion.discussionStudentPickerModal', [
    listDirective.name,
    DiscussionSummaryAPI.name,
    enrolledUserService.name,
  ])
  .component('discussionStudentPickerModal', {
    template,

    bindings: {
      resolve: '<',
      close: '&',
      dismiss: '&',
    },

    controller: [
      'LocalResourceStore',
      'DiscussionSummaryAPI',
      function (LocalResourceStore, DiscussionSummaryAPI) {
        this.loader = discussionId =>
          DiscussionSummaryAPI.getSummary(discussionId).then(userCounts => {
            return map(userCounts, userCount => ({
              ...userCount.user,
              totalPosts: userCount.totalPosts,
            }));
          });

        this.$onInit = () => {
          this.store = new LocalResourceStore(() => this.loader(this.resolve.discussionId));

          this.store.searchByProps = searchByProps;

          this.store.sortByProps = sortByProps;

          this.store.gotoPage(1);
        };
      },
    ],
  });
