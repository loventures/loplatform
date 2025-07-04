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
import { first, values } from 'lodash';
import { buildScorableAttemptState } from '../../assignmentGrade/models/scoreUtils.js';
import GradebookAPI from '../../services/GradebookAPI.js';
import QuizOverviewAPI from '../../services/QuizOverviewAPI.js';
import SubmissionActivityAPI from '../../services/SubmissionActivityAPI.js';
import { ATTEMPT_OPEN } from '../../utilities/attemptStates.js';

export default angular
  .module('lo.assessmentGrader.SubmissionGraderLoader', [
    GradebookAPI.name,
    SubmissionActivityAPI.name,
    QuizOverviewAPI.name,
  ])
  .factory('SubmissionGraderLoader', [
    'GradebookAPI',
    'SubmissionActivityAPI',
    'QuizOverviewAPI',
    function (GradebookAPI, SubmissionActivityAPI, QuizOverviewAPI) {
      const service = {};

      service.loadInfo = function (assignmentId) {
        return GradebookAPI.getAssignmentColumn(assignmentId).then(column => {
          return {
            gradebookPointsPossible: column.maximumPoints,
          };
        });
      };

      service.loadUsers = function (assignmentId) {
        return QuizOverviewAPI.getStudentSubmissionSummary(assignmentId);
      };

      service.formatAttempt = (attempt, userInfo) => {
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
      };

      service.startAttempt = (assignmentId, userId) => {
        return SubmissionActivityAPI.createAttempt(assignmentId, userId).then(({ attempts }) =>
          service.formatAttempt(first(values(attempts)))
        );
      };

      service.invalidateAttempt = attemptId => {
        return SubmissionActivityAPI.invalidateAttempt(attemptId);
      };

      service.loadAttemptsForUser = (assignmentId, userId) => {
        return SubmissionActivityAPI.loadSubmissionAttempts(assignmentId, userId);
      };

      return service;
    },
  ]);
