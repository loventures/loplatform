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

import { quizAPI as QuizAPI } from '../../../services/quizAPI.ts';
import { RESPONSE_SCORED, RESPONSE_SCORED_RELEASED } from '../../../utilities/attemptStates.js';
import { nativeQ as $q } from '../../../utilities/nativeQ.ts';
import { CompositeGrade, ViewCompositeGrade } from './compositeGrade.ts';
import {
  buildAllFeedback,
  buildBasicScore,
  buildRubricScore,
  getBasicResponseFeedback,
  getRubricResponse,
} from '../scoreUtils.ts';

/**
 * Pure-TS port of the Angular `lo.assignmentGrade.QuestionSubmissionAttemptGrade` module
 * (models/QuestionSubmissionAttemptGrade.js): the wrapper for a question-submission attempt grade
 * that may or may not be based off a rubric, plus its read-only `ViewQuestionSubmissionAttemptGrade`.
 *
 * Lift-and-shift of the constructor-function + prototype models so they prototype-chain against the
 * pure constructor-function `CompositeGrade` / `ViewCompositeGrade` bases. The GraderProvider's 150ms
 * poll reads mutable state on the gradeable variant (`isDirty()` / `outgoing.*` inherited from
 * CompositeGrade, plus the `attemptId` / `questionIndex` fields); they are preserved verbatim. Do NOT
 * make anything immutable.
 *
 * The native (axios) `quizAPI` and `nativeQ` singletons are imported directly.
 * `QuestionSubmissionAttemptGrade.js` keeps thin `.factory` adapters over these with the SAME service
 * names (`QuestionSubmissionAttemptGradeService`, `QuestionSubmissionAttemptGrade`,
 * `ViewQuestionSubmissionAttemptGrade`) so the still-Angular QuizGrader keeps resolving;
 * `EssayQuestionBaseView` / `EssayQuestionPrintView` import the View constructor directly.
 */
export const questionSubmissionAttemptGradeService = {
  createConfig: function (
    attempt: any,
    { question, response }: any,
    gradebookPointsPossible?: any
  ) {
    const score = response.score || {};
    const feedback = response.instructorFeedback || [];
    const attachmentInfos = attempt.attachments || [];
    const rubric = question.rubric;

    const basicFeedback = getBasicResponseFeedback({
      feedback,
      attachmentInfos,
    } as any);

    var config = {
      displayStyle: 'points',
      pointsPossible: question.pointsPossible,
      scaledPointsPossible: gradebookPointsPossible,
      rubric: rubric,

      rubricResponse: getRubricResponse({ score, feedback } as any, rubric),
      pointsAwarded: score.pointsAwarded,
      feedback: basicFeedback.comment || '',
      attachments: basicFeedback.attachments || [],
      releaseStatus:
        response.state === RESPONSE_SCORED_RELEASED || response.state === RESPONSE_SCORED,
      isBlankGrade: isEmpty(score),
    };

    return config;
  },
};

export const QuestionSubmissionAttemptGrade = function (
  this: any,
  attempt: any,
  question: any,
  gradebookPointsPossible: any
) {
  var config = questionSubmissionAttemptGradeService.createConfig(
    attempt,
    question,
    gradebookPointsPossible
  );

  CompositeGrade.call(this, config);
  this.attemptId = attempt.id;
  this.questionIndex = question.index;
} as any;

QuestionSubmissionAttemptGrade.prototype = Object.create(CompositeGrade.prototype);
QuestionSubmissionAttemptGrade.prototype.constructor = QuestionSubmissionAttemptGrade;

QuestionSubmissionAttemptGrade.prototype.scoreToJSON = function (submit: any) {
  const score = this.rubric
    ? buildRubricScore(this.pointsPossible, this.outgoing.rubric)
    : buildBasicScore(this.pointsPossible, this.outgoing.pointsAwarded);

  return {
    questionIndex: this.questionIndex,
    submit,
    score,
  };
};

QuestionSubmissionAttemptGrade.prototype.feedbackToJSON = function (submit: any) {
  const { feedback, feedbackManager, rubric } = this.outgoing;
  const values = buildAllFeedback(feedback, feedbackManager, rubric);

  return {
    questionIndex: this.questionIndex,
    submit,
    values,
  };
};

QuestionSubmissionAttemptGrade.prototype.saveGrade = function (isReleasing: any) {
  return QuizAPI.saveAttemptScore(this.attemptId, this.scoreToJSON(isReleasing)).then(() => {
    return QuizAPI.saveAttemptFeedback(this.attemptId, this.feedbackToJSON(isReleasing));
  });
};

QuestionSubmissionAttemptGrade.prototype.syncAttachments = function (updatedAttempt: any) {
  return $q.when(updatedAttempt);
};

export const ViewQuestionSubmissionAttemptGrade = function (this: any, questionTuple: any) {
  var config = questionSubmissionAttemptGradeService.createConfig({}, questionTuple);

  ViewCompositeGrade.call(this, config);
} as any;

ViewQuestionSubmissionAttemptGrade.prototype = Object.create(ViewCompositeGrade.prototype);
ViewQuestionSubmissionAttemptGrade.prototype.constructor = ViewQuestionSubmissionAttemptGrade;
