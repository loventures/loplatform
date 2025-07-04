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

import { get } from 'lodash';

import template from './questionGradingStrategy.html';

const scoringMessages = {
  allOrNothing: 'NO_PARTIAL_CREDIT_GRADING_STRATEGY',
  allowPartialCredit: 'PARTIAL_CREDIT_GRADING_STRATEGY',
  fullCreditForAnyCorrectChoice: 'FULL_CREDIT_FOR_ANY_CORRECT_CHOICE_GRADING_STRATEGY',
};

const matchingMessages = {
  allOrNothing: 'NO_PARTIAL_CREDIT_GRADING_STRATEGY_MATCHING',
  allowPartialCredit: 'PARTIAL_CREDIT_GRADING_STRATEGY_MATCHING',
};

const scoringClassName = {
  allOrNothing: 'deny-partial-credit',
  allowPartialCredit: 'allow-partial-credit',
  fullCreditForAnyCorrectChoice: 'full-credit-any-correct',
};

export default angular
  .module('lo.questions.addons.questionGradingStrategy', [])
  .component('questionGradingStrategy', {
    template,
    bindings: {
      question: '<',
    },
    controller: function () {
      this.scoringOption = get(this.question, 'scoringOption', 'allOrNothing');
      this.msg =
        this.question._type === 'matching'
          ? matchingMessages[this.scoringOption]
          : scoringMessages[this.scoringOption];
      this.className = scoringClassName[this.scoringOption];
    },
  });
