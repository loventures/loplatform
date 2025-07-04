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

import angular from 'angular';

/** A service to disconnect the application from the server. */
export default angular
  .module('lo.utilities.Disconnect', [])
  .factory('DisableRequestsInterceptor', [
    '$q',
    'errorMessageFilter',
    function ($q, errorMessageFilter) {
      var disabling = false;
      return {
        request: cfg =>
          !disabling
            ? cfg
            : $q((_, reject) =>
                reject(errorMessageFilter('Rejecting request due to server disconnect'))
              ),
        disable: () => (disabling = true),
        enable: () => (disabling = false),
      };
    },
  ])
  .service('DisconnectService', [
    'DisableRequestsInterceptor',
    function (DisableRequestsInterceptor) {
      return {
        disconnect: DisableRequestsInterceptor.disable,
        reconnect: DisableRequestsInterceptor.enable,
      };
    },
  ]);
