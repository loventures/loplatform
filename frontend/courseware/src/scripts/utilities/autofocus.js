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

import { isUndefined } from 'lodash';

/**
 * @ngdoc directive
 * @alias loAutofocus
 * @memberOf lo.utilities
 * @description
 *  Directive that autofocuses an element after an optional delay (the attribute value).
 *  You can also specify an optional autofocus-on attribute to trigger focus on a watched flag.
 **/
export default angular.module('lo.utilities.autofocus', []).directive('loAutofocus', [
  '$timeout',
  function ($timeout) {
    return {
      restrict: 'A',
      link: ($scope, $element, $attrs) => {
        const triggerFocus = () =>
          $timeout(() => $element[0].focus(), parseInt($attrs.loAutofocus, 10) || 0);
        if (!isUndefined($attrs.autofocusOn)) {
          $scope.$watch(
            () => {
              return $attrs.autofocusOn === 'true';
            },
            isFocusOn => {
              if (isFocusOn) {
                triggerFocus();
              }
            }
          );
        } else {
          triggerFocus();
        }
      },
    };
  },
]);
