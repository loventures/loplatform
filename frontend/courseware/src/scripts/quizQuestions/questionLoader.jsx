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

import questionTypes from './questionTypes/index.js';
import QuestionViewComponentRegistry from './QuestionViewComponentRegistry.js';

export default angular
  .module('lo.questions.questionLoader', [questionTypes.name, QuestionViewComponentRegistry.name])
  .component('questionLoader', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
      grading: '<',
    },
    template: `
        <div ng-if="$ctrl.template" compile="$ctrl.template"></div>
    `,
    controller: [
      'QuestionViewComponentRegistry',
      '$timeout',
      function (QuestionViewComponentRegistry, $timeout) {
        const renderArgs = {
          index: '$ctrl.index',
          'focus-on-render': '$ctrl.focusOnRender',
          assessment: '$ctrl.assessment',
          'question-count': '$ctrl.questionCount',
          question: '$ctrl.question',
          response: '$ctrl.response',
          'change-answer': '$ctrl.changeAnswer',
          'can-edit-answer': '$ctrl.canEditAnswer',
          grading: '$ctrl.grading',
        };
        this.$onChanges = ({ index }) => {
          /*
                CBLPROD-14516. When changing questions, force a digest
                loop update that will clear out the question
                This will allow the old question to properly destroy itself
                and remove all listeners (like keyboard shortcuts).
                What was happening is the combination of redux update of
                question -> question loader -> compile directive,
                Either there's a hanging reference so scope never gets destroyed,
                or it just happens too fast for the digest loop.
            */
          if (index && index.currentValue >= 0 && index.currentValue !== index.previousValue) {
            this.template = null;
            $timeout(() => {
              this.template = QuestionViewComponentRegistry.getRenderedTemplate(
                this.question,
                renderArgs
              );
            }, 0);
          }
        };
      },
    ],
  });
