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

import { isEmpty, isNumber } from 'lodash';
import Request from './Request.js';
import UrlBuilder from './UrlBuilder.js';

/**
 * @ngdoc service
 * @name lo.srs.StepLoader
 * @description
 *      utility method for loading the entirety of an srs resource
 *      that has a sever hard limit of 255 items per call
 */
export default angular.module('lo.utilities.StepLoader', [Request.name]).factory('StepLoader', [
  '$q',
  'Request',
  function ($q, Request) {
    var SRS_LIMIT = 255;

    var service = {};

    /**
     * @ngdoc method
     * @name loadOneStep
     * @methodOf lo.srs.StepLoader
     * @description
     *      Load one set of items and if there are more, initiate another step
     * @param {UrlBuilder} url
     *      The url builder object that should contain a paginate-able query object
     * @param {Array} loadedItems
     *      A continual tally of all items loaded so far
     * @returns {Promise}
     *      A promise chain that eventually resolves the complete result
     */
    service.loadOneStep = function (url, loadedItems) {
      return Request.promiseRequest(url, 'get').then(function (items) {
        loadedItems = loadedItems.concat(items);
        if (
          isEmpty(items) ||
          !isNumber(items.filterCount) ||
          loadedItems.length >= items.filterCount
        ) {
          return $q.when(loadedItems);
        } else {
          url.query.nextPage();
          return service.loadOneStep(url, loadedItems);
        }
      });
    };

    /**
     * @ngdoc method
     * @name stepLoad
     * @methodOf lo.srs.StepLoader
     * @description
     *      Load the complete content of an srs call and bypass the
     *      255 limit on the server
     * @param {string} url
     *      The url to load
     * @returns {Promise}
     *      A promise resolves the complete result of the call
     */
    service.stepLoad = function (url) {
      if (typeof url === 'string') {
        url = new UrlBuilder(url);
      }

      url.query.setLimit(SRS_LIMIT);
      url.query.setOffset(0);
      return service.loadOneStep(url, []);
    };

    return service;
  },
]);
