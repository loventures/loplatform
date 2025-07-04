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

import httpThrottler from '../utilities/httpThrottler';
import disconnectService from '../utilities/disconnectService';
import SessionService from '../services/SessionService';
import Settings from '../utilities/settings';
import csrfInterceptor from '../utilities/csrfInterceptor';
import UrlBuilder from '../utilities/UrlBuilder';
import { each, extend } from 'lodash';

export default angular
  .module('lof.bootstrap.interceptors', [
    SessionService.name,
    Settings.name,
    httpThrottler.name,
    disconnectService.name,
    csrfInterceptor.name,
  ])
  .config([
    '$provide',
    '$httpProvider',
    'SettingsProvider',
    function ($provide, $httpProvider, SettingsProvider) {
      /*
        These interceptors may selectively hold or block requests.
    */

      $httpProvider.interceptors.push('DisableRequestsInterceptor');

      $httpProvider.interceptors.push('httpThrottler');

      /*
        These interceptors will operate on requests that actually goes out.
        They should be monitoring only.
        Their order should not matter.
    */

      if (SettingsProvider.isFeatureEnabled('SessionListener')) {
        $httpProvider.interceptors.push('SessionListener');
      }

      $httpProvider.interceptors.push('csrfInterceptor');

      // Patch $http so that UrlBuilder objects are coerced to strings,
      // because Angular 1.4.9 no longer does this implicitly.
      // https://github.com/angular/angular.js/issues/14557
      $provide.decorator('$http', [
        '$delegate',
        function ($delegate) {
          var loHttp = function (cfg) {
            if (cfg.url instanceof UrlBuilder) {
              cfg.url = cfg.url.toString();
            }
            return $delegate(cfg);
          };

          extend(loHttp, $delegate);

          each(['get', 'delete', 'head', 'jsonp'], function (method) {
            loHttp[method] = function (url, cfg) {
              return loHttp(
                extend({}, cfg || {}, {
                  method: method,
                  url: url,
                })
              );
            };
          });

          each(['post', 'put'], function (method) {
            loHttp[method] = function (url, data, cfg) {
              return loHttp(
                extend({}, cfg || {}, {
                  method: method,
                  url: url,
                  data: data,
                })
              );
            };
          });

          return loHttp;
        },
      ]);
    },
  ]);
