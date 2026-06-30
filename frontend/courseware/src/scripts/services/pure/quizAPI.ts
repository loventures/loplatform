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

import { isEmpty, map, mapValues } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (uses both promiseRequest and promiseBuilderRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
  promiseBuilderRequest(url: any, params: any, query: any, ...rest: any[]): PromiseLike<any>;
}

/** Minimal shape of the injected User service used here (just the current user id). */
interface UserLike {
  id: any;
}

/**
 * Quiz attempt API, migrated verbatim from the AngularJS `QuizAPI` service to
 * plain TS taking the injected `Request` plus the `User` service (only used for
 * the default learner id on loadAttempts).
 */
export const makeQuizAPI = (Request: RequestLike, User: UserLike) => {
  const formatAttemptAttachments = (attempt: any) => ({
    ...attempt,
    attachments: mapValues(attempt.attachments, (info: any) => ({
      ...info,
      viewUrl: service.createAttachmentUrl(attempt.id, info.id),
      downloadUrl: service.createAttachmentUrl(attempt.id, info.id, true),
      thumbnailUrl: service.createAttachmentUrl(attempt.id, info.id, false, true),
    })),
  });

  const postSubmit = (data: any) => ({
    //intentional omit updated attempt info like attempt.state
    //see quizActivitySelectors#selectQuizActivityOpenAttemptLoaderComponent
    attemptId: data.id,
    questions: data.questions,
    responses: data.responses,
  });

  const service: any = {
    loadQuiz: (contentId: any, context: any = Course.id) =>
      Request.promiseBuilderRequest(loConfig.quiz.get, { contentId }, { context }),

    loadQuestions: (contentId: any, context: any = Course.id) =>
      Request.promiseBuilderRequest(loConfig.quiz.getQuestions, { contentId }, { context }),

    loadAttempt: (attemptId: any, context: any = Course.id) => {
      const url = new (UrlBuilder as any)(loConfig.quiz.attempt, { attemptId }, { context });
      return Request.promiseRequest(url, 'get').then(formatAttemptAttachments);
    },

    loadAttempts: (quizId: any, userId: any = User.id, context: any = Course.id) => {
      const url = new (UrlBuilder as any)(loConfig.quiz.attempts, {}, { quizId, context, userId });
      return Request.promiseRequest(url, 'get').then((attempts: any) => map(attempts, formatAttemptAttachments));
    },

    createAttempt: (contentId: any, context: any = Course.id, competencies: any = undefined) => {
      const url = new (UrlBuilder as any)(loConfig.quiz.attempts, {}, { context });
      return Request.promiseRequest(url, 'post', { contentId, competencies });
    },

    submitQuestions: (
      attemptId: any,
      questionResponses: any,
      submitResponse: any,
      submit: any,
      autoSubmit: any,
      context: any = Course.id
    ) => {
      const responses = map(questionResponses, ({ selection, attachments, uploads }: any, questionIndex: any) => {
        const formattedResponse: any = { questionIndex, attachments, selection, submitResponse };
        if (!isEmpty(uploads)) {
          formattedResponse.uploads = uploads;
        }
        return formattedResponse;
      });

      const url = new (UrlBuilder as any)(loConfig.quiz.attempt, { attemptId }, { context });
      return Request.promiseRequest(url, 'post', { responses, submit, autoSubmit });
    },

    // both functions need the same signature though autoSubmit has no meaning here
    saveAttempt: (attemptId: any, questionResponses: any, _autoSubmit: any, context: any = Course.id) =>
      service.submitQuestions(attemptId, questionResponses, false, false, false, context).then(postSubmit),

    submitAttempt: (attemptId: any, questionResponses: any, autoSubmit: any, context: any = Course.id) =>
      service.submitQuestions(attemptId, questionResponses, true, true, autoSubmit, context).then(postSubmit),

    createAttachmentUrl: (
      attemptId: any,
      attachmentId: any,
      download = false,
      thumbnail = false,
      context: any = Course.id
    ) => {
      const params: any = { attemptId, attachmentId, download };
      if (thumbnail) {
        params.size = 'medium';
      }
      const url = new (UrlBuilder as any)(loConfig.quiz.attachment, params, { context });
      return url.toString();
    },

    invalidateAttempt: (attemptId: any, context: any = Course.id) => {
      const url = new (UrlBuilder as any)(loConfig.quiz.invalidate, { attemptId }, { context });
      return Request.promiseRequest(url, 'post').then(formatAttemptAttachments);
    },

    saveAttemptScore: (attemptId: any, score: any, context: any = Course.id) => {
      const url = new (UrlBuilder as any)(loConfig.quiz.score, { attemptId }, { context });
      return Request.promiseRequest(url, 'post', { ...score }).then(formatAttemptAttachments);
    },

    saveAttemptFeedback: (attemptId: any, feedback: any, context: any = Course.id) => {
      const url = new (UrlBuilder as any)(loConfig.quiz.feedback, { attemptId }, { context });
      return Request.promiseRequest(url, 'post', { ...feedback }).then(formatAttemptAttachments);
    },
  };

  return service;
};

export type QuizAPI = ReturnType<typeof makeQuizAPI>;
