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

import headerTempl from './disconnectWarning.html';
import uibModal from 'angular-ui-bootstrap/src/modal';
import modalTempl from './disconnectAlertModal.html';
import disconnectService from '../../utilities/disconnectService';
import { appIsFramed } from '../../utilities/deviceType';
import Course from '../../bootstrap/course';

const maintenanceMessages = {
  banner: 'MAINTENANCE_MODE_STARTED_MESSAGE',
  modal: {
    title: 'MAINTENANCE_MODE_MODAL_TITLE',
    message: 'MAINTENANCE_MODE_MODAL_MESSAGE',
    dismiss: 'MAINTENANCE_MODE_MODAL_DISMISS_BUTTON',
  },
};
const maintenanceWithEndMessages = {
  ...maintenanceMessages,
  modal: {
    ...maintenanceMessages.modal,
    message: 'MAINTENANCE_MODE_MODAL_MESSAGE_WITH_END',
  },
};
const loggedoutMessages = {
  banner: 'LOGGED_OUT_BANNER_MESSAGE',
  modal: {
    title: 'LOGGED_OUT_MODAL_TITLE',
    message: 'LOGGED_OUT_MODAL_MESSAGE',
    dismiss: 'LOGGED_OUT_MODAL_DISMISS_BUTTON',
  },
};
const loggedinMessages = {
  banner: 'LOGGED_IN_BANNER_MESSAGE',
  modal: {
    title: 'LOGGED_IN_MODAL_TITLE',
    message: 'LOGGED_IN_MODAL_MESSAGE',
    dismiss: 'LOGGED_IN_MODAL_DISMISS_BUTTON',
  },
};
const transferredOutMessages = {
  banner: 'TRANSFERRED_OUT_BANNER_MESSAGE',
  modal: {
    title: 'TRANSFERRED_OUT_MODAL_TITLE',
    message: 'TRANSFERRED_OUT_MODAL_MESSAGE',
    dismiss: 'TRANSFERRED_OUT_MODAL_DISMISS_BUTTON',
  },
};

export default angular
  .module('lo.directives.disconnect', [uibModal, disconnectService.name])
  .directive('trackBannerHeight', function () {
    return {
      restrict: 'A',
      scope: {
        onChangeFn: '=trackBannerHeight',
        //        msgKey: '=',
      },
      controller: [
        '$scope',
        '$element',
        '$translate',
        '$window',
        function ($scope, $element, $translate, $window) {
          $scope.updateMessageAndAdjustHeight = function () {
            //          $translate(key).then(function(msg) {
            //            $element.find('.message').text(msg);
            const height = $element.outerHeight(true);
            $scope.onChangeFn(height);
            //          });
          };

          //        $scope.$watch('msgKey', function(key) {
          //          $scope.updateMessageAndAdjustHeight(key);
          //        });

          $window.onresize = function () {
            const height = $element.outerHeight(true);
            $scope.onChangeFn(height);
          };
          $scope.$on('$destroy', function () {
            $window.onresize = null;
          });

          $scope.updateMessageAndAdjustHeight();
        },
      ],
    };
  })
  .directive('maintenanceModeWarning', function () {
    return {
      restrict: 'E',
      template: headerTempl,
      scope: {},
      controller: [
        '$scope',
        '$rootScope',
        '$element',
        '$uibModal',
        'PresenceService',
        'DisconnectService',
        function ($scope, $rootScope, $element, $uibModal, PresenceService, DisconnectService) {
          $scope.adjustContentTopMargin = function (height) {
            $element.find('.content-top-margin').height(appIsFramed ? 0 : height);
          };

          const openModal = (messages, data) => {
            $scope.messages = messages; // really, friend?
            $scope.messageData = data; // yes really, friend?
            const modal = $uibModal.open({
              backdrop: 'static',
              template: modalTempl,
              scope: $scope,
            });
            modal.result.then(() => {
              DisconnectService.disconnect();
              $scope.showBanner = true;
            });
            return modal;
          };

          function onSystemEvent(ev) {
            if (ev.type === 'Maintenance') {
              openModal(ev.end ? maintenanceWithEndMessages : maintenanceMessages, ev);
            } else if (ev.type === 'Logout') {
              $rootScope.$emit('SessionService.logout');
              openModal(loggedoutMessages);
            } else if (ev.type === 'Login') {
              $rootScope.$emit('SessionService.logout');
              openModal(loggedinMessages);
            }
          }

          function onLearnerTransferEvent(ev) {
            if (Course.id === ev.sourceCourseId) {
              // If we are transfering out of the course we are in right now
              openModal(transferredOutMessages);
            }
          }

          PresenceService.onForScope('System', onSystemEvent, $scope);
          PresenceService.onForScope('LearnerTransferMessage', onLearnerTransferEvent, $scope);
        },
      ],
    };
  });
