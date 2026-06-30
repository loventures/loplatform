/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import { round } from './round.ts';

type Translate = (key: string) => string;

const isNumber = (x: unknown): x is number => typeof x === 'number';

export interface GradeDisplayMethods {
  noGrade: () => string;
  color: (awarded: number, possible: number) => unknown;
  defaultDisplay: (...args: any[]) => unknown;
  percent: (awarded: number, possible: number, precision?: number) => unknown;
  percentSign: (awarded: number, possible: number, precision?: number) => string;
  points: (awarded: number, possible: number, precision?: number) => unknown;
  pointsOutOf: (awarded: number, possible: number, precision?: number) => string;
  percentThenPoints: (
    awarded: number,
    possible: number,
    percentPrecision?: number,
    pointsPrecision?: number
  ) => string;
  passFail: (awarded: number, possible: number, threshhold?: number) => string;
}

/**
 * Build the grade-display strategy object. Decorate the result for more display
 * methods or to alter existing ones.
 *
 * Behaviour is preserved verbatim from the original `GradeDisplayMethods`
 * Angular service; the only change is that the `round` filter dependency is now
 * the pure function it already delegated to, and `$translate` is injected as a
 * function (identity by default).
 */
export const makeGradeDisplayMethods = (translate: Translate = key => key): GradeDisplayMethods => {
  const defaultNoGrade = '–';
  const defaultMethod = 'percent';
  const defaultPercentPrecision = 1;
  const defaultPointsPrecision = 2;
  const defaultPassThreshhold = 0;

  const service: GradeDisplayMethods = {
    noGrade: () => defaultNoGrade,
    color: (awarded, possible) => service.percent(awarded, possible, 0),
    defaultDisplay: (...args) => (service as any)[defaultMethod](...args),
    percent: (awarded, possible, precision) => {
      if (isNaN(awarded)) {
        return defaultNoGrade;
      }
      precision = isNaN(precision) ? defaultPercentPrecision : precision;
      return round((100 * awarded) / possible, precision);
    },
    percentSign: (awarded, possible, precision) => {
      if (isNaN(awarded)) {
        return defaultNoGrade + ' %';
      }
      precision = isNaN(precision) ? defaultPercentPrecision : precision;
      return service.percent(awarded, possible, precision) + '%';
    },
    points: (awarded, possible, precision) => {
      if (isNaN(awarded)) {
        return defaultNoGrade + ' / ' + round(possible, precision);
      }
      precision = isNaN(precision) ? defaultPointsPrecision : precision;
      return round(awarded, precision);
    },
    pointsOutOf: (awarded, possible, precision) => {
      if (isNaN(awarded)) {
        return defaultNoGrade + ' / ' + round(possible, precision);
      }
      precision = isNaN(precision) ? defaultPointsPrecision : precision;
      return round(awarded, precision) + ' / ' + round(possible, precision);
    },
    percentThenPoints: (awarded, possible, percentPrecision, pointsPrecision) => {
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
    passFail: (awarded, possible, threshhold) => {
      if (isNaN(awarded)) {
        return defaultNoGrade;
      }
      threshhold = isNaN(threshhold) ? defaultPassThreshhold : threshhold;
      return awarded > threshhold ? translate('GRADE_PASS') : translate('GRADE_FAIL');
    },
  };

  return service;
};

/**
 * Render a score object (or bare number) with a named display method, e.g.
 * `grade(methods, score, 'percentSign', 2)`. Falls back to the default display
 * for unknown methods, and to `noGrade()` for unusable scores.
 *
 * Behaviour is preserved verbatim from the original `grade` Angular filter,
 * including the field-name fallbacks for awarded/possible and the quirk that
 * the third filter argument doubles as both the "show empty postfix" flag and
 * the display method's first extra argument (usually precision).
 */
export const grade = (methods: GradeDisplayMethods, score: any, ...rest: any[]): any => {
  const displayMethod = rest[0];
  const showEmptyPostfix = rest[1];

  // If they pass in a bare number this will render it.
  if (isNumber(score)) {
    score = {
      pointsAwarded: score,
      pointsPossible: 1,
    };
  }

  if (!isObject(score) || isEmpty(score)) {
    return methods.noGrade();
  }

  const s: any = score;

  // ifonly types
  const awarded = isNumber(s.pointsAwarded)
    ? s.pointsAwarded
    : isNumber(s.points_awarded)
      ? s.points_awarded
      : isNumber(s.pointsEarned)
        ? s.pointsEarned
        : isNumber(s.awarded)
          ? s.awarded
          : isNumber(s.earned)
            ? s.earned
            : isNumber(s.grade)
              ? s.grade
              : NaN;

  // ifonly types
  const possible = isNumber(s.pointsPossible)
    ? s.pointsPossible
    : isNumber(s.points_possible)
      ? s.points_possible
      : isNumber(s.possible)
        ? s.possible
        : isNumber(s.maximumPoints)
          ? s.maximumPoints
          : isNumber(s.max)
            ? s.max
            : isNumber(get(s, 'info.score.possible'))
              ? get(s, 'info.score.possible')
              : 100;

  if (isNaN(possible)) {
    return methods.noGrade();
  }

  if (!showEmptyPostfix && isNaN(awarded)) {
    return methods.noGrade();
  }

  const method = (methods as any)[displayMethod] ? displayMethod : 'defaultDisplay';

  // Original slices arguments from index 2 (showEmptyPostfix onward).
  const args = [awarded, possible, ...rest.slice(1)];
  return (methods as any)[method](...args);
};
