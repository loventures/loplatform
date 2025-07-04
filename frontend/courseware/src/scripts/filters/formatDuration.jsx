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
 * @alias formatDuration
 * @memberOf lo.filters
 *
 * @description
 *  Formats a {@link http://day.js.org day.js } duration object as a string.
 *
 * @requires $translate
 *
 * @param {dayjs} input day.js duration to be stringified.
 * @param {string} human 'short' => 1hr 2min 3sec, true or 'human' 1 Hours 2 Minutes 3 Seconds
 *   If false, returns "1:2:..."
 * @returns {string} Duration represented as a string.
 *
 * @example
    <doc:example module="lo.filters">
      <doc:source>
        <script>
            function Ctrl($scope){
                $scope.dur = dayjs.duration;
            }
        </script>
        <div ng-controller="Ctrl">
            32768 seconds, inhuman: {{ dur(32768, 'seconds') | formatDuration }} <br>
            32768 seconds, human: {{ dur(98304, 'seconds') | formatDuration:true }} <br>
        </div>
      </doc:source>
    </doc:example>
 */

import dayjs from 'dayjs';

angular.module('lo.filters').filter('formatDuration', [
  '$translate',
  function ($translate) {
    function pluralize(out, num, txt) {
      if (num !== 1) {
        txt += 's';
      }
      txt = $translate.instant(txt) || txt;
      out = '' + num;
      out += ' ';
      out += txt;
      out += ' ';
      return out;
    }
    function pad(num) {
      return '0' + num;
    }
    function formatHuman(input, out, rest) {
      if (input.days() > 0) {
        out += pluralize(out, input.days(), 'Day');
      }
      if (rest || input.hours() > 0) {
        out += pluralize(out, input.hours(), 'Hour');
      }
      if (rest || input.minutes() > 0) {
        out += pluralize(out, input.minutes(), 'Minute');
      }
      if (rest || input.seconds() > 0) {
        out += pluralize(out, input.seconds(), 'Second');
      }
      return out;
    }
    function formatHumanShort(input, out, rest) {
      if (input.days() > 0) {
        out += pluralize(out, input.days(), 'day');
      }
      if (rest || input.hours() > 0) {
        out += input.hours() + ' hr ';
      }
      if (rest || input.minutes() > 0) {
        out += input.minutes() + ' min ';
      }
      if (rest || input.seconds() > 0) {
        out += input.seconds() + ' sec ';
      }
      return out;
    }
    function formatRegular(input) {
      var hours = Math.floor(input.asHours()),
        minutes = input.minutes(),
        seconds = input.seconds();

      if (hours > 99) {
        return '99:59:59';
      }
      if (hours < 10) {
        hours = pad(hours);
      }
      if (minutes < 10) {
        minutes = pad(minutes);
      }
      if (seconds < 10) {
        seconds = pad(seconds);
      }
      return hours + ':' + minutes + ':' + seconds;
    }
    return function (input, human) {
      var inputDayJs = dayjs.duration(input);
      var out = '';
      var rest = !human;
      if (inputDayJs) {
        if (human === 'short') {
          out = formatHumanShort(inputDayJs, out, rest);
        } else if (human) {
          out = formatHuman(inputDayJs, out, rest);
        } else {
          out = formatRegular(inputDayJs);
        }
      } else {
        if (!human) {
          out = '00:00:00';
        }
      }
      return out.trim();
    };
  },
]);
