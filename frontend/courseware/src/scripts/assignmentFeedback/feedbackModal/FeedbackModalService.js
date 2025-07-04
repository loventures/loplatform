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

import feedbackModal from './feedbackModal.html';

export default angular
  .module('lo.feedback.FeedbackModalService', [])
  .factory('FeedbackModalService', [
    '$rootScope',
    '$uibModal',
    function ($rootScope, $uibModal) {
      var service = {};

      service.open = function (type) {
        var config = service.create(type);
        return $uibModal.open(config).result;
      };

      service.create = function (type) {
        var scope = $rootScope.$new();

        scope.data = {
          type: type,
        };

        return {
          scope: scope,
          template: feedbackModal,
          controller: [
            '$scope',
            '$uibModalInstance',
            function ($scope, $uibModalInstance) {
              $scope.confirm = () => {
                $uibModalInstance.close(true);
              };

              $scope.cancel = () => {
                $uibModalInstance.dismiss('cancel');
              };
            },
          ],
        };
      };

      return service;
    },
  ]);
