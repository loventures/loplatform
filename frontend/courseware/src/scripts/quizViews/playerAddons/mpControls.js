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

import { trackQuizKeyboard } from '../../analytics/trackEvents.js';

import template from './mpControls.html';
import submit from './quizSubmitButton.js';

export default angular
  .module('lo.quizViews.mpControls', [submit.name])
  .constant('ConfidenceButtonConfigs', [
    {
      value: 1,
      class: 'confidence-high',
      label: 'CONFIDENCE_HIGH',
    },
    {
      value: 0.5,
      class: 'confidence-med',
      label: 'CONFIDENCE_MEDIUM',
    },
    {
      value: 0,
      class: 'confidence-low',
      label: 'CONFIDENCE_LOW',
    },
  ])
  .component('mpControls', {
    template,
    bindings: {
      displayConfidenceIndicators: '<',
      displaySkip: '<',

      selectedConfidence: '<',
      hasUnsavedChanges: '<',
      isLastQuestion: '<',
      unansweredQuestions: '<',
      onAttempt: '<',
      maxAttempts: '<',

      canEditAnswer: '<',
      nextQuestion: '<',

      canSubmitQuestion: '<',
      submitQuestion: '<',

      canSubmitQuiz: '<',
      submitQuiz: '<',

      canSkipQuestion: '<',
      skipQuestion: '<',

      isSubmitting: '<',
      isCheckpoint: '<',
    },
    controller: [
      '$document',
      'Settings',
      'ConfidenceButtonConfigs',
      function ($document, Settings, ConfidenceButtonConfigs) {
        this.warnUnsaved = Settings.isFeatureEnabled('AssessmentSaveWarning');
        this.confidenceButtons =
          Settings.getSettings('QuizConfidenceButtons') || ConfidenceButtonConfigs;
        this.canSkipQuestion = this.displaySkip && Settings.isFeatureEnabled('SkippingIsOK');

        this.$onInit = () => {
          $document.on('keydown', this.keybinding);
        };

        this.$onDestroy = () => {
          $document.off('keydown', this.keybinding);
        };

        this.keybinding = e => {
          const target = angular.element(e.target);
          if (
            target.is('.lo-rich-text-editor') ||
            target.is('.fill-blank-blank input') ||
            target.parents('.modal').length > 0
          ) {
            e.stopPropagation();
            return;
          }

          trackQuizKeyboard(e.which, 'mpControls');

          if (e.which === 13) {
            if (this.canSubmitQuestion) {
              this.submitQuestion();
            } else if (this.canSubmitQuiz) {
              this.showSubmitWarning = true;
            }
            return;
          }

          const input = +String.fromCharCode(e.which);

          if (!this.canEditAnswer && input === 1) {
            this.nextQuestion();
          } else if (!this.displayConfidenceIndicators && input === 1) {
            if (this.canSubmitQuestion) {
              this.submitQuestion();
            }
          } else if (this.displayConfidenceIndicators && this.confidenceButtons[input - 1]) {
            if (this.canSubmitQuestion) {
              this.submitQuestion(this.confidenceButtons[input - 1].value);
            }
          } else if (input === 4 && this.canSkipQuestion) {
            this.skipQuestion();
          }
        };
      },
    ],
  });
