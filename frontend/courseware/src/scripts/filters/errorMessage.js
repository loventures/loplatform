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
 * @ngdoc filter
 * @name lo.filters.filter:errorMessage
 */

import { isEmpty, map, isArray, each, extend } from 'lodash';

angular.module('lo.filters').filter('errorMessage', [
  '$translate',
  function ($translate) {
    return function (errorObj) {
      var error = errorObj && errorObj.data ? errorObj.data : errorObj;
      var messages = [];
      var interpolateParams = {};

      if (error && !isEmpty(error.messages)) {
        if (isArray(error.messages)) {
          each(error.messages, function (m) {
            if (m) {
              if (m.i18nableMessage) {
                extend(interpolateParams, m.data);
                messages.push(m.i18nableMessage);
              } else if (m.message) {
                messages.push(m.message);
              } else if (angular.isString(m)) {
                messages.push(m);
              }
            }
          });
        } else if (angular.isString(error.messages.message)) {
          messages = [error.messages.message];
        }
      } else if (error && error.message) {
        messages = [error.message];
      } else if (angular.isString(error)) {
        messages = [error];
      }

      if (!messages.length) {
        messages = ['Error, Unknown issue.'];
      }

      return map(messages, msg => $translate.instant(msg, interpolateParams)).join(' ');
    };
  },
]);
