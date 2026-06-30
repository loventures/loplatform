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

import { each, isArray, keyBy, map, values } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/** Minimal shape of the injected User service used here. */
export interface UserLike {
  getId(): any;
  recordActivity(): boolean;
}

/** A CallAggregator class (constructed with a call action). */
export type CallAggregatorClass = new (callAction: (argsMap: Record<string, unknown>) => any, delay?: number) => {
  queueCalls(argMap: Record<string, unknown>): PromiseLike<any>;
};

/** The progress reasons enum (was an Angular constant). */
export const ProgressReasons = {
  VISITED: 'VISITED',
  TESTED: 'TESTEDOUT',
  UNVISIT: 'UNVISIT',
  SKIPPED: 'SKIPPED',
} as const;

/**
 * Content-item progress service, migrated verbatim from the AngularJS
 * `ProgressService` to plain TS taking the injected `Request`, `CallAggregator`
 * class, and `User`. Goes through the (axios) Request, so the Request-boundary
 * digest bridge re-renders Angular consumers; the in-service `$q.when` sentinels
 * become native `Promise.resolve`.
 */
export const makeProgressService = (Request: RequestLike, CallAggregator: CallAggregatorClass, User: UserLike) => {
  const pS: any = {
    POLL_INTERVAL: 5000,
    polling: false,
    pollListeners: {},
    pollListenerIndex: 1,
    pollListenerCountMap: {},
  };

  pS.callAggregator = new CallAggregator(function (argsMap: Record<string, unknown>) {
    return pS.getProgress(values(argsMap)).then(function (data: any) {
      return data.progress;
    });
  });

  pS.getAggregatedProgress = function (path: any) {
    const argMap = keyBy([path], (a: any) => a);
    return pS.callAggregator.queueCalls(argMap).then(function (data: any) {
      const progress = data[path];
      return progress;
    });
  };

  /**
   *  @description Returns the current progress on contentItem(s) for
   *  particular user in a course context
   */
  pS.getProgress = function (paths: any, userId?: any, courseId?: any) {
    if (paths && !isArray(paths)) {
      paths = [paths];
    }

    const params: any = {
      paths: [],
    };

    each(paths, function (p: any) {
      params.paths.push(encodeURIComponent(p));
    });

    const url = new (UrlBuilder as any)(
      loConfig.progress.progress,
      {
        courseId: courseId || Course.id,
        userId: userId || User.getId(),
      },
      params
    );

    return Request.promiseRequest(url, 'get');
  };

  /**
   *  @description Returns the current progress for a course context for
   *  a particular user
   */
  pS.getCourseProgress = function (userId?: any, courseId?: any) {
    const url = new (UrlBuilder as any)(loConfig.progress.courseProgress, {
      courseId: courseId || Course.id,
      userId: userId || User.getId(),
    });
    return Request.promiseRequest(url, 'get');
  };

  /**
   *  @description Set progress on contentItem(s) for
   *  particular user in a course context
   */
  // TODO: completed should be a boolean!
  pS.setProgress = function (paths: any, completed: any, reason?: any, userId?: any, courseId?: any) {
    if (!User.recordActivity()) {
      return Promise.resolve([]);
    }

    if (paths && !isArray(paths)) {
      paths = [paths];
    }

    const progress: any[] = [];

    each(paths, function (path: any) {
      const type = reason === ProgressReasons.UNVISIT ? null : reason || ProgressReasons.VISITED;
      progress.push({
        path: encodeURIComponent(path),
        type,
        value: completed ? 1 : 0,
      });
    });

    const url = new (UrlBuilder as any)(loConfig.progress.progress, {
      courseId: courseId || Course.id,
      userId: userId || User.getId(),
    });
    return Request.promiseRequest(url, 'put', progress);
  };

  pS.getProgressReport = function (paths: any, userIds: any, courseId?: any) {
    const url = new (UrlBuilder as any)(
      loConfig.progress.progressReport,
      {
        courseId: courseId || Course.id,
      },
      {
        users: userIds,
        paths: map(paths, (p: any) => encodeURIComponent(p)),
      }
    );

    return Request.promiseRequest(url, 'get');
  };

  pS.getProgressReportForLearners = function (userIds: any, courseId?: any) {
    const url = new (UrlBuilder as any)(
      loConfig.progress.progressReport,
      {
        courseId: courseId || Course.id,
      },
      {
        users: userIds,
      }
    );
    return Request.promiseRequest(url, 'get');
  };

  pS.getOverallProgressReportForLearners = function (userIds: any, courseId?: any) {
    const url = new (UrlBuilder as any)(loConfig.progress.overallProgressReportForUsers, {
      courseId: courseId || Course.id,
      user: userIds,
    });

    return Request.promiseRequest(url, 'get');
  };

  pS.downloadProgressReport = () => {
    const url = new (UrlBuilder as any)(loConfig.progress.progressExport, {
      courseId: Course.id,
    }).toString();
    window.open(url, '_blank');
    return Promise.resolve();
  };

  return pS;
};

export type ProgressService = ReturnType<typeof makeProgressService>;
