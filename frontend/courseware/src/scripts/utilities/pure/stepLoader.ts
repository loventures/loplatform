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

import { isEmpty, isNumber } from 'lodash';

/** Server-side SRS hard limit: at most 255 items per call. */
const SRS_LIMIT = 255;

/**
 * Issues a paginated GET. The Angular adapter passes Request.promiseRequest
 * (which returns a $q promise, so the .then chain below stays digest-integrated);
 * tests/React can pass any promise-returning request.
 */
export type RequestFn = (url: any, method: string) => PromiseLike<any>;

/**
 * Load one page and, if more remain, recurse to the next — accumulating into
 * `loadedItems`. Behaviour is preserved verbatim from the AngularJS StepLoader;
 * the original's redundant `$q.when(loadedItems)` base case is just `loadedItems`
 * (the surrounding .then resolves it identically), so no $q is needed.
 */
export const loadOneStep = (request: RequestFn, url: any, loadedItems: any[]): PromiseLike<any[]> =>
  request(url, 'get').then(items => {
    loadedItems = loadedItems.concat(items);
    if (isEmpty(items) || !isNumber(items.filterCount) || loadedItems.length >= items.filterCount) {
      return loadedItems;
    } else {
      url.query.nextPage();
      return loadOneStep(request, url, loadedItems);
    }
  });

/**
 * Load the complete contents of an SRS call, paging past the 255-item server
 * limit. `url` must be a UrlBuilder (the Angular adapter wraps bare strings, so
 * this module stays free of the DOM-touching UrlBuilder import).
 */
export const stepLoad = (request: RequestFn, url: any): PromiseLike<any[]> => {
  url.query.setLimit(SRS_LIMIT);
  url.query.setOffset(0);
  return loadOneStep(request, url, []);
};
