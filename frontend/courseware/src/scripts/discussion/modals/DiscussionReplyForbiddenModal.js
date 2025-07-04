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

import template from './discussionReplyForbiddenModal.html';

import uibModal from 'angular-ui-bootstrap/src/modal';

export default angular
  .module('lo.discussion.DiscussionReplyForbiddenModal', [uibModal])
  .factory('DiscussionReplyForbiddenModal', [
    '$uibModal',
    function ($uibModal) {
      var service = {};

      service.buildModalConfig = config => {
        return {
          template,
          controller: [
            '$scope',
            '$uibModalInstance',
            function ($scope, $uibModalInstance) {
              $scope.close = keep => $uibModalInstance.close(keep);
              $scope.title = config.title;
              $scope.description = config.description;
            },
          ],
        };
      };

      service.open = state => {
        return $uibModal.open(service.buildModalConfig(state)).result;
      };

      return service;
    },
  ]);
