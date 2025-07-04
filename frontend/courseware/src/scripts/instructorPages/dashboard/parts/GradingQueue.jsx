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
import GradingQueueStoreLight from '../../../assignment/GradingQueueStoreLight';

import {
  InstructorGraderPageLink,
  InstructorAssignmentListPageLink,
} from '../../../utils/pageLinks';
import { gotoLink } from '../../../utilities/routingUtils';

const component = {
  template: `
    <grading-assignment-list
      store="assignmentStore"
      view-one="gradeAssignment"
      view-all="viewAssignmentsPage">
    </grading-assignment-list>
  `,
  controller: [
    '$scope',
    'GradingQueueStoreLight',
    function ($scope, GradingQueueStoreLight) {
      $scope.assignmentStore = new GradingQueueStoreLight();
      $scope.assignmentStore.gotoPage(1);

      $scope.gradeAssignment = function (assignment) {
        gotoLink(
          InstructorGraderPageLink.toLink({
            contentId: assignment.id,
          })
        );
      };

      $scope.viewAssignmentsPage = function () {
        gotoLink(InstructorAssignmentListPageLink.toLink());
      };
    },
  ],
};

export let GradingQueue = 'GradingQueue: ng module not found';

import { angular2react } from 'angular2react';

export default angular
  .module('ple.pages.instructor.gradingQueueReact', [
    assignmentLists.name,
    GradingQueueStoreLight.name,
  ])
  .component('gradingQueueReact', component)
  .run([
    '$injector',
    function ($injector) {
      GradingQueue = angular2react('gradingQueueReact', component, $injector);
    },
  ]);
