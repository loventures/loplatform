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

import typeahead from 'angular-ui-bootstrap/src/typeahead';
import { angular2react } from 'angular2react';
import Course from '../../bootstrap/course.js';
import { filter } from 'lodash';

import MultiSelectPicker from '../../directives/multiSelectPicker/multiSelectPicker.js';
import UserListStore from '../../users/UserListStore.js';
import template from './recipientPicker.html';
import typeaheadTmpl from './typeaheadLine.html';

const component = {
  restrict: 'E',
  template,
  bindings: {
    message: '<',
  },
  controller: [
    '$scope',
    '$timeout',
    '$uibModal',
    '$templateCache',
    '$translate',
    'UserListStore',
    'Roles',
    function ($scope, $timeout, $uibModal, $templateCache, $translate, UserListStore, Roles) {
      var allowedRoles = ['advisor', 'instructor'];
      if (Roles.isInstructor()) {
        allowedRoles.push('student');
        allowedRoles.push('trialLearner');
      }
      $scope.userStore = new UserListStore(Course.id, allowedRoles);
      $scope.userStore.title = 'MESSAGING_RECIPIENT_PICKER_TO';

      const TYPEAHEAD_URL = 'RECIPIENT_TYPEAHEAD_TEMPLATE'; // eslint-disable-line redux-constants/redux-constants

      $scope.typeaheadModel = {};

      $templateCache.put(TYPEAHEAD_URL, typeaheadTmpl);
      $scope.typeaheadTemplateUrl = TYPEAHEAD_URL;

      $scope.status = {
        selectingEntireClass: $scope.$ctrl.message.selectingEntireClass,
      };

      $scope.message = $scope.$ctrl.message;

      $scope.isInstructor = Roles.isInstructor();

      $scope.recipientErrorMessage = '';

      $scope.$on('validate', function () {
        // eslint-disable-line no-unused-vars
        // We are valid if we have recipients either through the picker or by
        // checking to send to the entire class.
        if (!$scope.message.hasRecipients()) {
          $scope.recipientErrorMessage = $translate.instant(
            'MESSAGING_RECIPIENT_NO_RECIPIENT_ERROR'
          );
        } else {
          $scope.recipientErrorMessage = '';
        }
      });

      $scope.showUserSelector = function () {
        $scope.userStore.searchByName('');
        $scope.typeaheadModel.text = '';

        $uibModal
          .open({
            component: 'multi-select-picker-modal',
            size: 'lg',
            resolve: {
              store: () => $scope.userStore,
              selected: () => $scope.$ctrl.message.recipients,
            },
          })
          .result.then(function (selectionStatus) {
            $scope.message.setSelection(selectionStatus);
          });
      };

      $scope.addSelection = function (selected) {
        $scope.message.addSelection(selected);
        $scope.typeaheadModel.text = '';
      };

      $scope.resetRecipientError = function () {
        $scope.recipientErrorMessage = '';
      };

      $scope.removeSelection = function (selected) {
        $scope.message.removeSelection(selected);
      };

      $scope.selectEntireClass = function (selecting) {
        $scope.message.selectEntireClass(selecting);
        $scope.message.selectingEntireClass = selecting;
        $scope.status.selectingEntireClass = selecting;
        if (selecting) {
          $scope.recipientErrorMessage = '';
          $scope.message.setSelection([]);
        }
      };

      $scope.getRecipientPool = function (value) {
        return $scope.userStore.searchByName(value).then(function (users) {
          return filter(users, function (user) {
            return !$scope.message.isSelected(user);
          });
        });
      };
    },
  ],
};

export let RecipientPicker = props => <div {...props}>'RecipientPicker: ng module not found'</div>;

export default angular
  .module('lo.messaging.directives.recipientPicker', [
    MultiSelectPicker.name,
    typeahead,
    UserListStore.name,
  ])
  .component('recipientPicker', component)
  .run([
    '$injector',
    function ($injector) {
      RecipientPicker = angular2react('recipientPicker', component, $injector);
    },
  ]);
