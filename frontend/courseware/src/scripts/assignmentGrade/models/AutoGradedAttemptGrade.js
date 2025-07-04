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

import CompositeGrade from './CompositeGrade.js';

/**
 * @ngdoc object
 * @memberOf lo.assessmentGrader
 * @description
 *  Wrapper for an attempt that the instructor can grade
 */
export default angular
  .module('lo.assessmentGrader.AutoGradedAttemptGrade', [CompositeGrade.name])
  .service('AutoGradedAttemptGradeService', function () {
    var service = {};

    service.createConfig = function (attempt, gradebookPointsPossible) {
      var config = {
        displayStyle: 'points',
        pointsAwarded: attempt.score.pointsAwarded,
        pointsPossible: attempt.score.pointsPossible,
        scaledPointsPossible: gradebookPointsPossible,
        releaseStatus: attempt.scorableAttemptState.scorePosted,
      };

      return config;
    };

    return service;
  })
  .factory('AutoGradedAttemptGrade', [
    '$q',
    'AutoGradedAttemptGradeService',
    'CompositeGrade',
    function ($q, AutoGradedAttemptGradeService, CompositeGrade) {
      class AutoGradedAttemptGrade extends CompositeGrade {
        constructor(attempt, gradebookPointsPossible) {
          const config = AutoGradedAttemptGradeService.createConfig(
            attempt,
            gradebookPointsPossible
          );

          super(config);

          this.assessmentId = attempt.assessmentId;
          this.attemptId = attempt.id;
          this.submitDate = dayjs(attempt.submitTime);
          this.title = this.submitDate.format('LLL');
        }

        saveGrade() {
          return $q.when();
        }
      }

      return AutoGradedAttemptGrade;
    },
  ]);
