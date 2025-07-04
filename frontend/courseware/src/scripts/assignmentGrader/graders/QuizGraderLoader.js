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

import dayjs from 'dayjs';
import { each, filter, keyBy } from 'lodash';
import { buildScorableAttemptState } from '../../assignmentGrade/models/scoreUtils.js';
import GradebookAPI from '../../services/GradebookAPI.js';
import QuizAPI from '../../services/QuizAPI.js';
import QuizOverviewAPI from '../../services/QuizOverviewAPI.js';
import { ATTEMPT_OPEN } from '../../utilities/attemptStates.js';

export default angular
  .module('lo.assessmentGrader.QuizGraderLoader', [
    GradebookAPI.name,
    QuizAPI.name,
    QuizOverviewAPI.name,
  ])
  .factory('QuizGraderLoader', [
    '$q',
    'GradebookAPI',
    'QuizAPI',
    'QuizOverviewAPI',
    function ($q, GradebookAPI, QuizAPI, QuizOverviewAPI) {
      const service = {};

      service.loadInfo = function (assignmentId) {
        return GradebookAPI.getAssignmentColumn(assignmentId).then(column => {
          return {
            gradebookPointsPossible: column.maximumPoints,
          };
        });
      };

      service.loadUsers = function (assignmentId) {
        return $q
          .all({
            users: QuizOverviewAPI.getStudentSubmissionSummary(assignmentId),
          })
          .then(({ users }) => {
            return users;
          });
      };

      service.formatAttempt = (attempt, userInfo) => {
        userInfo = userInfo || {};

        const scorableAttemptState = buildScorableAttemptState(attempt, userInfo);
        const submitTime = dayjs(attempt.submitTime);
        return {
          ...attempt,
          scorableAttemptState,
          title: submitTime.format('LLL'),
          submitTimestamp: submitTime.valueOf(),
        };
      };

      service.loadAttemptsForUser = function (quizId, userId) {
        return QuizAPI.loadAttempts(quizId, userId).then(attempts =>
          keyBy(
            filter(attempts, att => att.state != ATTEMPT_OPEN),
            'id'
          )
        );
      };

      service.filterRemediationDetails = function (question) {
        let filteredQuestion = {
          ...question,
          rationales: [],
        };
        each(filteredQuestion.choices, choice => (choice.rationales = []));
        return filteredQuestion;
      };

      service.invalidateAttempt = function (attemptId) {
        return QuizAPI.invalidateAttempt(attemptId);
      };

      return service;
    },
  ]);
