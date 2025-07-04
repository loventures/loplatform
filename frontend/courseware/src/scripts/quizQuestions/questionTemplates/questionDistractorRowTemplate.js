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

import { find, isEmpty } from 'lodash';

import choiceCorrectness from '../questionAddons/choiceCorrectness.js';
import choiceLevelRemediation from '../questionAddons/choiceLevelRemediation.js';
import printTemplate from './printQuestionDistractorRowTemplate.html';
import template from './questionDistractorRowTemplate.html';

const isBlank = reason =>
  isEmpty(reason) || reason.match(/^\s*<p[^>]*>(?:\s*<br[^>]*>)?\s*<\/p>\s*$/);

const questionDistractorRowTemplateController = [
  '$uibModal',
  function ($uibModal) {
    this.updateRemediationDisplay = () => {
      const feedback = this.choice.correct
        ? find(this.choice.rationales, ({ _type }) => _type === 'rationale')
        : find(this.choice.rationales, ({ _type }) => _type === 'textRemediation');
      const feedbackReason = !isBlank(feedback?.reason) ? feedback.reason : undefined;
      const hasAttempt = !this.isInstructor; // what a name

      this.choiceFeedback = feedbackReason;

      //Students see feedback for choices they selected, and instructors/advisors can see feedback on content review
      const showChoiceLevelRemediation = feedbackReason && (this.isSelected || !hasAttempt);

      // We also want to show "remediation" aka "this was the correct choice" on the correct choices that you
      // did not make and have no actual remediation content. Truthfully I do not understand hasCorrectness.
      this.showInlineRemediation =
        showChoiceLevelRemediation ||
        (hasAttempt && this.hasCorrectness && !this.isSelected && this.choice.correct);

      this.showModalRemediation = false; // TODO kill, unused

      this.viewRemediationInModal = () => {
        $uibModal.open({
          component: 'choice-level-remediation-modal',
          resolve: {
            remediation: () => this.choiceFeedback,
            isChoiceCorrect: () => this.choice.correct,
          },
        });
      };
    };

    this.$onChanges = ({ isSelected, hasCorrectness }) => {
      if (
        (isSelected && isSelected.currentValue) ||
        (hasCorrectness && hasCorrectness.currentValue)
      ) {
        this.updateRemediationDisplay();
      }
    };

    this.updateRemediationDisplay();
  },
];

export default angular
  .module('lo.questions.questionDistractorRowTemplate', [
    choiceCorrectness.name,
    choiceLevelRemediation.name,
  ])
  .component('questionDistractorRowTemplate', {
    bindings: {
      choice: '<',
      hasCorrectness: '<',
      isCorrect: '<',
      isSelected: '<',
      isInstructor: '<', // this is actually we're viewing the quiz, not an attempt
      isMulti: '<',
      index: '<',
    },
    template,
    transclude: {
      distractorSlot: 'distractorSlot',
    },
    controller: questionDistractorRowTemplateController,
  })
  .component('printQuestionDistractorRowTemplate', {
    bindings: {
      choice: '<',
      hasCorrectness: '<',
      isCorrect: '<',
      isSelected: '<',
    },
    template: printTemplate,
    transclude: {
      distractorSlot: 'distractorSlot',
    },
    controller: questionDistractorRowTemplateController,
  });
