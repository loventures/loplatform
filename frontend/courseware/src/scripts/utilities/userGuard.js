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
 * @ngdoc module
 * @name lof.userGuard
 * @description
 *   Adds X-UserId header to all outgoing HTTP requests, and monitors responses for errors that
 *   indicate that we are logged out, or have switched users (ie. in another tab).
 */

export default angular
  .module('lof.userGuard', ['ng'])
  .factory('userGuardInterceptor', [
    '$q',
    '$rootScope',
    function ($q, $rootScope) {
      var handleExpiration = function (res) {
        if (res.status === 403) {
          var msg = angular.fromJson(res.data);
          if (msg.error === 'session' && !res.config.url.match(/.*\.html?$/)) {
            $rootScope.$emit('sessionExpired', msg);
          }
        }
        return $q.reject(res);
      };

      return {
        requestError: handleExpiration,
        responseError: handleExpiration,
      };
    },
  ])
  .config([
    '$httpProvider',
    function ($httpProvider) {
      $httpProvider.interceptors.push('userGuardInterceptor');
    },
  ]);
