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

import feedback from '../../../assignmentFeedback/index.js';
import FeedbackManager from '../../../assignmentFeedback/FeedbackManager.js';
import viewCompositeGrade from '../../../assignmentGrade/directives/viewCompositeGrade.js';
import QuestionSubmissionAttemptGrade from '../../../assignmentGrade/models/QuestionSubmissionAttemptGrade.js';
import Rubric from '../../../assignmentGrade/models/Rubric.js';
import { richTextEditor } from '../../../contentEditor/index.js';
import { RESPONSE_SUBMITTED } from '../../../utilities/attemptStates.js';
import { SELECTION_TYPE_ESSAY } from '../../../utilities/questionTypes.js';

import basicQuestionTemplate from '../../questionTemplates/basicQuestionTemplate.js';
import gradingQuestionTemplate from '../../questionTemplates/gradingQuestionTemplate.js';
import template from './essayQuestion.html';
import baseViewTemplate from './essayQuestionBaseView.html';
import baseViewPlayTemplate from './essayQuestionBaseViewPlay.html';
import gradingTemplate from './essayQuestionGradingView.html';
import printViewTemplate from './essayQuestionPrintView.html';

export default angular
  .module('lo.questions.essayQuestion', [
    viewCompositeGrade.name,
    FeedbackManager.name,
    QuestionSubmissionAttemptGrade.name,
    Rubric.name,
    feedback.name,
    richTextEditor.name,
    basicQuestionTemplate.name,
    gradingQuestionTemplate.name,
  ])
  .controller('EssayQuestionCtrl', [
    'FeedbackManager',
    'ViewQuestionSubmissionAttemptGrade',
    'ViewRubric',
    '$scope',
    function (FeedbackManager, ViewQuestionSubmissionAttemptGrade, ViewRubric, $scope) {
      this.$onInit = () => {
        if (this.question.rubric) {
          this.rubric = new ViewRubric(this.question.rubric);
        }
      };

      this.$onChanges = ({ response }) => {
        if (response && response.currentValue) {
          const state = response.currentValue.state;
          this.responseToSelection(response.currentValue);

          if (this.canEditAnswer && response.isFirstChange()) {
            this.watchForFileChanges();
          }

          this.isPendingGrade = state === RESPONSE_SUBMITTED;

          if (this.response && !this.grade) {
            this.grade = new ViewQuestionSubmissionAttemptGrade({
              question: this.question,
              response: this.response,
            });
          }
        }
      };

      this.$onDestroy = () => {
        this.feedbackManager && this.feedbackManager.commitRemovingFiles();
      };

      this.responseToSelection = response => {
        this.textSelection = (response.selection && response.selection.response) || '';
        if (!this.feedbackManager) {
          this.feedbackManager = new FeedbackManager([
            ...(response.attachments || []),
            ...(response.uploads || []),
          ]);
        }
      };

      this.selectionToResponse = (
        html = this.textSelection || '',
        attachments = this.feedbackManager.getAttachedFiles(),
        uploads = this.feedbackManager.getFileInfoInStaging()
      ) => {
        const selection = this.response.selection || {
          responseType: SELECTION_TYPE_ESSAY,
        };
        return {
          ...this.response,
          attachments,
          uploads,
          selection: {
            ...selection,
            response: html,
          },
        };
      };

      this.updateResponseText = html => {
        this.changeAnswer(this.index, this.selectionToResponse(html));
      };

      this.updateUploads = () => {
        this.changeAnswer(this.index, this.selectionToResponse());
      };

      this.watchForFileChanges = () => {
        $scope.$watch(
          () => {
            if (this.feedbackManager.hasInProgressFiles()) {
              return -1;
            } else {
              return this.feedbackManager.files.length;
            }
          },
          (cur, prev) => {
            //this filters out the first change
            if (cur !== prev && cur !== -1) {
              this.updateUploads();
            }
          }
        );
      };
    },
  ])
  .component('essayQuestion', {
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
      grading: '<',
    },
    template,
    controller: [
      '$window',
      function ($window) {
        this.inPrintMode = () => $window.inPrintMode;
      },
    ],
  })
  .component('essayQuestionBaseView', {
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
    controller: function () {},
  })
  .component('essayQuestionBaseViewPlay', {
    bindings: {
      index: '<',
      focusOnRender: '<',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: baseViewPlayTemplate,
    controller: 'EssayQuestionCtrl',
  })
  .component('essayQuestionGradingView', {
    bindings: {
      index: '<',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: gradingTemplate,
    controller: 'EssayQuestionCtrl',
  })
  .component('essayQuestionPrintView', {
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
    controller: 'EssayQuestionCtrl',
  });
