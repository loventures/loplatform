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

import Course from '../bootstrap/course.js';
import { loConfig } from '../bootstrap/loConfig.js';
import { keyBy, map, mapValues } from 'lodash';
import Request from '../utilities/Request.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import User from '../utilities/User.js';

export default angular
  .module('lo.services.SubmissionActivityAPI', [User.name, Request.name])
  .service('SubmissionActivityAPI', [
    'User',
    'Request',
    '$q',
    function (User, Request, $q) {
      const service = {};

      const formatAttemptAttachments = attempt => {
        return {
          ...attempt,
          attachmentInfos: mapValues(attempt.attachmentInfos, info => {
            return {
              ...info,
              viewUrl: service.createAttachmentUrl(attempt.id, info.id),
              downloadUrl: service.createAttachmentUrl(attempt.id, info.id, true),
              thumbnailUrl: service.createAttachmentUrl(attempt.id, info.id, false, true),
            };
          }),
        };
      };

      service.loadSubmissionAssessment = (assessmentId, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessment.oneAssessment,
          { assessmentId },
          { context }
        );
        return Request.promiseRequest(url);
      };

      service.loadSubmissionAttempts = (assessmentId, userId = User.id, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.attempts,
          {},
          { context, userId, assessmentId }
        );
        return Request.promiseRequest(url).then(attempts =>
          keyBy(map(attempts, formatAttemptAttachments), 'id')
        );
      };

      service.loadSubmissionActivity = (contentId, userId = User.id, context = Course.id) => {
        return $q
          .all({
            assessment: service.loadSubmissionAssessment(contentId, context),
            attempts: service.loadSubmissionAttempts(contentId, userId, context),
          })
          .then(({ assessment, attempts }) => {
            return {
              assessment,
              attempts,
            };
          });
      };

      service.attemptToMap = attempt => ({
        attempts: {
          [attempt.id]: formatAttemptAttachments(attempt),
        },
      });

      service.createAttempt = (contentId, subjectId, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.oneAttempt,
          {},
          { context }
        );
        return Request.promiseRequest(url, 'post', {
          contentId,
          subjectId,
        }).then(service.attemptToMap);
      };

      service.saveAttempt = (attempt, { essay, attachments, uploads }, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.oneAttempt,
          { attemptId: attempt.id },
          { context }
        );
        return Request.promiseRequest(url, 'post', {
          essay,
          attachments: attachments.map(a => a.id),
          uploads,
        }).then(service.attemptToMap);
      };

      service.saveAndSubmitAttempt = (
        attempt,
        { essay, attachments, uploads },
        context = Course.id
      ) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.oneAttempt,
          { attemptId: attempt.id },
          { context }
        );
        return Request.promiseRequest(url, 'post', {
          essay,
          attachments: attachments.map(a => a.id),
          uploads,
          submit: true,
        }).then(service.attemptToMap);
      };

      service.submitAttempt = (attempt, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.submit,
          { attemptId: attempt.id },
          { context }
        );
        return Request.promiseRequest(url, 'post').then(service.attemptToMap);
      };

      service.saveAttemptScore = (attemptId, score, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.score,
          { attemptId },
          { context }
        );
        return Request.promiseRequest(url, 'post', {
          ...score,
        }).then(formatAttemptAttachments);
      };

      service.saveAttemptFeedback = (attemptId, feedback, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.feedback,
          { attemptId },
          { context }
        );
        return Request.promiseRequest(url, 'post', {
          ...feedback,
        }).then(formatAttemptAttachments);
      };

      service.createAttachmentUrl = (
        attemptId,
        attachmentId,
        download = false,
        thumbnail = false,
        context = Course.id
      ) => {
        let params = {
          attemptId,
          attachmentId,
        };

        if (download) {
          params.download = true;
        }

        if (thumbnail) {
          params.size = 'medium';
        }

        const url = new UrlBuilder(loConfig.submissionAssessmentAttempt.attachment, params, {
          context,
        });

        return url.toString();
      };

      service.createAttachmentRedirectUrl = (attemptId, attachmentId, context = Course.id) => {
        let params = {
          attemptId,
          attachmentId,
        };

        const url = new UrlBuilder(loConfig.submissionAssessmentAttempt.attachmentUrl, params, {
          context,
        });

        return url.toString();
      };

      service.invalidateAttempt = (attemptId, context = Course.id) => {
        const url = new UrlBuilder(
          loConfig.submissionAssessmentAttempt.invalidate,
          { attemptId },
          { context }
        );
        return Request.promiseRequest(url, 'post').then(formatAttemptAttachments);
      };

      return service;
    },
  ]);
