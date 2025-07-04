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

export default angular.module('lo.modals.idleModal', []).controller('IdleModalCtrl', [
  '$scope',
  '$uibModalInstance',
  '$timeout',
  function ($scope, $uibModalInstance, $timeout) {
    $scope.close = $uibModalInstance.close;

    $scope.$on('$destroy', function () {
      if ($scope.secondsLeft <= 0) {
        $scope.close(false);
      }
    });

    $scope.cycleTime = 1000;

    $scope.updateTimer = function () {
      $scope.secondsLeft = $scope.expiry;

      $scope.expiry -= $scope.cycleTime;

      $timeout($scope.updateTimer, $scope.cycleTime);
    };

    $scope.updateTimer();
  },
]);
