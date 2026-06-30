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
import { first, values } from 'lodash';

import { buildScorableAttemptState } from '../../../assignmentGrade/models/scoreUtils.ts';
import { gradebookAPI as GradebookAPI } from '../../../services/gradebookAPI.ts';
import { quizOverviewAPI as QuizOverviewAPI } from '../../../services/quizOverviewAPI.ts';
import { submissionActivityAPI as SubmissionActivityAPI } from '../../../services/submissionActivityAPI.ts';
import { ATTEMPT_OPEN } from '../../../utilities/attemptStates.js';

/**
 * Pure-TS port of the Angular `lo.assessmentGrader.SubmissionGraderLoader` factory
 * (graders/SubmissionGraderLoader.js): the data loader the `SubmissionGrader` delegates its API calls to.
 *
 * Lift-and-shift of the plain service object — the pure API singletons (gradebookAPI / quizOverviewAPI /
 * submissionActivityAPI) are imported directly. `SubmissionGraderLoader.js` keeps a thin `.factory`
 * adapter with the SAME service name so the still-Angular grader module deps keep resolving.
 */
export const submissionGraderLoader = {
  loadInfo(assignmentId: any) {
    return GradebookAPI.getAssignmentColumn(assignmentId).then((column: any) => {
      return {
        gradebookPointsPossible: column.maximumPoints,
      };
    });
  },

  loadUsers(assignmentId: any) {
    return QuizOverviewAPI.getStudentSubmissionSummary(assignmentId);
  },

  formatAttempt(attempt: any, userInfo?: any) {
    userInfo = userInfo || {};

    const scorableAttemptState = buildScorableAttemptState(attempt, userInfo);

    const displayDate =
      attempt.state === ATTEMPT_OPEN ? dayjs(attempt.createTime) : dayjs(attempt.submitTime);

    return {
      ...attempt,
      scorableAttemptState,
      title: displayDate.format('LLL'),
      submitTimestamp: displayDate.valueOf(),
    };
  },

  startAttempt(assignmentId: any, userId: any) {
    return SubmissionActivityAPI.createAttempt(assignmentId, userId).then(({ attempts }: any) =>
      submissionGraderLoader.formatAttempt(first(values(attempts)))
    );
  },

  invalidateAttempt(attemptId: any) {
    return SubmissionActivityAPI.invalidateAttempt(attemptId);
  },

  loadAttemptsForUser(assignmentId: any, userId: any) {
    return SubmissionActivityAPI.loadSubmissionAttempts(assignmentId, userId);
  },
};

export default submissionGraderLoader;
