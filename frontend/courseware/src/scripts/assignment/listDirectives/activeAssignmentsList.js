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

import activeAssignmentsListTmpl from './activeAssignmentsList.html';
import activeDiscussionsListTmpl from './activeDiscussionsList.html';

import tooltip from 'angular-ui-bootstrap/src/tooltip';

export default angular
  .module('lo.assignment.listDirectives.activeDiscussionsList', [tooltip])
  .directive('activeDiscussionsList', function () {
    return {
      restrict: 'EA',
      template: activeDiscussionsListTmpl,
      scope: {
        store: '=',
        viewOne: '=',
        viewAll: '=',
      },
      controller: [
        '$scope',
        'Roles',
        function ($scope, Roles) {
          $scope.headerText = 'ACTIVE_DISCUSSION_LIST_TITLE';
          $scope.emptyMsg = 'DASHBOARD_ACTIVE_DISCUSSIONS_NO_ACTIVITY';

          $scope.headerButtonConfig = {
            label: 'DISCUSSION_LIST_VIEW_ALL',
            onClick: $scope.viewAll,
          };

          $scope.showUnresponded = Roles.isInstructor() && !Roles.isAdvisor();
        },
      ],
    };
  })
  .directive('gradingAssignmentList', function () {
    return {
      restrict: 'EA',
      template: activeAssignmentsListTmpl,
      scope: {
        store: '=',
        viewOne: '=',
        viewAll: '=',
      },
      controller: [
        '$scope',
        function ($scope) {
          $scope.headerText = 'Assignments to Grade';
          $scope.emptyMsg = 'GRADING_ASSIGNMENT_EMPTY_QUEUE';
          $scope.headerButtonConfig = {
            label: 'GRADING_ASSIGNMENT_VIEW_ALL',
            onClick: $scope.viewAll,
          };
        },
      ],
    };
  });
