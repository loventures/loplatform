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
import { each, filter, keyBy } from 'lodash';

import { buildScorableAttemptState } from '../../../assignmentGrade/models/scoreUtils.ts';
import { gradebookAPI as GradebookAPI } from '../../../services/gradebookAPI.ts';
import { quizAPI as QuizAPI } from '../../../services/quizAPI.ts';
import { quizOverviewAPI as QuizOverviewAPI } from '../../../services/quizOverviewAPI.ts';
import { ATTEMPT_OPEN } from '../../../utilities/attemptStates.js';

/**
 * Pure-TS port of the Angular `lo.assessmentGrader.QuizGraderLoader` factory (graders/QuizGraderLoader.js):
 * the data loader the `QuizGrader` delegates its API calls to.
 *
 * Lift-and-shift of the plain service object — the pure API singletons (gradebookAPI / quizAPI /
 * quizOverviewAPI) are imported directly, and `$q.all` over an object map becomes `Promise.all` over its
 * values (re-keyed). `QuizGraderLoader.js` keeps a thin `.factory` adapter with the SAME service name so
 * the still-Angular grader module deps keep resolving.
 */
export const quizGraderLoader = {
  loadInfo(assignmentId: any) {
    return GradebookAPI.getAssignmentColumn(assignmentId).then((column: any) => {
      return {
        gradebookPointsPossible: column.maximumPoints,
      };
    });
  },

  loadUsers(assignmentId: any) {
    return QuizOverviewAPI.getStudentSubmissionSummary(assignmentId).then((users: any) => {
      return users;
    });
  },

  formatAttempt(attempt: any, userInfo?: any) {
    userInfo = userInfo || {};

    const scorableAttemptState = buildScorableAttemptState(attempt, userInfo);
    const submitTime = dayjs(attempt.submitTime);
    return {
      ...attempt,
      scorableAttemptState,
      title: submitTime.format('LLL'),
      submitTimestamp: submitTime.valueOf(),
    };
  },

  loadAttemptsForUser(quizId: any, userId: any) {
    return QuizAPI.loadAttempts(quizId, userId).then((attempts: any) =>
      keyBy(
        filter(attempts, (att: any) => att.state != ATTEMPT_OPEN),
        'id'
      )
    );
  },

  filterRemediationDetails(question: any) {
    let filteredQuestion = {
      ...question,
      rationales: [],
    };
    each(filteredQuestion.choices, (choice: any) => (choice.rationales = []));
    return filteredQuestion;
  },

  invalidateAttempt(attemptId: any) {
    return QuizAPI.invalidateAttempt(attemptId);
  },
};

export default quizGraderLoader;
