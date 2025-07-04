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

export default angular.module('lo.utilties.errorService', []).factory('errorService', [
  '$rootScope',
  '$uibModal',
  function ($rootScope, $uibModal) {
    var service = function (title, message, actions, buttons) {
      title = title || 'Error';
      message = message || 'An error ocurred';
      actions = actions || ['OK'];
      buttons = buttons || { hideSecondaryButton: false };

      var $scope = $rootScope.$new();

      $scope.error = {
        title: title,
        message: message,
        actions: actions,
        buttons: buttons,
      };

      $scope.selected = {
        action: $scope.error.actions[0],
      };

      console.log('modal prepared ', $scope.error.actions);

      var $uibModalInstance = $uibModal.open({
        scope: $scope,
        template: '<error-modal></error-modal>',
      });

      $scope.ok = function () {
        $uibModalInstance.close($scope.selected.action);
      };

      $scope.cancel = function () {
        console.log('cancel');
        $uibModalInstance.dismiss('cancel');
      };

      return $uibModalInstance.result;
    };

    service.generic = function () {
      return service.apply(service, arguments);
    };

    return service;
  },
]);
