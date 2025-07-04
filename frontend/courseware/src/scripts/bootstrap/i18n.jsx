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

import 'angular-translate';
import 'angular-translate-interpolation-messageformat';
import 'angular-translate-loader-static-files';

import { loaderForAngularTranslate } from '../api/i18nApi';
import dayjs from 'dayjs';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(timezone);

var escapeHtml = value => {
  var element = angular.element('<div></div>');
  element.text(value);
  return element.html();
};

export default angular
  .module('lo.locales', ['pascalprecht.translate'])
  .factory('i18nApi', loaderForAngularTranslate)
  .factory('loFallbackTranslator', function () {
    return (key, lang, params) => params?.['_'] ?? key; // match polyglot behaviour
  })
  .config([
    '$translateProvider',
    function ($translateProvider) {
      $translateProvider.useLoader('i18nApi');
      $translateProvider.addInterpolation('$translateMessageFormatInterpolation');
      $translateProvider.useMissingTranslationHandler('loFallbackTranslator');

      // SECURITY: we have to HTML escape parameters to our translation
      // messages or we are vulnerable to XSS attacks.
      // The angular-translate sanitization procedure is to recursively
      // escape the entire input parameter structure and *not* the
      // formatted strings that are actually used. The default implementation
      // is expensive and destroys anything interesting (think a moment
      // date time that we want to format). So this implementation just
      // escapes strings and only recurses to a depth of one. It is impossible
      // thus to have translation strings that dereference parameters nested
      // deeply in an object.
      var sanitizeParameters = (value, depth) => {
        // we test for simple object to avoid destroying moment date times.
        // one day this may not be good; we should perhaps just blacklist
        // moment and date and whatever is good and proper.
        if (angular.isObject(value) && value.constructor == Object) {
          var result = {};
          if (depth < 3) {
            // to reduce cost, just drop nested objects
            angular.forEach(value, (propertyValue, propertyKey) => {
              result[propertyKey] = sanitizeParameters(propertyValue, depth + 1);
            });
          }
          return result;
        } else if (angular.isString(value)) {
          return escapeHtml(value);
        } else {
          return value;
        }
      };

      $translateProvider.useSanitizeValueStrategy((value, mode) => {
        if (mode == 'params') {
          return sanitizeParameters(value, 0);
        }
        return value;
      });
    },
  ])
  .run([
    '$translate',
    function ($translate) {
      const lo_platform = window.lo_platform ?? {};
      const locale = lo_platform?.i18n?.locale?.toLowerCase();
      $translate.use(locale);
      dayjs.locale(locale);
      dayjs.tz.setDefault(dayjs.tz.guess());
    },
  ]);
