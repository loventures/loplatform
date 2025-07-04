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

import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTime);

/**
 * @ngdoc filter
 * @alias fromNow
 * @memberOf lo.filters
 *
 * @description
 *  Convenience method for using the day.js
 *   {@link https://day.js.org/docs/en/plugin/relative-time fromNow() }
 *   method in an Angular template.
 *
 * @param {(dayjs)} input day.js date object as our point of reference.
 * @param {(boolean)} noSuffix Suppress the "ago" suffix that dayjs appends to the duration.
 * @param {(boolean)} duration treats the input as a duration rather than as an absolute date.
 * @returns {string} Human-readable representation of the amount of time between now and the
 *   input.
 *
 * @example
    <doc:example module="lo.filters">
      <doc:source>
        <script>
            function Ctrl($scope){
                $scope.ref = dayjs().startOf('week');
            }
        </script>
        <div ng-controller="Ctrl">
            This week began: {{ ref | fromNow }} <br>
            Without suffix: {{ ref | fromNow:true }}
        </div>
      </doc:source>
    </doc:example>
 */

angular.module('lo.filters').filter('fromNow', function () {
  return function (input, suffix, duration) {
    return duration ? dayjs.duration(input).humanize(suffix) : dayjs(input).fromNow(suffix);
  };
});
