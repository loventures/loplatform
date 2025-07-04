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

import { isObject, isEmpty, get } from 'lodash';

/**
 * @ngdoc filter
 * @alias grade
 * @memberOf lo.filters
 *
 * @description
 *  Turns decimals into percentages.
 *
 * @param {number|string} percentage Decimal number to be converted
 *    into a percentage (ie. 0.8 -> 80%)
 * @param {boolean} noSuffix Suppresses the "%" appended onto
 *    the end of this filter's output.
 * @param {number} precision number of decimals to round to
 * @returns {string} Formatted string as a human-readable percentage.
 *
 * @example
    <doc:example module="lo.filters">
      <doc:source>
       {{ {pointsPossible: 0.5, pointsEarned} | grade:percent:2:false }}
      </doc:source>
    </doc:example>
 */
angular
  .module('lo.filters')
  .service('GradeDisplayMethods', [
    'roundFilter',
    '$translate',
    function (roundFilter, $translate) {
      //decorate this service for more display methods or to alter existing ones

      var defaultNoGrade = '–';
      var defaultMethod = 'percent';
      var defaultPercentPrecision = 1;
      var defaultPointsPrecision = 2;
      var defaultPassThreshhold = 0;

      var service = {
        noGrade: function () {
          return defaultNoGrade;
        },
        color: function (awarded, possible) {
          return service.percent(awarded, possible, 0);
        },
        defaultDisplay: function () {
          var method = defaultMethod || 'percent';
          return service[method].apply(null, arguments);
        },
        percent: function (awarded, possible, precision) {
          if (isNaN(awarded)) {
            return defaultNoGrade;
          }
          precision = isNaN(precision) ? defaultPercentPrecision : precision;
          return roundFilter((100 * awarded) / possible, precision);
        },
        percentSign: function (awarded, possible, precision) {
          if (isNaN(awarded)) {
            return defaultNoGrade + ' %';
          }
          precision = isNaN(precision) ? defaultPercentPrecision : precision;
          return service.percent(awarded, possible, precision) + '%';
        },
        points: function (awarded, possible, precision) {
          if (isNaN(awarded)) {
            return defaultNoGrade + ' / ' + roundFilter(possible, precision);
          }
          precision = isNaN(precision) ? defaultPointsPrecision : precision;
          return roundFilter(awarded, precision);
        },
        pointsOutOf: function (awarded, possible, precision) {
          if (isNaN(awarded)) {
            return defaultNoGrade + ' / ' + roundFilter(possible, precision);
          }
          precision = isNaN(precision) ? defaultPointsPrecision : precision;
          return roundFilter(awarded, precision) + ' / ' + roundFilter(possible, precision);
        },
        percentThenPoints: function (awarded, possible, percentPrecision, pointsPrecision) {
          if (isNaN(awarded)) {
            return defaultNoGrade;
          }
          return (
            service.percentSign(awarded, possible, percentPrecision) +
            ' (' +
            service.pointsOutOf(awarded, possible, pointsPrecision) +
            ')'
          );
        },
        passFail: function (awarded, possible, threshhold) {
          if (isNaN(awarded)) {
            return defaultNoGrade;
          }
          threshhold = isNaN(threshhold) ? defaultPassThreshhold : threshhold;

          if (awarded > threshhold) {
            return $translate.instant('GRADE_PASS');
          } else {
            return $translate.instant('GRADE_FAIL');
          }
        },
      };

      return service;
    },
  ])
  .filter('grade', [
    'GradeDisplayMethods',
    function gradeFilter(GradeDisplayMethods) {
      return function (score, displayMethod, showEmptyPostfix) {
        //If they pass in score percent this will render it.
        if (angular.isNumber(score)) {
          score = {
            pointsAwarded: score,
            pointsPossible: 1,
          };
        }

        if (!isObject(score) || isEmpty(score)) {
          return GradeDisplayMethods.noGrade();
        }

        // ifonly types
        const awarded = angular.isNumber(score.pointsAwarded)
          ? score.pointsAwarded
          : angular.isNumber(score.points_awarded)
            ? score.points_awarded
            : angular.isNumber(score.pointsEarned)
              ? score.pointsEarned
              : angular.isNumber(score.awarded)
                ? score.awarded
                : angular.isNumber(score.earned)
                  ? score.earned
                  : angular.isNumber(score.grade)
                    ? score.grade
                    : NaN;

        // ifonly types
        const possible = angular.isNumber(score.pointsPossible)
          ? score.pointsPossible
          : angular.isNumber(score.points_possible)
            ? score.points_possible
            : angular.isNumber(score.possible)
              ? score.possible
              : angular.isNumber(score.maximumPoints)
                ? score.maximumPoints
                : angular.isNumber(score.max)
                  ? score.max
                  : angular.isNumber(get(score, 'info.score.possible'))
                    ? get(score, 'info.score.possible')
                    : 100;

        if (isNaN(possible)) {
          return GradeDisplayMethods.noGrade();
        }

        if (!showEmptyPostfix && isNaN(awarded)) {
          return GradeDisplayMethods.noGrade();
        }

        if (!GradeDisplayMethods[displayMethod]) {
          displayMethod = 'defaultDisplay';
        }

        let args = [].slice.call(arguments, 2);
        args = [awarded, possible].concat(args);

        return GradeDisplayMethods[displayMethod].apply(null, args);
      };
    },
  ]);
