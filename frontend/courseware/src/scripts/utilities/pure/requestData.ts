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

import { defaults, isEmpty, pick, extend } from 'lodash';

/**
 * The pure response-shaping/validation helpers from the AngularJS `Request`
 * service — the parts that touch neither $http nor $q. They run on every API
 * response (resolve() calls isValid + getActualData), so they are the
 * highest-value, lowest-risk slice of Request to extract and unit-test. The
 * $http/$q orchestration stays in the Request Angular adapter.
 */

/** SRS query-result properties to preserve when unwrapping `{ objects }`. */
export const metaProps = ['offset', 'limit', 'count', 'filterCount', 'totalCount'];

/**
 * Our APIs return a string like 'failed', a number id, or an object; the server
 * can also return HTTP 200 with an error body. Decide whether a response looks
 * like real data. Behaviour preserved verbatim.
 */
export const isValid = (data: any, status?: number): boolean => {
  if (status && status >= 400) {
    return false;
  }

  if (status === 204) {
    return true;
  }

  if (data == null) {
    return false;
  }

  if (data && data.error) {
    // is actually sending back an error using wrong status
    return false;
  }

  return true;
};

/**
 * APIs return either `[]` or `{ count, objects: [] }`; unwrap to the inner
 * `objects` array (carrying the meta props across, plus an `isPaged()` helper)
 * so callers don't deal with `data.objects` everywhere. Behaviour preserved
 * verbatim (angular.isUndefined replaced with `=== undefined`).
 */
export const getActualData = (data: any): any => {
  if (data && data.objects) {
    defaults(data.objects, pick(data, metaProps));
    data.objects.isPaged = function () {
      return !(
        data.objects.limit === undefined ||
        data.objects.offset === undefined ||
        data.objects.totalCount === undefined
      );
    };
    return data.objects;
  }
  return data;
};

/** Copy the SRS meta props from `src` onto `target`. */
export const extendMeta = (target: any, src: any): any => {
  const props = pick(src, metaProps);
  return extend(target, props);
};

/** True if the response is valid and actually carries results. */
export const hasResults = (data: any): boolean => {
  if (isValid(data)) {
    if (!isEmpty(data)) {
      if (data.objects && isEmpty(data.objects)) {
        return false;
      }
      return true;
    }
  }
  return false;
};
