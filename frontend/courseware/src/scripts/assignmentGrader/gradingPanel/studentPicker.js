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

import { find } from 'lodash';

import template from './studentPicker.html';
import pickerTemplate from './studentPickerModal.html';
import studentPickerStore from './StudentPickerStore.js';
import errorService from '../../utilities/errorService.jsx';
import coloredGradient from '../../directives/coloredGradient.js';

export default angular
  .module('lo.assignmentGrader.gradingPanelStudentPicker', [
    errorService.name,
    studentPickerStore.name,
    coloredGradient.name,
  ])
  .component('gradingPanelStudentPicker', {
    template: template,
    bindings: {
      grader: '<',
    },
    controller: [
      '$rootScope',
      '$uibModal',
      function ($rootScope, $uibModal) {
        //pick students
        this.showStudentListModal = function () {
          $rootScope.$emit('BootstrapModalOpened', { open: true });

          const grader = this.grader;

          $uibModal
            .open({
              template: pickerTemplate,
              controller: 'GradingPanelStudentPickerCtrl',
              resolve: {
                grader: function () {
                  return grader;
                },
              },
            })
            .result.then(function (studentId) {
              grader.changeUser(studentId);
            });
        };
      },
    ],
  })
  .controller('GradingPanelStudentPickerCtrl', [
    '$scope',
    'grader',
    'errorService',
    'StudentPickerStore',
    '$uibModalInstance',
    function ($scope, grader, errorService, StudentPickerStore, $uibModalInstance) {
      $scope.store = new StudentPickerStore(grader);
      $scope.store.gotoPage(1);

      $scope.select = function (student) {
        grader
          .confirmDiscardChanges()
          .then(() => grader.loadGradableUsers())
          .then(users => $scope.selectUser(student, users));
      };

      $scope.selectUser = function (student, users) {
        var target = find(users, function (user) {
          return student.id === user.id;
        });

        if (target && $uibModalInstance) {
          $uibModalInstance.close(target.id);
        } else {
          $scope.showError();
        }
      };

      $scope.showError = () => {
        return errorService.generic(
          'StudentHasNoSubmission',
          //This actually seems like a flaw?
          'CannotGradeTillSubmit',
          [],
          { hideSecondaryButton: true }
        );
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel', null, '');
      };
    },
  ]);
