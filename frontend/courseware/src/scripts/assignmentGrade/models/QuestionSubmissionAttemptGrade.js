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

import { RESPONSE_SCORED, RESPONSE_SCORED_RELEASED } from '../../utilities/attemptStates.js';

import {
  buildBasicScore,
  buildRubricScore,
  buildAllFeedback,
  getBasicResponseFeedback,
  getRubricResponse,
} from './scoreUtils.js';

import CompositeGrade from './CompositeGrade.js';
import QuizAPI from '../../services/QuizAPI.js';

/**
 * @ngdoc object
 * @alias QuestionSubmissionAttemptGrade
 * @memberOf lo.assignmentGrade
 * @description
 *   Wrapper for an submission assignment attempt grade that may or may not based off of a rubric
 */
export default angular
  .module('lo.assignmentGrade.QuestionSubmissionAttemptGrade', [CompositeGrade.name, QuizAPI.name])
  .factory('QuestionSubmissionAttemptGradeService', function () {
    var service = {};

    service.createConfig = function (attempt, { question, response }, gradebookPointsPossible) {
      const score = response.score || {};
      const feedback = response.instructorFeedback || [];
      const attachmentInfos = attempt.attachments || [];
      const rubric = question.rubric;

      const basicFeedback = getBasicResponseFeedback({
        feedback,
        attachmentInfos,
      });

      var config = {
        displayStyle: 'points',
        pointsPossible: question.pointsPossible,
        scaledPointsPossible: gradebookPointsPossible,
        rubric: rubric,

        rubricResponse: getRubricResponse({ score, feedback }, rubric),
        pointsAwarded: score.pointsAwarded,
        feedback: basicFeedback.comment || '',
        attachments: basicFeedback.attachments || [],
        releaseStatus:
          response.state === RESPONSE_SCORED_RELEASED || response.state === RESPONSE_SCORED,
        isBlankGrade: isEmpty(score),
      };

      return config;
    };

    return service;
  })
  .factory('QuestionSubmissionAttemptGrade', [
    'CompositeGrade',
    'QuestionSubmissionAttemptGradeService',
    'QuizAPI',
    '$q',
    function (CompositeGrade, QuestionSubmissionAttemptGradeService, QuizAPI, $q) {
      var QuestionSubmissionAttemptGrade = function (attempt, question, gradebookPointsPossible) {
        var config = QuestionSubmissionAttemptGradeService.createConfig(
          attempt,
          question,
          gradebookPointsPossible
        );

        CompositeGrade.call(this, config);
        this.attemptId = attempt.id;
        this.questionIndex = question.index;
      };

      QuestionSubmissionAttemptGrade.prototype = Object.create(CompositeGrade.prototype);
      QuestionSubmissionAttemptGrade.prototype.constructor = QuestionSubmissionAttemptGrade;

      QuestionSubmissionAttemptGrade.prototype.scoreToJSON = function (submit) {
        const score = this.rubric
          ? buildRubricScore(this.pointsPossible, this.outgoing.rubric)
          : buildBasicScore(this.pointsPossible, this.outgoing.pointsAwarded);

        return {
          questionIndex: this.questionIndex,
          submit,
          score,
        };
      };

      QuestionSubmissionAttemptGrade.prototype.feedbackToJSON = function (submit) {
        const { feedback, feedbackManager, rubric } = this.outgoing;
        const values = buildAllFeedback(feedback, feedbackManager, rubric);

        return {
          questionIndex: this.questionIndex,
          submit,
          values,
        };
      };

      QuestionSubmissionAttemptGrade.prototype.saveGrade = function (isReleasing) {
        return QuizAPI.saveAttemptScore(this.attemptId, this.scoreToJSON(isReleasing)).then(() => {
          return QuizAPI.saveAttemptFeedback(this.attemptId, this.feedbackToJSON(isReleasing));
        });
      };

      QuestionSubmissionAttemptGrade.prototype.syncAttachments = function (updatedAttempt) {
        return $q.when(updatedAttempt);
      };

      return QuestionSubmissionAttemptGrade;
    },
  ])
  .factory('ViewQuestionSubmissionAttemptGrade', [
    'QuestionSubmissionAttemptGradeService',
    'ViewCompositeGrade',
    function (QuestionSubmissionAttemptGradeService, ViewCompositeGrade) {
      var ViewQuestionSubmissionAttemptGrade = function (questionTuple) {
        var config = QuestionSubmissionAttemptGradeService.createConfig({}, questionTuple);

        ViewCompositeGrade.call(this, config);
      };

      ViewQuestionSubmissionAttemptGrade.prototype = Object.create(ViewCompositeGrade.prototype);
      ViewQuestionSubmissionAttemptGrade.prototype.constructor = ViewQuestionSubmissionAttemptGrade;

      return ViewQuestionSubmissionAttemptGrade;
    },
  ]);
