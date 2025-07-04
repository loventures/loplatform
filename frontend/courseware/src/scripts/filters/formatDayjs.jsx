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
import advanced from 'dayjs/plugin/advancedFormat';
import localized from 'dayjs/plugin/localizedFormat';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(localized);
dayjs.extend(advanced);

/**
 * @ngdoc filter
 * @alias formatDayjs
 * @memberOf lo.filters
 *
 * @description
 *  Formats a {@link http://day.js.org dayjs } date object as a string.
 *
 * @param {(dayjs)} input dayjs date object to be stringified.
 * @param {(string)} [format='l'] String representation of date that you'd like to generate.
 *   For more details,
     {@link https://day.js.org/docs/en/display/format see the dayjs documentation}.
 * @returns {string} Date/Time represented a string.
 *
 * @example
    <doc:example module="lo.filters">
      <doc:source>
        <script>
            function Ctrl($scope){
                $scope.date = dayjs();
            }
        </script>
        <div ng-controller="Ctrl">
            Default ("l"): {{ date | formatDayjs }} <br>
            "L": {{ date | formatDayjs:'L' }} <br>
            "LLLL": {{ date | formatDayjs:'LLLL' }}
        </div>
      </doc:source>
    </doc:example>
 */

export const FormatDayjsSetFormats = {
  time: 'MMM DD h:mm A z',
  full: 'MMM DD, YYYY h:mm A z',
};

function formatDayjsFilter() {
  return function (input, format, timezone) {
    if (!input) {
      return '';
    }
    input = dayjs(input);

    const lang = window.lo_platform.i18n.language || 'en';
    const fmt = FormatDayjsSetFormats[format] || format || 'l';

    if (timezone) {
      if (timezone === 'guessTimezone') {
        timezone = dayjs.tz.guess();
      }
      return input.locale(lang).tz(timezone).format(fmt);
    }
    return input.locale(lang).format(fmt);
  };
}

angular
  .module('lo.filters')
  .value('FormatDayjsSetFormats', FormatDayjsSetFormats)
  .filter('formatDayjs', formatDayjsFilter);
