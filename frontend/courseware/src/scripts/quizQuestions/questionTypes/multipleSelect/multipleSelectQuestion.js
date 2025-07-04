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

import { get, keyBy, keys, mapValues, pickBy, some } from 'lodash';
import { choiceOrdinal } from '../../../filters/choiceOrdinalFilter.js';
import { SELECTION_TYPE_MULTIPLE_SELECT } from '../../../utilities/questionTypes.js';

import questionTemplate from '../../questionTemplates/basicQuestionTemplate.js';
import choiceRowTemplate from '../../questionTemplates/questionDistractorRowTemplate.js';
import template from './multipleSelectQuestion.html';
import baseViewTemplate from './multipleSelectQuestionBaseView.html';
import printViewTemplate from './multipleSelectQuestionPrintView.html';

const multipleSelectQuestionCtrl = [
  '$translate',
  function ($translate) {
    this.$onChanges = ({ response }) => {
      if (response && response.currentValue) {
        this.responseToSelection(response.currentValue);
      }
    };

    this.responseToSelection = response => {
      const selectedIndexes = get(response, 'selection.selectedIndexes', {});
      this.selection = mapValues(
        keyBy(selectedIndexes, a => a),
        () => true
      );
    };

    this.selectionToResponse = () => {
      const response = this.response || {};
      let selection = response.selection || {
        responseType: SELECTION_TYPE_MULTIPLE_SELECT,
      };
      const hasSelection = some(this.selection, val => val);
      if (hasSelection) {
        selection.selectedIndexes = keys(pickBy(this.selection, val => val));
      } else {
        selection = null;
      }
      return {
        ...response,
        selection,
      };
    };

    this.toggleChoiceIndex = () => {
      this.changeAnswer(this.index, this.selectionToResponse());
    };

    this.hasCorrectness = choice => {
      return this.assessment.settings.isCheckpoint
        ? false
        : this.response
          ? this.question.displayDetail.correctAnswer
          : choice.correct;
    };

    this.isCorrect = (index, choice) => {
      return this.response
        ? (choice.correct && this.selection[index]) || (!choice.correct && !this.selection[index])
        : choice.correct;
    };

    this.choiceOrdinalAriaLabel = (index, choice) => {
      let key;
      if (this.hasCorrectness(choice)) {
        key = choice.correct
          ? this.isCorrect(index, choice)
            ? 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT'
            : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT_NOT_SELECTED'
          : this.isCorrect(index, choice)
            ? 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_INCORRECT_NOT_SELECTED'
            : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_INCORRECT_SELECTED';
      } else {
        key = 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL';
      }

      return $translate.instant(key, { ordinal: choiceOrdinal(index) });
    };
  },
];

export default angular
  .module('lo.questions.multipleSelectQuestion', [questionTemplate.name, choiceRowTemplate.name])
  .component('multipleSelectQuestion', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template,
    controller: [
      '$window',
      function ($window) {
        this.inPrintMode = () => $window.inPrintMode;
      },
    ],
  })
  .component('multipleSelectQuestionBaseView', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: baseViewTemplate,
    controller: multipleSelectQuestionCtrl,
  })
  .component('multipleSelectQuestionPrintView', {
    bindings: {
      index: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: printViewTemplate,
    controller: multipleSelectQuestionCtrl,
  });
