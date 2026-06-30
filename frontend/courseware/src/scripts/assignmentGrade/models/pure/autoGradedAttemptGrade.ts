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

import dayjs from 'dayjs';

import { nativeQ as $q } from '../../../utilities/nativeQ.ts';
import { CompositeGrade } from './compositeGrade.ts';

/**
 * Pure-TS port of the Angular `lo.assessmentGrader.AutoGradedAttemptGrade` module
 * (models/AutoGradedAttemptGrade.js): the wrapper for an auto-graded attempt the instructor reviews.
 *
 * Lift-and-shift of the constructor-function + prototype model so it prototype-chains against the
 * pure constructor-function `CompositeGrade` base. The GraderProvider's 150ms poll reads mutable
 * state on these objects (the `isDirty()` / `outgoing.*` surface inherited from CompositeGrade); the
 * fields and methods are preserved verbatim. Do NOT make anything immutable.
 *
 * `AutoGradedAttemptGrade.js` keeps thin `.factory` adapters over these with the SAME service names
 * (`AutoGradedAttemptGradeService`, `AutoGradedAttemptGrade`) so the still-Angular QuizGrader keeps
 * resolving.
 */
export const autoGradedAttemptGradeService = {
  createConfig: function (attempt: any, gradebookPointsPossible: any) {
    var config = {
      displayStyle: 'points',
      pointsAwarded: attempt.score.pointsAwarded,
      pointsPossible: attempt.score.pointsPossible,
      scaledPointsPossible: gradebookPointsPossible,
      releaseStatus: attempt.scorableAttemptState.scorePosted,
    };

    return config;
  },
};

export const AutoGradedAttemptGrade = function (
  this: any,
  attempt: any,
  gradebookPointsPossible: any
) {
  const config = autoGradedAttemptGradeService.createConfig(attempt, gradebookPointsPossible);

  CompositeGrade.call(this, config);

  this.assessmentId = attempt.assessmentId;
  this.attemptId = attempt.id;
  this.submitDate = dayjs(attempt.submitTime);
  this.title = this.submitDate.format('LLL');
} as any;

AutoGradedAttemptGrade.prototype = Object.create(CompositeGrade.prototype);
AutoGradedAttemptGrade.prototype.constructor = AutoGradedAttemptGrade;

AutoGradedAttemptGrade.prototype.saveGrade = function () {
  return $q.when();
};
