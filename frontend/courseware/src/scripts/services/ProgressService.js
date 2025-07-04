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
import { each, isArray, keyBy, map, values } from 'lodash';
import CallAggregator from '../utilities/CallAggregator.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

/**
 * @ngdoc service
 * @alias ProgressService
 * @memberOf lo.services
 * @description
 *     Manages content item progress information within a course context
 */
export default angular
  .module('lo.services.ProgressService', [CallAggregator.name])
  .constant('ProgressReasons', {
    VISITED: 'VISITED',
    TESTED: 'TESTEDOUT',
    UNVISIT: 'UNVISIT',
    SKIPPED: 'SKIPPED',
  })
  .factory('ProgressService', [
    '$q',
    'CallAggregator',
    'ProgressReasons',
    'Request',
    'User',
    function ($q, CallAggregator, ProgressReasons, Request, User) {
      var pS = {
        POLL_INTERVAL: 5000,
        polling: false,
        pollListeners: {},
        pollListenerIndex: 1,
        pollListenerCountMap: {},
      };

      pS.callAggregator = new CallAggregator(function (argsMap) {
        return pS.getProgress(values(argsMap)).then(function (data) {
          return data.progress;
        });
      });

      pS.getAggregatedProgress = function (path) {
        var argMap = keyBy([path], a => a);
        return pS.callAggregator.queueCalls(argMap).then(function (data) {
          var progress = data[path];
          return progress;
        });
      };

      /**
       *  @description Returns the current progress on contentItem(s) for
       *  particular user in a course context
       */
      pS.getProgress = function (paths, userId, courseId) {
        if (paths && !isArray(paths)) {
          paths = [paths];
        }

        var params = {
          paths: [],
        };

        each(paths, function (p) {
          params.paths.push(encodeURIComponent(p));
        });

        var url = new UrlBuilder(
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
      pS.getCourseProgress = function (userId, courseId) {
        var url = new UrlBuilder(loConfig.progress.courseProgress, {
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
      pS.setProgress = function (paths, completed, reason, userId, courseId) {
        if (!User.recordActivity()) {
          return $q.when([]);
        }

        if (paths && !isArray(paths)) {
          paths = [paths];
        }

        var progress = [];

        each(paths, function (path) {
          const type =
            reason === ProgressReasons.UNVISIT ? null : reason || ProgressReasons.VISITED;
          progress.push({
            path: encodeURIComponent(path),
            type,
            value: completed ? 1 : 0,
          });
        });

        var url = new UrlBuilder(loConfig.progress.progress, {
          courseId: courseId || Course.id,
          userId: userId || User.getId(),
        });
        return Request.promiseRequest(url, 'put', progress);
      };

      pS.getProgressReport = function (paths, userIds, courseId) {
        var url = new UrlBuilder(
          loConfig.progress.progressReport,
          {
            courseId: courseId || Course.id,
          },
          {
            users: userIds,
            paths: map(paths, p => encodeURIComponent(p)),
          }
        );

        return Request.promiseRequest(url, 'get');
      };

      pS.getProgressReportForLearners = function (userIds, courseId) {
        var url = new UrlBuilder(
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

      pS.getOverallProgressReportForLearners = function (userIds, courseId) {
        var url = new UrlBuilder(loConfig.progress.overallProgressReportForUsers, {
          courseId: courseId || Course.id,
          user: userIds,
        });

        return Request.promiseRequest(url, 'get');
      };

      pS.downloadProgressReport = () => {
        const url = new UrlBuilder(loConfig.progress.progressExport, {
          courseId: Course.id,
        }).toString();
        window.open(url, '_blank');
        return $q.when();
      };

      return pS;
    },
  ]);
