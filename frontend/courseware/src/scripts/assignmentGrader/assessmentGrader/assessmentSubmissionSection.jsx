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

import { isFunction } from 'lodash';

import loSelect from '../../directives/loSelect/index.js';
import template from './assessmentSubmissionSection.html';

import attemptDropdownItemTmpl from './templates/attemptDropdownItem.html';
import questionDropdownItemTmpl from './templates/questionDropdownItem.html';

import deleteAttemptModalTmpl from './templates/deleteAttemptModal.html';
import deleteAttemptFailTmpl from './templates/deleteAttemptFailModal.html';

export default angular
  .module('lo.assignmentGrader.assessmentSubmissionSection', [loSelect.name])
  .component('assessmentSubmissionSection', {
    template: template,
    bindings: {
      grader: '<',
      user: '<',
      attempt: '<',
      activeQuestions: '<',
    },
    controller: [
      '$uibModal',
      function ($uibModal) {
        this.updateAttempts = () => {
          return this.grader.loadUserOrderedAttempts().then(attempts => (this.attempts = attempts));
        };

        this.updateBoth = () => {
          this.updateAttempts();

          if (isFunction(this.grader.loadAttemptGradableQuestions)) {
            this.grader
              .loadAttemptGradableQuestions()
              .then(questions => (this.questions = questions));
          }
        };

        this.$onChanges = changesObj => {
          if (changesObj.user && changesObj.user.currentValue) {
            this.updateAttempts();
          } else if (changesObj.attempt && changesObj.attempt.currentValue) {
            this.updateBoth();
          }
        };

        this.$onInit = () => {
          //this is required because angular is not
          //consistent with $onChange with existing values
          this.updateBoth();
        };

        this.changeAttempt = attempt => {
          this.grader.changeAttempt(attempt.id);
        };

        this.changeQuestion = question => {
          this.grader.changeQuestion(question.index);
        };

        this.confirmDelete = () => {
          return $uibModal
            .open({
              template: deleteAttemptModalTmpl,
            })
            .result.then(() => {
              // TODO: Consider moving business logic onto AssessmentAttemptGrade
              this.grader.invalidateAttempt().catch(function errModal() {
                return $uibModal.open({
                  template: deleteAttemptFailTmpl,
                }).result;
              });
            });
        };

        this.showQuestionsSelector = () => {
          return (
            this.activeQuestions &&
            this.activeQuestions[0] &&
            this.grader.assignmentType !== 'final-project'
          );
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
        this.questionSelectedTemplate = `
            <question-dropdown-item
              question="$ctrl.selected"
              icon="'icon-chevron-down'"
            ></question-dropdown-item>
        `;
        this.questionOptionTemplate = `
            <question-dropdown-item
              question="option"
            ></question-dropdown-item>
        `;
      },
    ],
  })
  .directive('attemptDropdownItem', function () {
    return {
      restrict: 'E',
      scope: {
        attempt: '=',
        grade: '=',
        isActive: '=',
        icon: '=',
      },
      template: attemptDropdownItemTmpl,
    };
  })
  .directive('questionDropdownItem', function () {
    return {
      restrict: 'E',
      scope: {
        question: '=',
        isActive: '=',
        icon: '=',
      },
      template: questionDropdownItemTmpl,
    };
  });
