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

import tmpl from './submissionScore.html';

export default angular
  .module('lo.assignmentGrader.submissionScore', [])
  .directive('submissionScore', function () {
    return {
      restrict: 'E',
      template: tmpl,
      scope: {
        grade: '=',
        invalidated: '=?',
        late: '=?',
        disableEdit: '=?',
      },
      controller: [
        '$scope',
        function ($scope) {
          $scope.$watch('grader.activeGrade.outgoing.pointsAwarded', function () {
            if (!$scope.grade) {
              return false;
            }

            if (!$scope.grade.outgoing.pointsAwarded && $scope.grade.outgoing.pointsAwarded !== 0) {
              $scope.scaledAwarded = null;
            } else {
              const scale = $scope.grade.scaledPointsPossible / $scope.grade.pointsPossible;

              $scope.scaledAwarded = $scope.grade.outgoing.pointsAwarded * scale;

              $scope.scaledAwarded = Math.round($scope.scaledAwarded * 100) / 100;
            }
          });
        },
      ],
    };
  });
