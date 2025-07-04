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

import modal from 'angular-ui-bootstrap/src/modal';
import uibPopover from 'angular-ui-bootstrap/src/popover';
import { map } from 'lodash';
import autofocus from '../../utilities/autofocus.js';

import template from './quizSubmitButton.html';
import quizSubmitButtonModal from './quizSubmitModal.html';

export default angular
  .module('lo.quizViews.quizSubmitButton', [autofocus.name, uibPopover])
  .component('quizSubmitButton', {
    template,
    modal,
    bindings: {
      canSubmit: '<',
      unansweredQuestions: '<',
      onAttempt: '<',
      maxAttempts: '<',
      submitQuiz: '<',
      enableAutofocus: '<',
      showWarning: '<',
      isCheckpoint: '<',
    },
    controller: [
      '$element',
      '$timeout',
      '$templateCache',
      '$uibModal',
      function ($element, $timeout, $templateCache, $uibModal) {
        this.$onChanges = ({ unansweredQuestions, showWarning }) => {
          if (unansweredQuestions?.currentValue) {
            this.unansweredQuestionCount = unansweredQuestions.currentValue.length;
            this.unansweredQuestionNumbers = map(
              unansweredQuestions.currentValue,
              q => q.ordinal
            ).join(', ');
            this.warnUnanswered =
              unansweredQuestions.currentValue && unansweredQuestions.currentValue.length > 0;
          }

          if (showWarning && showWarning.currentValue) {
            this.click();
          }
        };

        this.$onDestroy = () => {
          if (this.isOpen) {
            this.isOpen = false;
            this.modal?.dismiss();
          }
        };

        this.enableAutofocus = this.enableAutofocus || false;
        this.isOpen = false;

        this.click = () => {
          if (!this.isOpen) {
            this.isOpen = true;
            this.modal = $uibModal.open({
              template: quizSubmitButtonModal,
              resolve: {
                $buttonCtrl: () => this,
              },
              controllerAs: '$ctrl',
              controller: 'quizSubmitModal',
            });
            this.modal.result.then(this.submitQuiz).catch(this.cancel);
          }
        };

        this.warnMaxAttempts = this.maxAttempts > 0 && !this.isCheckpoint;
        this.warnUnanswered = this.unansweredQuestions.length > 0;
        this.maxAttemptsMsg =
          this.maxAttempts === 1
            ? 'WARN_ONLY_ATTEMPT_MSG'
            : this.onAttempt === this.maxAttempts
              ? 'WARN_FINAL_ATTEMPT_MSG'
              : 'WARN_MAX_ATTEMPT_MSG';
        this.submitMsg = this.isCheckpoint ? 'CHECKPOINT_PLAYER_SUBMIT' : 'QUIZ_PLAYER_SUBMIT';

        this.cancel = () => {
          this.isOpen = false;
          this.modal = null;
          $timeout(() => $element.find('button').focus(), 250);
        };
      },
    ],
  })
  .controller('quizSubmitModal', [
    '$uibModalInstance',
    '$buttonCtrl',
    function ($uibModalInstance, $buttonCtrl) {
      this.maxAttemptsMsg = $buttonCtrl.maxAttemptsMsg;
      this.warnMaxAttempts = $buttonCtrl.warnMaxAttempts;
      this.onAttempt = $buttonCtrl.onAttempt;
      this.maxAttempts = $buttonCtrl.maxAttempts;
      this.warnUnanswered = $buttonCtrl.warnUnanswered;
      this.unansweredQuestionCount = $buttonCtrl.unansweredQuestionCount;
      this.unansweredQuestionNumbers = $buttonCtrl.unansweredQuestionNumbers;
      this.isCheckpoint = $buttonCtrl.isCheckpoint;
      this.submitQuiz = () => {
        $uibModalInstance.close();
      };
      this.cancel = () => {
        $uibModalInstance.dismiss();
      };
    },
  ]);
