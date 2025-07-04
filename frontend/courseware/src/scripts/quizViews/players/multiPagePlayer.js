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

import {
  autosaveQuizActionCreatorMaker,
  changeQuestionAnswerActionCreatorMaker,
  gotoQuestionActionCreatorMaker,
  skipQuestionActionCreatorMaker,
  submitQuestionActionCreatorMaker,
  submitQuizActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerActions.js';
import ng from '../../quizPlayerModule/ng.js';
import { multiPagePlayerSelectorCreator } from '../../quizPlayerModule/selectors/multiPagePlayerSelectors.js';
import questionLoader from '../../quizQuestions/questionLoader.jsx';
import NavBlockerService from '../../services/NavBlockerService.js';

import controls from '../playerAddons/mpControls.js';
import nav from '../playerAddons/mpQuestionNav.js';
import autosave from '../playerAddons/quizAutosave.js';
import template from './multiPagePlayer.html';

export default angular
  .module('lo.quiz.multiPagePlayer', [
    autosave.name,
    NavBlockerService.name,
    nav.name,
    controls.name,
    questionLoader.name,
    ng.name,
  ])
  .component('multiPagePlayer', {
    template,
    bindings: {
      assessment: '<',
      attemptId: '<',
      onAttempt: '<',
      printView: '<?',
    },
    controller: [
      '$ngRedux',
      '$timeout',
      'Settings',
      'errorMessageFilter',
      'NavBlockerService',
      function ($ngRedux, $timeout, Settings, errorMessageFilter, NavBlockerService) {
        this.canSkipQuestion = Settings.isFeatureEnabled('SkippingIsOK');

        this.$onInit = () => {
          $ngRedux.connectToCtrl(multiPagePlayerSelectorCreator(this.assessment, this.attemptId), {
            changeAnswer: changeQuestionAnswerActionCreatorMaker(this.attemptId),
            _gotoQuestion: gotoQuestionActionCreatorMaker(this.attemptId),
            _skipQuestion: skipQuestionActionCreatorMaker(this.attemptId, this.assessment),
            _submitQuestion: submitQuestionActionCreatorMaker(this.attemptId, this.assessment),
            _autosaveQuiz: autosaveQuizActionCreatorMaker(this.attemptId, this.assessment),
            _submitQuiz: submitQuizActionCreatorMaker(this.attemptId, this.assessment),
          })(this);

          this.setupBlocker();
        };

        this.setupBlocker = () => {
          const navBlockCondition = () => this.currentQuestionHasUnsavedChanges;
          const navBlockMessage = 'QUIZ_CONFIRM_MOVE_UNSAVED_CHANGES';

          this.removeNavBlocker = NavBlockerService.register(navBlockCondition, navBlockMessage);
          this.confirmDiscard = () => NavBlockerService.confirmNavByModal([navBlockMessage]);
        };

        this.$onDestroy = () => {
          this.removeNavBlocker();
        };

        this.gotoQuestion = toIndex => {
          if (this.canGoStatus && !this.canGoStatus[toIndex]) {
            return;
          }

          this.confirmDiscard().then(() => {
            //to get a complete re-render of the question loader
            this.currentQuestion = null;
            $timeout(() => this._gotoQuestion(toIndex));
          });
        };

        this.nextQuestion = () => this.gotoQuestion(this.indexToGoAfterSkip);

        this.skipQuestion = () => {
          const skipResponse = { ...this.currentQuestionSavedResponse };
          skipResponse.selection.skip = true;
          this._skipQuestion(this.currentQuestionIndex, skipResponse, this.indexToGoAfterSkip);
        };

        this.submitQuestion = confidenceValue => {
          const response = { ...this.currentQuestionResponse };
          if (response.selection) {
            response.selection.confidence = confidenceValue;
            response.selection.skip = false;
          }
          this._submitQuestion(this.currentQuestionIndex, response, this.indexToGoAfter);
        };

        this.submitQuiz = () => {
          this._submitQuiz(this.allUnsavedChanges);
        };

        this.autosaveQuiz = () => {
          this._autosaveQuiz(this.allUnsavedChanges, this.currentQuestionIndex);
        };

        this.hasError = () =>
          (!this.questionSubmissionState.loading && this.questionSubmissionState.error) ||
          (!this.quizSubmissionState.loading && this.quizSubmissionState.error);

        this.getErrorMessage = () => {
          const error = this.quizSubmissionState.error || this.questionSubmissionState.error;
          return errorMessageFilter(error);
        };
      },
    ],
  });
