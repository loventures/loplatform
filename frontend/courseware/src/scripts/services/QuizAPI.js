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
import { isEmpty, map, mapValues } from 'lodash';
import Request from '../utilities/Request.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import User from '../utilities/User.js';

/**
 * @ngdoc service
 * @alias QuizAPI
 * @memberof lo.services
 * @description quiz API
 */
export default angular.module('lo.services.QuizAPI', [Request.name, User.name]).service('QuizAPI', [
  'Request',
  'User',
  function QuizAPI(Request, User) {
    const service = {};

    const formatAttemptAttachments = attempt => {
      return {
        ...attempt,
        attachments: mapValues(attempt.attachments, info => {
          return {
            ...info,
            viewUrl: service.createAttachmentUrl(attempt.id, info.id),
            downloadUrl: service.createAttachmentUrl(attempt.id, info.id, true),
            thumbnailUrl: service.createAttachmentUrl(attempt.id, info.id, false, true),
          };
        }),
      };
    };

    service.loadQuiz = (contentId, context = Course.id) =>
      Request.promiseBuilderRequest(loConfig.quiz.get, { contentId }, { context });

    service.loadQuestions = (contentId, context = Course.id) =>
      Request.promiseBuilderRequest(loConfig.quiz.getQuestions, { contentId }, { context });

    service.loadAttempt = (attemptId, context = Course.id) => {
      const url = new UrlBuilder(loConfig.quiz.attempt, { attemptId }, { context });
      return Request.promiseRequest(url, 'get').then(formatAttemptAttachments);
    };

    service.loadAttempts = (quizId, userId = User.id, context = Course.id) => {
      const url = new UrlBuilder(loConfig.quiz.attempts, {}, { quizId, context, userId });
      return Request.promiseRequest(url, 'get').then(attempts =>
        map(attempts, formatAttemptAttachments)
      );
    };

    service.createAttempt = (contentId, context = Course.id, competencies = undefined) => {
      const url = new UrlBuilder(loConfig.quiz.attempts, {}, { context });
      return Request.promiseRequest(url, 'post', { contentId, competencies });
    };

    service.submitQuestions = (
      attemptId,
      questionResponses,
      submitResponse, // submit each question response, only has meaning in multipage player w/ on-response-score
      submit, // submit the attempt
      autoSubmit, // the attempt submission is because of auto-submit
      context = Course.id
    ) => {
      const responses = map(
        questionResponses,
        ({ selection, attachments, uploads }, questionIndex) => {
          let formattedResponse = {
            questionIndex,
            attachments,
            selection,
            submitResponse,
          };

          if (!isEmpty(uploads)) {
            formattedResponse.uploads = uploads;
          }
          return formattedResponse;
        }
      );

      const url = new UrlBuilder(loConfig.quiz.attempt, { attemptId }, { context });

      return Request.promiseRequest(url, 'post', { responses, submit, autoSubmit });
    };

    const postSubmit = data => {
      //intentional omit updated attempt info like attempt.state
      //see quizActivitySelectors#selectQuizActivityOpenAttemptLoaderComponent
      //TODO: simplify in TECH-1335
      return {
        attemptId: data.id,
        questions: data.questions,
        responses: data.responses,
      };
    };

    // both functions need the same signature though autoSubmit has no meaning here
    service.saveAttempt = (attemptId, questionResponses, _autoSubmit, context = Course.id) => {
      return service
        .submitQuestions(attemptId, questionResponses, false, false, false, context)
        .then(postSubmit);
    };

    service.submitAttempt = (attemptId, questionResponses, autoSubmit, context = Course.id) => {
      return service
        .submitQuestions(attemptId, questionResponses, true, true, autoSubmit, context)
        .then(postSubmit);
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
        download,
      };
      if (thumbnail) {
        params.size = 'medium';
      }

      const url = new UrlBuilder(loConfig.quiz.attachment, params, { context });

      return url.toString();
    };

    service.invalidateAttempt = (attemptId, context = Course.id) => {
      const url = new UrlBuilder(loConfig.quiz.invalidate, { attemptId }, { context });
      return Request.promiseRequest(url, 'post').then(formatAttemptAttachments);
    };

    service.saveAttemptScore = (attemptId, score, context = Course.id) => {
      const url = new UrlBuilder(loConfig.quiz.score, { attemptId }, { context });
      return Request.promiseRequest(url, 'post', {
        ...score,
      }).then(formatAttemptAttachments);
    };

    service.saveAttemptFeedback = (attemptId, feedback, context = Course.id) => {
      const url = new UrlBuilder(loConfig.quiz.feedback, { attemptId }, { context });
      return Request.promiseRequest(url, 'post', {
        ...feedback,
      }).then(formatAttemptAttachments);
    };

    return service;
  },
]);
