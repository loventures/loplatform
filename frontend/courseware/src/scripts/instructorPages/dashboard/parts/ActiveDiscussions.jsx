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

import assignmentLists from '../../../assignment/listDirectives/activeAssignmentsList';
import ActiveDiscussionsStoreLight from '../../../assignment/ActiveDiscussionsStoreLight';

import { ContentPlayerPageLink, DiscussionListPageLink } from '../../../utils/pageLinks';
import { gotoLink } from '../../../utilities/routingUtils';

const component = {
  template: `
    <active-discussions-list
      store="activeDiscussionsStore"
      view-one="viewDiscussion"
      view-all="viewDiscussionsPage">
    </active-discussions-list>
  `,
  controller: [
    '$scope',
    'ActiveDiscussionsStoreLight',
    function ($scope, ActiveDiscussionsStoreLight) {
      $scope.activeDiscussionsStore = new ActiveDiscussionsStoreLight();
      $scope.activeDiscussionsStore.gotoPage(1);

      $scope.viewDiscussion = function (discussion) {
        gotoLink(ContentPlayerPageLink.toLink({ content: discussion, nav: 'none' }));
      };

      $scope.viewDiscussionsPage = function () {
        gotoLink(DiscussionListPageLink.toLink());
      };
    },
  ],
};

export let ActiveDiscussions = 'ActiveDiscussions: ng module not found';

import { angular2react } from 'angular2react';

export default angular
  .module('ple.pages.instructor.activeDiscussionsReact', [
    assignmentLists.name,
    ActiveDiscussionsStoreLight.name,
  ])
  .component('activeDiscussionsReact', component)
  .run([
    '$injector',
    function ($injector) {
      ActiveDiscussions = angular2react('activeDiscussionsReact', component, $injector);
    },
  ]);
