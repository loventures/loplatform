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

import { isFunction, defaults, isEmpty, pick, extend } from 'lodash';
import { urlBase } from '../bootstrap/loConfig.js';
import UrlBuilder from './UrlBuilder.js';
import Sanity from './Sanitize.js';

/**
 * @ngdoc object
 * @description Perform RPC connections and wrap with our standard reject and resolve
 * functions.  This also has a wrapper for doing promise http calls (resolves when done).
 */
export default angular.module('lo.utilities.Request', []).factory('Request', [
  '$http',
  '$q',
  function ($http, $q) {
    /**
     * Configuring Common parameters for Service
     * URLs base
     * @alias Request
     */
    var Request = {
      /**
       * @description
       * Kick off a request that will resolve the defer on the success of the call
       *
       * ONLY use this if you are actually in need of $q, which is to say MORE THAN
       * 1 ajax call at a time.  Otherwise use internal request.
       *
       * @param {String} url to call
       * @param {String} [method="get"] post|get|delete|put
       * @param {Object} [params] the item to pass to the server.
       * @param {function} [successCb] Defaults to window.resolve
       * @param {function} [errorCb] Defaults to window.reject
       * @param {$q.Defer} [defer] If you want to resolve a specific defer object
       * @param {boolean} [noSanity] If true it will not run post process sanity checks / operations (DEPRECATED??)
       * @param {boolean} [cfg] Extra configuration options to be added to the Angular $http request.
       * @returns {$q.Promise} A promise that will defer.resolve(Sanitize.dates(response))
       */
      promiseRequest: function (url, method, params, successCb, errorCb, defer, noSanity, cfg) {
        defer = defer || $q.defer();
        successCb = isFunction(successCb) ? successCb(defer) : Request.resolve(defer);
        errorCb = isFunction(errorCb) ? errorCb(defer) : Request.reject(defer);

        url = url.toString(); //UrlBuilder object, CBLPROD-1209 must make sure it is a string (DRAGONS)
        var conf = {
          url: /http/.test(url) ? url : urlBase + url,
          method: method ? method.toUpperCase() : 'GET',
        };

        //Add extra $http options if specified
        if (cfg) {
          defaults(conf, cfg);
        }

        // Translate our payload to URL parameters for GET requests
        if (conf.method === 'GET') {
          conf.params = isEmpty(params) ? null : params;
        } else {
          conf.data = params || {};
        }

        if (conf.method === 'DELETE') {
          conf.headers = { 'Content-Type': 'application/json;charset=UTF-8' };
        }

        $http(conf).then(
          response => successCb(response.data),
          response => errorCb(response.data)
        );
        var promise = defer.promise;
        promise.then(Sanity.dates);
        return promise;
      },

      /**
       * @ngdoc property
       * @name lo.utilities.included:Request#NO_SESSION_EXTENSION
       * @description
       * header value to explicitly say a call should not extend the session.
       */
      NO_SESSION_EXTENSION: { headers: { 'X-No-Session-Extension': 'true' } },

      /**
       * @description
       *
       * Wrapper that uses UrlBuilder to process loConfig urls with params.
       * Accepts all other params promiseRequest accepts, shifted one to the right
       *
       * @param {String} url loCOnfig url
       * @param {Object} params params to augment url with
       */
      promiseBuilderRequest: function (url, params, query, ...args) {
        if (!isFunction(UrlBuilder)) {
          throw new Error('Must include UrlBuilder to use promiseBuilderRequest');
        }
        const builderUrl = new UrlBuilder(url, params, query);

        return Request.promiseRequest(
          builderUrl,
          'get',
          {}, //placeholder for params
          ...args
        );
      },

      /**
       * @description
       * Our APIs return either [] or {count: n, objects: []}, we don't really want to deal
       * with caching and pruning data.objects all over the damn place.
       */
      getActualData: function (data) {
        if (data && data.objects) {
          defaults(data.objects, pick(data, Request.metaProps));
          data.objects.isPaged = function () {
            return !(
              angular.isUndefined(data.objects.limit) ||
              angular.isUndefined(data.objects.offset) ||
              angular.isUndefined(data.objects.totalCount)
            );
          };
          return data.objects;
        }
        return data;
      },
      /**
       * @description
       * Array of possible ApiQueryResults properties that we should preserve
       */
      metaProps: ['offset', 'limit', 'count', 'filterCount', 'totalCount'],
      /**
       * @description
       * Takes the SRS query parameters off of an array or object, and stick
       * them on another array or object
       *
       * @param {object} target  target object or array
       * @param {object} src     source object or array
       */
      extendMeta: function (target, src) {
        var props = pick(src, Request.metaProps);
        return extend(target, props);
      },
      /**
       * @description The APIs we have will either return an object or in the
       * case of a single id result, it will be a number.   A string result
       * comes back from some of the legacy APIs that return things like: 'failed'.
       * However a string or boolean like 123 should be valid (ie id lookup)
       *
       * @param {Object} data The data returned from the ajax request
       * @returns {boolean} true if we think the server didn't blow up again
       */
      isValid: function (data, status) {
        if (status && status >= 400) {
          return false;
        }

        if (status === 204) {
          return true;
        }

        //check data format

        if (data == null) {
          return false;
        }

        if (data && data.error) {
          //is actually sending back an error using wrong status
          return false;
        }

        return true;
      },
      /**
       * @description Convenience wrapper around `isValid` that can be invoked directly
       *   as part of a promise chain.
       *
       * @param  {object} data   Result of HTTP request
       * @param  {number} status HTTP Status Code
       * @return {promise} Promise that will be rejected if the response
       *    looks like an error
       */
      validate: function (data, status) {
        return Request.isValid(data, status) ? data : $q.reject(data);
      },
      /**
       * @description Check if the item actually has results.   This means that
       * we expect it to either be an Object or possibly just an id returned from
       * the server.
       * @param {Object} data The result you want to check
       * @resturns {boolean} returns true if we think there is data and it is valid
       */
      hasResults: function (data) {
        if (Request.isValid(data)) {
          if (!isEmpty(data)) {
            if (data.objects && isEmpty(data.objects)) {
              return false;
            }
            return true;
          }
        }
        return false;
      },

      /*
       * The main reason for global items is that the server can return an
       * http 200, with the string result "Failed", and many other screwy codes
       * / error states.  We assume that overall a call succeeds only if it is
       * a valid ajax object, or if the server returned the string 'true', or
       * 'false'
       *
       *  Request.resolve -> resolves the promise if we think the result was valid.
       *  Request.reject  -> rejects the promise on an ajax call.
       */
      resolve: function (deferred, msg) {
        return function (data, status, headers, config) {
          if (Request.isValid(data, status)) {
            deferred.resolve(Request.getActualData(data));
          } else {
            Request.reject(deferred, msg)(data, status, headers, config);
          }
        };
      },

      //For use in ajax calls when an error occured, provide
      reject: function (deferred, msg) {
        return function (data, status, headers, config) {
          if (deferred) {
            deferred.reject(data);
          }
          console.error(
            'Error Server w(data, status, headers, config, msg)',
            data,
            status,
            headers,
            config,
            msg
          );
        };
      },
    };

    window.$q = $q;
    window.Request = Request; //Debug in prod helper, because we will need it.
    return Request;
    //END OF RETURN
  },
]);
