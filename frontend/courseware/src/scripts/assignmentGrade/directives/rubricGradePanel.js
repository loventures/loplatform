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

import tmpl from './rubricGradePanel.html';
import gradingRubric from './rubric/gradingRubric.js';
import gradingRubricViewCards from './rubric/gradingRubricViewCards.js';

export default angular
  .module('lo.assignmentGrade.directives.rubricGradePanel', [
    gradingRubric.name,
    gradingRubricViewCards.name,
  ])
  .directive('rubricGradePanel', function () {
    return {
      restrict: 'E',
      template: tmpl,
      scope: {
        grade: '=',
        disableEdit: '=?',
      },
      controller: [
        '$scope',
        '$timeout',
        '$element',
        function ($scope, $timeout, $element) {
          $scope.disableEdit = angular.isDefined($scope.disableEdit) ? $scope.disableEdit : false;

          // Our version of angular does not seem to support interpolation
          // '{{}}' inside the min/max attributes.  When we upgrade see if
          // removing this watch works for rubricless composite grades.
          $scope.$watch('grade.outgoing.pointsAwarded', function (val) {
            $timeout(function () {
              $element.find('.direct-grade-input').val(val);
            });
          });
        },
      ],
    };
  });
