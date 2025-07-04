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
 * @alias round
 * @memberOf lo.filters
 *
 * @description
 *  Convenience method for rounding a value to a specific number of decimal points. Default is 2.
 *
 *  Different from the 'number' filter in that it returns a number, not a String, and will NOT render trailing zeroes.
 *
 * @param {(number)} Decimal points to round to. 0 for no decimals. Negative to round the integer.
 * @returns {number} Number that has been rounded to the specified number of decimal points.
 *
 * @example
    <doc:example module="lo.filters">
      <doc:source>
        <script>
            function Ctrl($scope){
                $scope.amount = '123.456';
            }
        </script>
        <div ng-controller="Ctrl">
            Round default:  {{ amount | round }} //Renders 123.46
            Round positive: {{ amount | round:2 }} //Renders 123.46
            Round zero:     {{ amount | round:0 }} //Renders 123
            Round negative: {{ amount | round:-1}} //Renders 120
            Round beyond:   {{ amount | round:4 }} //Renders 123.456
        </div>
      </doc:source>
    </doc:example>
 */
angular.module('lo.filters').filter('round', function () {
  return function (value, decimals) {
    if (value === 0) {
      return 0;
    }
    if (!value) {
      return null;
    }
    decimals = isNaN(decimals) ? 2 : decimals;
    var p = Math.pow(10, decimals);
    return Math.round(value * p) / p;
  };
});
