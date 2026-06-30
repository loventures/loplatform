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

import { map } from 'lodash';
import { getUserFullName } from '../../utilities/getUserFullName.js';
import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';
import Course from '../../bootstrap/course.ts';
import type { RequestFn } from './discussionSummaryAPI.ts';

/**
 * Quiz overview / instructor submission summary, migrated verbatim from the
 * AngularJS `QuizOverviewAPI` service to plain TS taking an injected `request`.
 */
export const makeQuizOverviewAPI = (request: RequestFn) => {
  const buildUserInfo = (learner: any) => ({
    ...learner,
    fullName: getUserFullName(learner),
  });

  const contentToQuizId = (contentId: any, context: any) => context + '.' + contentId;

  const service = {
    getOverview(quizId: any) {
      const url = new (UrlBuilder as any)(loConfig.quiz.overview, { quizId }, {});
      return request(url, 'get');
    },

    getOverviewByContent(contentId: any, context: any = Course.id) {
      return service.getOverview(contentToQuizId(contentId, context));
    },

    getStudentSubmissionSummary(quizId: any) {
      return service.getOverview(quizId).then((overview: any) =>
        map(overview, (info: any) => ({
          ...buildUserInfo(info.learner),
          ...info,
        }))
      );
    },

    getStudentSubmissionSummaryByContent(contentId: any, context: any = Course.id) {
      return service.getStudentSubmissionSummary(contentToQuizId(contentId, context));
    },

    sendMessage(contentId: any, userId: any, message: any) {
      const url = new (UrlBuilder as any)(loConfig.instructorNotification.notify, { context: Course.id }, {});
      return request(url, 'post', {
        edgePath: contentId,
        message,
        notifiedUserIds: [userId],
        urgency: 'Alert',
      });
    },
  };

  return service;
};

export type QuizOverviewAPI = ReturnType<typeof makeQuizOverviewAPI>;
