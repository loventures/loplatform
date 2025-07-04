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

import { isEmpty } from 'lodash';
import {
  LEGACY_RESPONSE_SAVED,
  RESPONSE_NOT_SUBMITTED,
  RESPONSE_SUBMITTED,
} from '../../utilities/attemptStates.js';
import {
  QUESTION_TYPE_ESSAY,
  QUESTION_TYPE_FILL_BLANK,
  QUESTION_TYPE_HOTSPOT,
  QUESTION_TYPE_LEGACY_ESSAY,
  QUESTION_TYPE_LEGACY_FILL_BLANK,
  QUESTION_TYPE_LEGACY_HOTSPOT,
  QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE,
  QUESTION_TYPE_LEGACY_TRUE_FALSE,
  QUESTION_TYPE_MULTIPLE_CHOICE,
  QUESTION_TYPE_TRUE_FALSE,
} from '../../utilities/questionTypes.js';

import { questionCompetenciesComponent } from '../questionAddons/QuestionCompetencies.js';
import questionGradingStrategy from '../questionAddons/questionGradingStrategy.js';
import questionLevelRemediation from '../questionAddons/questionLevelRemediation.js';
import questionResourceRemediation from '../questionAddons/questionResourceRemediation.js';
import questionScore from '../questionAddons/questionScore.js';
import template from './basicQuestionTemplate.html';
import printTemplate from './printQuestionTemplate.html';

//This until we figure out a non-spammy and graceful solution to display it
const getQuestionTypeHasPartialCredit = questionType => {
  return (
    [
      QUESTION_TYPE_MULTIPLE_CHOICE,
      QUESTION_TYPE_TRUE_FALSE,
      QUESTION_TYPE_HOTSPOT,
      QUESTION_TYPE_ESSAY,
      QUESTION_TYPE_FILL_BLANK,
      QUESTION_TYPE_LEGACY_MULTIPLE_CHOICE,
      QUESTION_TYPE_LEGACY_TRUE_FALSE,
      QUESTION_TYPE_LEGACY_HOTSPOT,
      QUESTION_TYPE_LEGACY_ESSAY,
      QUESTION_TYPE_LEGACY_FILL_BLANK,
    ].indexOf(questionType) === -1
  );
};

const basicQuestionTemplateController = [
  'Settings',
  '$translate',
  function (Settings, $translate) {
    this.showQuestionPoints = Settings.isFeatureEnabled('ShowPoints');

    this.$onInit = () => {
      this.questionTypeHasPartialCredit = getQuestionTypeHasPartialCredit(this.question._type);
      this.isCheckpoint = this.assessment?.settings.isCheckpoint;
      this.hideQuestionNumber = this.questionCount === 1;
      this.showCompetencies = this.response?.state !== RESPONSE_NOT_SUBMITTED;
    };

    this.$onChanges = ({ response }) => {
      if (response && response.currentValue) {
        this.showCompetencies = response.currentValue.state !== RESPONSE_NOT_SUBMITTED;
        this.isAnswered =
          response.currentValue.state === LEGACY_RESPONSE_SAVED ||
          response.currentValue.state === RESPONSE_SUBMITTED ||
          !!response.currentValue.selection;
        const score = response.currentValue.score;
        this.hasScore = !isEmpty(score);
        this.isCorrect = score && score.pointsAwarded >= score.pointsPossible;

        const t9nArgs = {
          questionNumber: this.index + 1,
          questionType: $translate.instant(this.question._type),
        };
        if (this.isCorrect) {
          this.questionAriaLabel = $translate.instant(
            'QUIZ_PLAYER_QUESTION_LABEL_CORRECT',
            t9nArgs
          );
        } else if (this.hasScore) {
          this.questionAriaLabel = $translate.instant(
            'QUIZ_PLAYER_QUESTION_LABEL_INCORRECT',
            t9nArgs
          );
        } else {
          this.questionAriaLabel = $translate.instant('QUIZ_PLAYER_QUESTION_LABEL', t9nArgs);
        }
      }
    };
  },
];

export default angular
  .module('lo.questions.basicQuestionTemplate', [
    questionGradingStrategy.name,
    questionLevelRemediation.name,
    questionResourceRemediation.name,
    questionCompetenciesComponent.name,
    questionScore.name,
  ])
  .component('basicQuestionTemplate', {
    template,
    bindings: {
      index: '<',
      question: '<',
      response: '<',
      attempt: '<?',
      assessment: '<?',
      questionCount: '<?',
    },
    transclude: {
      questionContentSlot: '?questionContentSlot',
      questionTextSlot: '?questionTextSlot',
    },
    controller: basicQuestionTemplateController,
  })
  .component('printQuestionTemplate', {
    template: printTemplate,
    bindings: {
      index: '<',
      question: '<',
      response: '<',
      attempt: '<?',
      assessment: '<?',
      questionCount: '<?',
    },
    transclude: {
      questionContentSlot: '?questionContentSlot',
      questionTextSlot: '?questionTextSlot',
    },
    controller: basicQuestionTemplateController,
  });
