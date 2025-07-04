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

import { map } from 'lodash';
import { getUserFullName } from '../utilities/getUserFullName.js';

import Request from '../utilities/Request.js';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';

/**
 * @ngdoc service
 * @alias QuizOverviewAPI
 * @memberof lo.services
 * @description quiz overview API
 */
export default angular
  .module('lo.services.QuizOverviewAPI', [Request.name])
  .service('QuizOverviewAPI', [
    'Request',
    function QuizAPI(Request) {
      const service = {};

      const buildUserInfo = learner => {
        return {
          ...learner,
          fullName: getUserFullName(learner),
        };
      };

      const contentToQuizId = (contentId, context) => context + '.' + contentId;

      service.getOverview = quizId => {
        const url = new UrlBuilder(loConfig.quiz.overview, { quizId }, {});
        return Request.promiseRequest(url, 'get');
      };

      service.getOverviewByContent = (contentId, context = Course.id) => {
        return service.getOverview(contentToQuizId(contentId, context));
      };

      service.getStudentSubmissionSummary = quizId => {
        return service.getOverview(quizId).then(overview => {
          const studentSubmissionSummary = map(overview, info => {
            const userInfo = buildUserInfo(info.learner);
            return {
              ...userInfo,
              ...info,
            };
          });

          return studentSubmissionSummary;
        });
      };

      service.getStudentSubmissionSummaryByContent = (contentId, context = Course.id) => {
        return service.getStudentSubmissionSummary(contentToQuizId(contentId, context));
      };

      service.sendMessage = (contentId, userId, message) => {
        const url = new UrlBuilder(
          loConfig.instructorNotification.notify,
          { context: Course.id },
          {}
        );

        return Request.promiseRequest(url, 'post', {
          edgePath: contentId,
          message: message,
          notifiedUserIds: [userId],
          urgency: 'Alert',
        });
      };

      return service;
    },
  ]);
