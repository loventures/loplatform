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

import { ATTEMPT_OPEN, ATTEMPT_FINALIZED } from '../../utilities/attemptStates.js';

import {
  buildBasicScore,
  buildRubricScore,
  buildAllFeedback,
  getBasicResponseFeedback,
  getRubricResponse,
} from './scoreUtils.js';

import { loConfig } from '../../bootstrap/loConfig.js';
import CompositeGrade from './CompositeGrade.js';
import SubmissionActivityAPI from '../../services/SubmissionActivityAPI.js';
import FeedbackManager from '../../assignmentFeedback/FeedbackManager.js';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/**
 * @ngdoc object
 * @alias SubmissionAttemptGrade
 * @memberOf lo.assignmentGrade
 * @description
 *   Wrapper for an submission assignment attempt grade that may or may not based off of a rubric
 */
export default angular
  .module('lo.assignmentGrade.SubmissionAttemptGrade', [
    CompositeGrade.name,
    SubmissionActivityAPI.name,
    FeedbackManager.name,
  ])
  .factory('SubmissionAttemptGradeService', function () {
    var service = {};

    service.createAttachmentUrl = function (attemptId) {
      if (!attemptId) {
        return '';
      }

      return new UrlBuilder(loConfig.submissionAssessmentAttempt.attachment, {
        attemptId,
      });
    };

    service.createConfig = function (attempt, rubric, gradebookPointsPossible) {
      const score = attempt.score || {};
      const basicFeedback = getBasicResponseFeedback(attempt);

      var config = {
        displayStyle: 'points',
        pointsPossible: score.pointsPossible || gradebookPointsPossible,
        scaledPointsPossible: gradebookPointsPossible,
        rubric: rubric,

        attachmentUrl: service.createAttachmentUrl(attempt.id),

        rubricResponse: getRubricResponse(attempt, rubric),
        pointsAwarded: score.pointsAwarded,
        feedback: basicFeedback.comment,
        attachments: basicFeedback.attachments,
        releaseStatus: attempt.state === ATTEMPT_FINALIZED,
        isBlankGrade: isEmpty(score),
      };

      return config;
    };

    return service;
  })
  .factory('SubmissionAttemptGrade', [
    'CompositeGrade',
    'SubmissionAttemptGradeService',
    'SubmissionActivityAPI',
    'FeedbackManager',
    '$q',
    function (
      CompositeGrade,
      SubmissionAttemptGradeService,
      SubmissionActivityAPI,
      FeedbackManager,
      $q
    ) {
      var SubmissionAttemptGrade = function (attempt, rubric, gradebookPointsPossible) {
        var config = SubmissionAttemptGradeService.createConfig(
          attempt,
          rubric,
          gradebookPointsPossible
        );

        CompositeGrade.call(this, config);
        this.attemptId = attempt.id;
        this.isAttemptOpen = attempt.state === ATTEMPT_OPEN;
      };

      SubmissionAttemptGrade.prototype = Object.create(CompositeGrade.prototype);
      SubmissionAttemptGrade.prototype.constructor = SubmissionAttemptGrade;

      SubmissionAttemptGrade.prototype.scoreToJSON = function (submit) {
        const score = this.rubric
          ? buildRubricScore(this.pointsPossible, this.outgoing.rubric)
          : buildBasicScore(this.pointsPossible, this.outgoing.pointsAwarded);

        return {
          submit,
          score,
        };
      };

      SubmissionAttemptGrade.prototype.feedbackToJSON = function (submit) {
        const { feedback, feedbackManager, rubric } = this.outgoing;
        const values = buildAllFeedback(feedback, feedbackManager, rubric);

        return {
          submit,
          values,
        };
      };

      SubmissionAttemptGrade.prototype.doSaveGrade = function (isReleasing) {
        return SubmissionActivityAPI.saveAttemptScore(
          this.attemptId,
          this.scoreToJSON(isReleasing)
        ).then(() => {
          return SubmissionActivityAPI.saveAttemptFeedback(
            this.attemptId,
            this.feedbackToJSON(isReleasing)
          );
        });
      };

      SubmissionAttemptGrade.prototype.saveGrade = function (isReleasing) {
        if (this.isAttemptOpen) {
          return SubmissionActivityAPI.submitAttempt({ id: this.attemptId }).then(() => {
            this.isAttemptOpen = false;
            return this.doSaveGrade(isReleasing);
          });
        } else {
          return this.doSaveGrade(isReleasing);
        }
      };

      SubmissionAttemptGrade.prototype.syncAttachments = function (updatedAttempt) {
        const basicFeedback = getBasicResponseFeedback(updatedAttempt);
        this.initial.attachments = basicFeedback.attachments;
        this.outgoing.feedbackManager = new FeedbackManager(basicFeedback.attachments);
        return $q.when(updatedAttempt);
      };

      return SubmissionAttemptGrade;
    },
  ]);
