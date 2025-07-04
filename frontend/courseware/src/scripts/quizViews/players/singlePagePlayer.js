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

import { loadQuizActivityActionCreator } from '../../courseActivityModule/actions/quizActivityActions.js';
import {
  autosaveQuizActionCreatorMaker,
  changeQuestionAnswerActionCreatorMaker,
  saveQuizActionCreatorMaker,
  submitQuizActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerActions.js';
import ng from '../../quizPlayerModule/ng.js';
import { singlePagePlayerSelectorCreator } from '../../quizPlayerModule/selectors/singlePagePlayerSelectors.js';
import questionLoader from '../../quizQuestions/questionLoader.jsx';
import NavBlockerService from '../../services/NavBlockerService.js';
import { selectCurrentUser } from '../../utilities/rootSelectors.js';

import autosave from '../playerAddons/quizAutosave.js';
import submit from '../playerAddons/quizSubmitButton.js';
import template from './singlePagePlayer.html';

export default angular
  .module('lo.quiz.singlePagePlayer', [
    autosave.name,
    submit.name,
    NavBlockerService.name,
    questionLoader.name,
    ng.name,
  ])
  .component('singlePagePlayer', {
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
      'NavBlockerService',
      function ($ngRedux, $timeout, Settings, NavBlockerService) {
        this.allowSaving = true;
        this.$onInit = () => {
          $ngRedux.connectToCtrl(singlePagePlayerSelectorCreator(this.assessment, this.attemptId), {
            changeAnswer: changeQuestionAnswerActionCreatorMaker(this.attemptId),
            _saveQuiz: saveQuizActionCreatorMaker(this.attemptId, this.assessment),
            _autosaveQuiz: autosaveQuizActionCreatorMaker(this.attemptId, this.assessment),
            _submitQuiz: submitQuizActionCreatorMaker(this.attemptId, this.assessment),
            _discardQuiz: () => (dispatch, getState) => {
              const state = getState();
              const viewingAs = selectCurrentUser(state);
              dispatch(loadQuizActivityActionCreator(this.assessment, viewingAs, viewingAs.id));
            },
          })(this);

          this.setupBlocker();
        };

        this.setupBlocker = () => {
          const navBlockCondition = () => {
            return this.anyQuestionHasUnsavedChanges;
          };

          const navBlockMessage = 'QUIZ_CONFIRM_MOVE_UNSAVED_CHANGES';

          this.removeNavBlocker = NavBlockerService.register(navBlockCondition, navBlockMessage);
        };

        this.$onDestroy = () => {
          this.removeNavBlocker();
        };

        this.saveQuiz = () => {
          this._saveQuiz(this.allUnsavedChanges);
        };

        this.submitQuiz = () => {
          this._submitQuiz(this.allUnsavedChanges);
        };

        this.discardQuiz = () => {
          this._discardQuiz();
        };

        this.autosaveQuiz = () => {
          this._autosaveQuiz(this.allUnsavedChanges);
        };
      },
    ],
  });
