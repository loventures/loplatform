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

import { isEmpty } from 'lodash';

import template from './authenticAssessmentSubmissionSection.html';

import deleteAttemptModalTmpl from '../assessmentGrader/templates/deleteAttemptModal.html';
import deleteAttemptFailTmpl from '../assessmentGrader/templates/deleteAttemptFailModal.html';

export default angular
  .module('lo.assignmentGrader.authenticAssessmentSubmissionSection', [])
  .component('authenticAssessmentSubmissionSection', {
    template: template,
    bindings: {
      grader: '<',
      user: '<',
      attempt: '<',
    },
    controller: [
      '$uibModal',
      function ($uibModal) {
        this.updateAttempts = () => {
          return this.grader.loadUserOrderedAttempts().then(attempts => {
            this.attempts = attempts || [];
            this.hasAttempts = !isEmpty(attempts);
          });
        };

        this.$onChanges = changesObj => {
          if (
            (changesObj.user && changesObj.user.currentValue) ||
            (changesObj.attempt && changesObj.attempt.currentValue)
          ) {
            this.updateAttempts();
          }
        };

        this.$onInit = () => {
          //this is required because angular is not
          //consistent with $onChange with existing values
          this.startingAttempt = false;
          this.updateAttempts();
          this.grader.registerOnChangeCallback(this.updateAttempts);
        };

        this.changeAttempt = attempt => {
          return this.grader.changeAttempt(attempt.id);
        };

        this.confirmDelete = () => {
          return $uibModal
            .open({
              template: deleteAttemptModalTmpl,
            })
            .result.then(() => {
              this.grader.invalidateAttempt().catch(function errModal() {
                return $uibModal.open({
                  template: deleteAttemptFailTmpl,
                }).result;
              });
            });
        };

        this.startAttempt = () => {
          this.startingAttempt = true;
          this.grader.startAttempt().then(newAttempt => {
            this.changeAttempt(newAttempt).then(() => {
              this.startingAttempt = false;
            });
          });
        };

        this.attemptSelectedTemplate = `
            <attempt-dropdown-item
              attempt="$ctrl.selected"
              icon="'icon-chevron-down'"
            ></attempt-dropdown-item>
        `;

        this.attemptOptionTemplate = `
            <attempt-dropdown-item
              attempt="option"
            ></attempt-dropdown-item>
        `;
      },
    ],
  });
