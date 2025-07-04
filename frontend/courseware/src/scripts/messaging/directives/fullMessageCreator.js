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

import { richTextEditor } from '../../contentEditor/index.js';

import FullMessage from '../FullMessage.js';
import fullMessageCreatorTmpl from './fullMessageCreator.html';

export default angular
  .module('lo.messaging.directives.fullMessageCreator', [richTextEditor.name, FullMessage.name])
  .directive('fullMessageCreator', [
    '$timeout',
    function ($timeout) {
      return {
        restrict: 'E',
        template: fullMessageCreatorTmpl,
        scope: {
          recipients: '=?',
          entireClass: '=?',
        },
        controller: [
          '$scope',
          '$translate',
          'FullMessage',
          function ($scope, $translate, FullMessage) {
            $scope.init = function () {
              $scope.message = new FullMessage();

              $scope.message.selectingEntireClass = $scope.entireClass === 'true';

              $scope.message.setSelection($scope.recipients);

              $scope.titleErrorMessage = '';
            };

            $scope.validateAndSend = function () {
              $scope.message.feedbackManager.confirmResetByModal().then(function () {
                $scope.sendMessage();
              });
            };

            $scope.messageChanged = function (updatedContent) {
              $scope.message.content = updatedContent;
            };

            $scope.sendMessage = function () {
              // We validate the final message content here, but child directives validate their own state
              // and display errors as appropriate.
              $scope.$broadcast('validate');
              if (!$scope.message.title) {
                $scope.titleErrorMessage = $translate.instant('MESSAGING_ENTER_SUBJECT_ERROR');
              } else {
                $scope.titleErrorMessage = '';
              }

              if ($scope.message.hasRecipients() && $scope.message.title) {
                $scope.sendingMessage = true;
                $scope.sentMessage = false;
                $scope.errorMessage = false;

                $scope.message.send().then(
                  function () {
                    $scope.sendingMessage = false;
                    $scope.message = new FullMessage();

                    $scope.uiMessageSent();
                  },
                  function (err) {
                    console.error('Error sending message', err);
                    $scope.errorMessage = true;
                    $scope.errorMessageContent = err;
                    $scope.sendingMessage = false;
                    $scope.sentMessage = false;
                  }
                );
              }
            };

            $scope.uiMessageSent = function () {
              $scope.sentMessage = true;
              $timeout(function () {
                $scope.sentMessage = false;
              }, 1000 * 5);

              $timeout(function () {
                $scope.message = new FullMessage();
              }, 1000); //Force them to see the green success, THEN let them type again
            };
          },
        ],
      };
    },
  ]);
