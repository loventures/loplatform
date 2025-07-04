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

/**
 * @license HTTP Throttler Module for AngularJS
 *
 * Sourced from https://github.com/mikepugh/angular-http-throttler
 *
 * (c) 2013 Mike Pugh
 * License: MIT
 */

export default angular
  .module('lo.utilities.httpThrottler', [])
  .factory('httpThrottler', [
    '$q',
    'httpBuffer',
    function ($q, httpBuffer) {
      var service = {
        ongoingRequestCount: function () {
          return httpBuffer.reqCount;
        },
        setMaxConcurrentRequests: function (val) {
          return (httpBuffer.maxConcurrentRequests = val || httpBuffer.maxConcurrentRequests);
        },
        request: function (config) {
          var deferred = $q.defer();
          httpBuffer.append(deferred);
          return deferred.promise.then(function () {
            return config;
          });
        },
        response: function (response) {
          httpBuffer.requestComplete();
          return $q.when(response);
        },
        responseError: function (rejection) {
          httpBuffer.requestComplete();
          return $q.reject(rejection);
        },
      };

      return service;
    },
  ])
  .factory('httpBuffer', function () {
    var service = {
      maxConcurrentRequests: 5,
      buffer: [],
      reqCount: 0,

      append: function (deferred) {
        service.buffer.push(deferred);
        service.retryOne();
      },

      requestComplete: function () {
        service.reqCount--;
        service.retryOne();
      },

      retryOne: function () {
        if (service.reqCount < service.maxConcurrentRequests) {
          var deferred = service.buffer.shift();
          if (deferred) {
            service.reqCount++;
            deferred.resolve();
          }
        } else {
          console.warn('Too many requests');
        }
      },
    };

    return service;
  });
