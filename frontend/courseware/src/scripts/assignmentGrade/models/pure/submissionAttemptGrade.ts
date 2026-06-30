/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { loConfig } from '../../../bootstrap/loConfig.ts';
import { submissionActivityAPI as SubmissionActivityAPI } from '../../../services/submissionActivityAPI.ts';
import { ATTEMPT_FINALIZED, ATTEMPT_OPEN } from '../../../utilities/attemptStates.js';
import { nativeQ as $q } from '../../../utilities/nativeQ.ts';
import UrlBuilder from '../../../utilities/UrlBuilder.ts';
import { FeedbackManager } from '../../../assignmentFeedback/FeedbackManager.js';
import { CompositeGrade } from './compositeGrade.ts';
import {
  buildAllFeedback,
  buildBasicScore,
  buildRubricScore,
  getBasicResponseFeedback,
  getRubricResponse,
} from '../scoreUtils.ts';

/**
 * Pure-TS port of the Angular `lo.assignmentGrade.SubmissionAttemptGrade` module
 * (models/SubmissionAttemptGrade.js): the wrapper for a submission-assignment attempt grade that may
 * or may not be based off a rubric.
 *
 * Lift-and-shift of the constructor-function + prototype model so it prototype-chains against the
 * pure constructor-function `CompositeGrade` base. The GraderProvider's 150ms poll reads mutable
 * state on these objects (`isDirty()` / `outgoing.*` inherited from CompositeGrade, plus the
 * `attemptId` / `isAttemptOpen` fields mutated during grading); they are preserved verbatim. Do NOT
 * make anything immutable.
 *
 * The pure singletons are imported directly: the native (axios) `submissionActivityAPI` and
 * `nativeQ` (which the pure CompositeGrade base already uses). `SubmissionAttemptGrade.js` keeps thin
 * `.factory` adapters over these with the SAME service names (`SubmissionAttemptGradeService`,
 * `SubmissionAttemptGrade`) so the still-Angular SubmissionGrader keeps resolving.
 */
export const submissionAttemptGradeService = {
  createAttachmentUrl: function (attemptId: any) {
    if (!attemptId) {
      return '';
    }

    return new (UrlBuilder as any)(loConfig.submissionAssessmentAttempt.attachment, {
      attemptId,
    });
  },

  createConfig: function (attempt: any, rubric: any, gradebookPointsPossible: any) {
    const score = attempt.score || {};
    const basicFeedback = getBasicResponseFeedback(attempt);

    var config = {
      displayStyle: 'points',
      pointsPossible: score.pointsPossible || gradebookPointsPossible,
      scaledPointsPossible: gradebookPointsPossible,
      rubric: rubric,

      attachmentUrl: submissionAttemptGradeService.createAttachmentUrl(attempt.id),

      rubricResponse: getRubricResponse(attempt, rubric),
      pointsAwarded: score.pointsAwarded,
      feedback: basicFeedback.comment,
      attachments: basicFeedback.attachments,
      releaseStatus: attempt.state === ATTEMPT_FINALIZED,
      isBlankGrade: isEmpty(score),
    };

    return config;
  },
};

export const SubmissionAttemptGrade = function (
  this: any,
  attempt: any,
  rubric: any,
  gradebookPointsPossible: any
) {
  var config = submissionAttemptGradeService.createConfig(attempt, rubric, gradebookPointsPossible);

  CompositeGrade.call(this, config);
  this.attemptId = attempt.id;
  this.isAttemptOpen = attempt.state === ATTEMPT_OPEN;
} as any;

SubmissionAttemptGrade.prototype = Object.create(CompositeGrade.prototype);
SubmissionAttemptGrade.prototype.constructor = SubmissionAttemptGrade;

SubmissionAttemptGrade.prototype.scoreToJSON = function (submit: any) {
  const score = this.rubric
    ? buildRubricScore(this.pointsPossible, this.outgoing.rubric)
    : buildBasicScore(this.pointsPossible, this.outgoing.pointsAwarded);

  return {
    submit,
    score,
  };
};

SubmissionAttemptGrade.prototype.feedbackToJSON = function (submit: any) {
  const { feedback, feedbackManager, rubric } = this.outgoing;
  const values = buildAllFeedback(feedback, feedbackManager, rubric);

  return {
    submit,
    values,
  };
};

SubmissionAttemptGrade.prototype.doSaveGrade = function (isReleasing: any) {
  return SubmissionActivityAPI.saveAttemptScore(this.attemptId, this.scoreToJSON(isReleasing)).then(
    () => {
      return SubmissionActivityAPI.saveAttemptFeedback(
        this.attemptId,
        this.feedbackToJSON(isReleasing)
      );
    }
  );
};

SubmissionAttemptGrade.prototype.saveGrade = function (isReleasing: any) {
  if (this.isAttemptOpen) {
    return SubmissionActivityAPI.submitAttempt({ id: this.attemptId }).then(() => {
      this.isAttemptOpen = false;
      return this.doSaveGrade(isReleasing);
    });
  } else {
    return this.doSaveGrade(isReleasing);
  }
};

SubmissionAttemptGrade.prototype.syncAttachments = function (updatedAttempt: any) {
  const basicFeedback = getBasicResponseFeedback(updatedAttempt);
  this.initial.attachments = basicFeedback.attachments;
  this.outgoing.feedbackManager = new (FeedbackManager as any)(basicFeedback.attachments);
  return $q.when(updatedAttempt);
};
