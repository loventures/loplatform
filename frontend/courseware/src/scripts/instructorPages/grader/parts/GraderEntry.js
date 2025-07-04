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

import { connect } from 'react-redux';
import { selectGraderPageComponent } from '../selectors';
import browserType, { IE } from '../../../utilities/browserType';

import template from './graderEntry.html';

import { InstructorDashboardPageLink } from '../../../utils/pageLinks';
import { gotoLink } from '../../../utilities/routingUtils';
import { isSubmission } from '../../../utilities/contentTypes';

import graderModule from '../../../assignmentGrader';

const component = {
  template,
  bindings: {
    content: '<',
  },
  controller: [
    '$scope',
    '$ngRedux',
    'QuizAPI',
    'SubmissionActivityAPI',
    function ($scope, $ngRedux, QuizAPI, SubmissionActivityAPI) {
      //CBLPROD-1060.  IE PDF viewer is screwy on very small screens
      $scope.isIE = browserType === IE;

      $scope.isSubmission = isSubmission($scope.$ctrl.content);
      $scope.isAssessment = !$scope.isSubmission;

      $scope.status = {};

      $scope.returnLink = () => {
        gotoLink(InstructorDashboardPageLink.toLink());
      };

      $scope.loadAssignment = function () {
        if ($scope.isSubmission) {
          return SubmissionActivityAPI.loadSubmissionAssessment($scope.$ctrl.content.contentId);
        } else {
          return QuizAPI.loadQuiz($scope.$ctrl.content.contentId);
        }
      };

      $scope.loadAssignment().then(assignment => {
        if ($scope.$ctrl.content) {
          assignment.dueDate = $scope.$ctrl.content.dueDate;
        }

        $scope.assignmentId = assignment.contentId;
        $scope.assignmentName = assignment.name || assignment.title;
        $scope.dueDate = assignment.dueDate;
        $scope.assignment = assignment;
        $scope.status.assignmentLoaded = true;
      });
    },
  ],
};

export let GraderEntry = 'GraderEntry: ng module not found';

import { angular2react } from 'angular2react';

export default angular
  .module('ple.pages.instructor.graderEntryReact', [graderModule.name])
  .component('graderEntryReact', component)
  .run([
    '$injector',
    function ($injector) {
      GraderEntry = connect(selectGraderPageComponent)(
        angular2react('graderEntryReact', component, $injector)
      );
    },
  ]);
