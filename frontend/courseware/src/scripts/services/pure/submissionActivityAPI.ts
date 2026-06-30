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

import { keyBy, map, mapValues } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/** Minimal shape of the injected User service used here (just the current user id). */
export interface UserLike {
  id: any;
}

/**
 * Resolve an object-of-promises to an object-of-values ($q.all's object form).
 * The Angular adapter passes `$q.all` (digest-preserving); native callers pass
 * the object-aware all from pure/callAggregator.
 */
export type AllObjFn = (obj: Record<string, PromiseLike<any>>) => PromiseLike<Record<string, any>>;

/**
 * Submission-activity (essay/upload assessment attempts) API, migrated verbatim
 * from the AngularJS `SubmissionActivityAPI` service to plain TS taking the
 * injected `Request`, `User`, and an object-form `all` runtime.
 */
export const makeSubmissionActivityAPI = (Request: RequestLike, User: UserLike, all: AllObjFn) => {
  const service: any = {};

  const formatAttemptAttachments = (attempt: any) => {
    return {
      ...attempt,
      attachmentInfos: mapValues(attempt.attachmentInfos, (info: any) => {
        return {
          ...info,
          viewUrl: service.createAttachmentUrl(attempt.id, info.id),
          downloadUrl: service.createAttachmentUrl(attempt.id, info.id, true),
          thumbnailUrl: service.createAttachmentUrl(attempt.id, info.id, false, true),
        };
      }),
    };
  };

  service.loadSubmissionAssessment = (assessmentId: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(loConfig.submissionAssessment.oneAssessment, { assessmentId }, { context });
    return Request.promiseRequest(url);
  };

  service.loadSubmissionAttempts = (assessmentId: any, userId: any = User.id, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(
      loConfig.submissionAssessmentAttempt.attempts,
      {},
      { context, userId, assessmentId }
    );
    return Request.promiseRequest(url).then((attempts: any) => keyBy(map(attempts, formatAttemptAttachments), 'id'));
  };

  service.loadSubmissionActivity = (contentId: any, userId: any = User.id, context: any = Course.id) => {
    return all({
      assessment: service.loadSubmissionAssessment(contentId, context),
      attempts: service.loadSubmissionAttempts(contentId, userId, context),
    }).then(({ assessment, attempts }: any) => {
      return {
        assessment,
        attempts,
      };
    });
  };

  service.attemptToMap = (attempt: any) => ({
    attempts: {
      [attempt.id]: formatAttemptAttachments(attempt),
    },
  });

  service.createAttempt = (contentId: any, subjectId: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(loConfig.submissionAssessmentAttempt.oneAttempt, {}, { context });
    return Request.promiseRequest(url, 'post', {
      contentId,
      subjectId,
    }).then(service.attemptToMap);
  };

  service.saveAttempt = (attempt: any, { essay, attachments, uploads }: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(
      loConfig.submissionAssessmentAttempt.oneAttempt,
      { attemptId: attempt.id },
      { context }
    );
    return Request.promiseRequest(url, 'post', {
      essay,
      attachments: attachments.map((a: any) => a.id),
      uploads,
    }).then(service.attemptToMap);
  };

  service.saveAndSubmitAttempt = (attempt: any, { essay, attachments, uploads }: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(
      loConfig.submissionAssessmentAttempt.oneAttempt,
      { attemptId: attempt.id },
      { context }
    );
    return Request.promiseRequest(url, 'post', {
      essay,
      attachments: attachments.map((a: any) => a.id),
      uploads,
      submit: true,
    }).then(service.attemptToMap);
  };

  service.submitAttempt = (attempt: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(
      loConfig.submissionAssessmentAttempt.submit,
      { attemptId: attempt.id },
      { context }
    );
    return Request.promiseRequest(url, 'post').then(service.attemptToMap);
  };

  service.saveAttemptScore = (attemptId: any, score: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(loConfig.submissionAssessmentAttempt.score, { attemptId }, { context });
    return Request.promiseRequest(url, 'post', {
      ...score,
    }).then(formatAttemptAttachments);
  };

  service.saveAttemptFeedback = (attemptId: any, feedback: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(loConfig.submissionAssessmentAttempt.feedback, { attemptId }, { context });
    return Request.promiseRequest(url, 'post', {
      ...feedback,
    }).then(formatAttemptAttachments);
  };

  service.createAttachmentUrl = (
    attemptId: any,
    attachmentId: any,
    download = false,
    thumbnail = false,
    context: any = Course.id
  ) => {
    const params: any = {
      attemptId,
      attachmentId,
    };

    if (download) {
      params.download = true;
    }

    if (thumbnail) {
      params.size = 'medium';
    }

    const url = new (UrlBuilder as any)(loConfig.submissionAssessmentAttempt.attachment, params, {
      context,
    });

    return url.toString();
  };

  service.createAttachmentRedirectUrl = (attemptId: any, attachmentId: any, context: any = Course.id) => {
    const params: any = {
      attemptId,
      attachmentId,
    };

    const url = new (UrlBuilder as any)(loConfig.submissionAssessmentAttempt.attachmentUrl, params, {
      context,
    });

    return url.toString();
  };

  service.invalidateAttempt = (attemptId: any, context: any = Course.id) => {
    const url = new (UrlBuilder as any)(loConfig.submissionAssessmentAttempt.invalidate, { attemptId }, { context });
    return Request.promiseRequest(url, 'post').then(formatAttemptAttachments);
  };

  return service;
};

export type SubmissionActivityAPI = ReturnType<typeof makeSubmissionActivityAPI>;
