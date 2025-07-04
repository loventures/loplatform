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

import { choiceOrdinal } from '../../../filters/choiceOrdinalFilter.js';
import { SELECTION_TYPE_MULTIPLE_CHOICE } from '../../../utilities/questionTypes.js';

import questionTemplate from '../../questionTemplates/basicQuestionTemplate.js';
import choiceRowTemplate from '../../questionTemplates/questionDistractorRowTemplate.js';
import template from './multipleChoiceQuestion.html';
import baseViewTemplate from './multipleChoiceQuestionBaseView.html';
import printViewTemplate from './multipleChoiceQuestionPrintView.html';

const multipleChoiceQuestionCtrl = [
  '$translate',
  function ($translate) {
    this.$onChanges = ({ response }) => {
      if (response) {
        this.selectedIndex =
          response.currentValue && response.currentValue.selection
            ? response.currentValue.selection.selectedIndexes[0]
            : null;
      }
    };

    this.selectChoiceIndex = index => {
      const response = this.response || {};
      const selection = response.selection || {
        responseType: SELECTION_TYPE_MULTIPLE_CHOICE,
      };
      this.changeAnswer(this.index, {
        ...response,
        selection: {
          ...selection,
          selectedIndexes: [index],
        },
      });
    };

    this.hasCorrectness = (index, choice) => {
      return (
        this.question.displayDetail.correctAnswer &&
        (index === this.selectedIndex || choice.correct)
      );
    };

    this.choiceOrdinalAriaLabel = (index, choice) => {
      let key;
      if (this.hasCorrectness(index, choice)) {
        key = choice.correct
          ? index === this.selectedIndex
            ? 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT'
            : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_CORRECT_NOT_SELECTED'
          : 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL_INCORRECT_SELECTED';
      } else {
        key = 'QUIZ_PLAYER_CHOICE_ORDINAL_LABEL';
      }

      return $translate.instant(key, { ordinal: choiceOrdinal(index) });
    };
  },
];

export default angular
  .module('lo.questions.multipleChoiceQuestion', [questionTemplate.name, choiceRowTemplate.name])
  .component('multipleChoiceQuestion', {
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
  .component('multipleChoiceQuestionBaseView', {
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
    controller: multipleChoiceQuestionCtrl,
  })
  .component('multipleChoiceQuestionPrintView', {
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
    controller: multipleChoiceQuestionCtrl,
  });
