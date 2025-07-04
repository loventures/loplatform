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

import { each } from 'lodash';
import { SELECTION_TYPE_FILL_BLANK } from '../../../utilities/questionTypes.js';

import template from './fillBlankQuestion.html';
import baseViewTemplate from './fillBlankQuestionBaseView.html';
import printViewTemplate from './fillBlankQuestionPrintView.html';

const buildQuestionText = (question, createBlankFn) => {
  let questionText = '';
  let nextIndex = 0;
  each(question.blanks, (blank, idx) => {
    const startIndex = blank.startIndex || blank.offset;
    questionText += `
            <div class="fill-blank-text">
                ${question.questionText.slice(nextIndex, startIndex)}
            </div>
        `;
    questionText += createBlankFn(idx, blank);
    nextIndex = blank.endIndex + 1 || blank.offset;
  });

  questionText += question.questionText.slice(nextIndex);
  return questionText;
};

const responseToEntries = response => [...((response.selection || {}).entries || [])];

export default angular
  .module('lo.questions.fillBlankQuestion', [])
  .component('fillBlankQuestion', {
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
  .component('fillBlankQuestionBaseView', {
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
    controller: function () {
      const createBlankHtml = index => {
        return `
                <div class="fill-blank-blank">
                    <input class="form-control"
                        ${index === 0 && this.focusOnRender ? 'lo-autofocus' : ''}
                        aria-label="{{ 'FILL_BLANK_BLANK_LABEL' | translate}}"
                        ng-if="!$ctrl.showCorrect"
                        ng-model="$ctrl.selection[${index}]"
                        ng-change="$ctrl.inputChanged(${index})"
                        ng-disabled="!$ctrl.canEditAnswer"
                    />

                    <div class="fill-blank-answers"
                        ng-if="!$ctrl.canEditAnswer && $ctrl.showCorrect"
                    >
                      <span translate="FILL_BLANK_CORRECT_ANSWER_LABEL"></span>
                      <span class="blank-candidate"
                        ng-repeat="candidate in $ctrl.question.blanks[${index}].answers track by $index"
                        ng-bind="candidate"
                      ></span>
                    </div>
                </div>
            `;
      };

      this.$onInit = () => {
        this.questionText = buildQuestionText(this.question, createBlankHtml);

        if (this.response) {
          this.showResponse();
        } else {
          this.showAnswer();
        }
      };

      this.$onChanges = ({ response }) => {
        if (response && response.currentValue) {
          this.selection = responseToEntries(response.currentValue);
        }
      };

      this.selectionToResponse = (idx, selection) => {
        const updatedEntries = responseToEntries(this.response);
        updatedEntries[idx] = selection;
        return {
          ...this.response,
          selection: {
            ...this.response.selection,
            responseType: SELECTION_TYPE_FILL_BLANK,
            entries: updatedEntries,
          },
        };
      };

      this.inputChanged = idx => {
        this.changeAnswer(this.index, this.selectionToResponse(idx, this.selection[idx]));
      };

      this.showResponse = () => (this.showCorrect = false);
      this.showAnswer = () => (this.showCorrect = true);
    },
  })
  .component('fillBlankQuestionPrintView', {
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
    controller: function () {
      const createBlankHtml = index => {
        return `
                <div>
                    <input class="form-control"
                        ng-value="$ctrl.selection[${index}]"
                        ng-disabled="!$ctrl.canEditAnswer"
                        ng-if="$ctrl.selection"
                    />
                    <div class="fill-blank-answers"
                        ng-if="!$ctrl.canEditAnswer && $ctrl.question.displayDetail.correctAnswer"
                    >
                      <span translate="FILL_BLANK_CORRECT_ANSWER_LABEL"></span>
                      <span class="blank-candidate"
                        ng-repeat="candidate in $ctrl.question.blanks[${index}].answers track by $index"
                        ng-bind="candidate"
                      ></span>
                    </div>
                    <div class="fill-blank-answers"
                        ng-if="!$ctrl.canEditAnswer && !$ctrl.question.displayDetail.correctAnswer"
                    >
                      <span class="blank-candidate" style="min-width: 10rem">&nbsp;</span>
                    </div>
                </div>
            `;
      };

      this.$onInit = () => {
        this.questionText = buildQuestionText(this.question, createBlankHtml);
      };

      this.$onChanges = ({ response }) => {
        if (response && response.currentValue) {
          this.selection = responseToEntries(response.currentValue);
        }
      };
    },
  });
