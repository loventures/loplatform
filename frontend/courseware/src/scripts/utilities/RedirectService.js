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

import { each, isString, isFunction, some, isArray, isNil } from 'lodash';

var goToLogin = function () {
  var features = window.lo_platform.features;
  if (features && features.LoginPage && !isNil(features.LoginPage.value)) {
    window.location.href = features.LoginPage.value;
  } else {
    window.location.href = '';
  }
};

export default angular
  .module('lo.utilties.Redirector', [])
  .provider(
    'Redirector',
    /**
     * @ngdoc provider
     * @alias RedirectorProvider
     * @memberof lo.utilties
     */
    function RedirectorProvider() {
      /**
       * @ngdoc type
       * @memberof lo.utilties
       * @description an object that represent a set of redirection rules
       * that should be considered together, when the check is triggered.
       */
      var Redirector = function (link) {
        this.defaultLink = link;
        this.redirects = [];
        this.isRedirecting = false;
      };

      Redirector.prototype.when = function (condition, link) {
        this.redirects.push({
          condition: condition,
          link: link,
        });

        return this;
      };

      Redirector.prototype.redirect = function () {
        var done = false;
        each(this.redirects, redirect => {
          if (!done && redirect.condition()) {
            done = true;
            this.isRedirecting = true;
            if (isString(redirect.link)) {
              window.location.href = redirect.link;
            } else if (isFunction(redirect.link)) {
              window.location.href = redirect.link();
            }
          }
        });

        if (!done && this.defaultLink) {
          this.isRedirecting = true;
          window.location.href = this.defaultLink;
        }
      };

      this.Redirector = Redirector;
      this.redirectToLogin = goToLogin;

      this.$get = function () {
        return Redirector;
      };
    }
  )
  .provider(
    'RedirectService',
    /**
     * @ngdoc service
     * @alias RedirectServiceProvider
     * @memberof lo.services
     * @description set up Redirector objects, and provide
     * common rules
     */
    [
      'RedirectorProvider',
      function RedirectServiceProvider(RedirectorProvider) {
        var Redirector = RedirectorProvider.Redirector;

        /**
         * @ngdoc service
         * @memberof lo.services
         */
        var RedirectService = {};
        RedirectService.redirectors = {};

        RedirectService.on = function (name, defaultLink) {
          RedirectService.redirectors[name] = new Redirector(defaultLink);
          return RedirectService.redirectors[name];
        };

        this.on = RedirectService.on;

        RedirectService.redirectToLogin = goToLogin;
        RedirectService.redirect = function (name) {
          if (RedirectService.redirectors[name]) {
            RedirectService.redirectors[name].redirect();
          }
        };

        RedirectService.isRedirecting = function () {
          return some(RedirectService.redirectors, 'isRedirecting', true);
        };

        this.init = function (user) {
          this.user = user;
        };

        this.hasRole = function (role, reverse) {
          var serviceUser = this.user;
          return function (user) {
            user = user || serviceUser || {};
            var uInfo = user.current() || user;
            var userRoles = isArray(uInfo.roles) ? uInfo.roles : [uInfo.roles || 'unknown'];

            var hasRole = userRoles.indexOf(role) !== -1;

            return reverse ? !hasRole : hasRole;
          };
        };

        this.$get = function () {
          return RedirectService;
        };
      },
    ]
  )
  .run([
    'RedirectService',
    '$rootScope',
    function (RedirectService, $rootScope) {
      $rootScope.$on('$locationChangeStart', function (evt) {
        if (RedirectService.isRedirecting()) {
          evt.preventDefault();
        }
      });
    },
  ]);
