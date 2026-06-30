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

import { isFunction, defaults, isEmpty } from 'lodash';
import { urlBase } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../UrlBuilder.js';
import Sanity from '../Sanitize.js';
import { isValid, getActualData, hasResults, extendMeta, metaProps } from './requestData.ts';
import type { RequestRuntime } from './requestRuntime.ts';

/**
 * The Request RPC layer, parameterised by a {@link RequestRuntime}. Behaviour is
 * preserved verbatim from the AngularJS `Request` factory; the only `$http`/`$q`
 * touch points become `rt.http()` / `rt.defer()` / `rt.reject()`. The Angular
 * adapter (utilities/Request.js) wires a `$http`/`$q` runtime (digest preserved
 * for existing consumers); utilities/request.ts wires the native axios runtime.
 */
export const makeRequest = (rt: RequestRuntime) => {
  const Request: any = {
    /**
     * The raw `$http(config)` / axios escape hatch: resolves with the full
     * `{ data, status, headers, config }` response and rejects with the same shape
     * (the response body on `.data`). For callers that need the status code or a
     * fully custom config — e.g. SessionService's `dehttp` 202-challenge dance.
     */
    http(config: any) {
      return rt.http(config);
    },
    /**
     * Kick off a request that resolves the defer on success. `successCb`/`errorCb`
     * are invoked with the response body only (status/headers/config are not
     * threaded through on this path — preserved verbatim). Optional `defer`,
     * `successCb`, `errorCb` let callers supply their own.
     */
    promiseRequest(
      url: any,
      method?: string,
      params?: any,
      successCb?: any,
      errorCb?: any,
      defer?: any,
      noSanity?: boolean,
      cfg?: any
    ) {
      defer = defer || rt.defer();
      successCb = isFunction(successCb) ? successCb(defer) : Request.resolve(defer);
      errorCb = isFunction(errorCb) ? errorCb(defer) : Request.reject(defer);

      url = url.toString(); // UrlBuilder object -> string (CBLPROD-1209)
      const conf: any = {
        url: /http/.test(url) ? url : urlBase + url,
        method: method ? method.toUpperCase() : 'GET',
      };

      // Extra $http/axios options
      if (cfg) {
        defaults(conf, cfg);
      }

      // Payload -> query params for GET, body otherwise
      if (conf.method === 'GET') {
        conf.params = isEmpty(params) ? null : params;
      } else {
        conf.data = params || {};
      }

      if (conf.method === 'DELETE') {
        conf.headers = { 'Content-Type': 'application/json;charset=UTF-8' };
      }

      rt.http(conf).then(
        response => successCb(response.data),
        response => errorCb(response.data)
      );
      const promise = defer.promise;
      // Fire-and-forget date sanitisation of the resolved body (mutates in place).
      // The reject no-op avoids an unhandled-rejection on this discarded chain —
      // native promises flag it where $q did not; the returned `promise` (which
      // callers await) still rejects unchanged.
      promise.then(Sanity.dates, () => {});
      return promise;
    },

    /** Header value to explicitly say a call should not extend the session. */
    NO_SESSION_EXTENSION: { headers: { 'X-No-Session-Extension': 'true' } },

    /** UrlBuilder wrapper for loConfig urls; other args shift one to the right. */
    promiseBuilderRequest(url: string, params: any, query: any, ...args: any[]) {
      if (!isFunction(UrlBuilder)) {
        throw new Error('Must include UrlBuilder to use promiseBuilderRequest');
      }
      const builderUrl = new (UrlBuilder as any)(url, params, query);
      return Request.promiseRequest(builderUrl, 'get', {}, ...args);
    },

    // Pure response-shaping/validation helpers (unit-tested in ./requestData.ts).
    getActualData,
    metaProps,
    extendMeta,
    isValid,

    /** isValid wrapper for promise chains; rejects (via the runtime) on an error body. */
    validate(data: any, status?: number) {
      return isValid(data, status) ? data : rt.reject(data);
    },

    hasResults,

    /**
     * The server can return HTTP 200 with a string "Failed" and other quirks, so
     * a call succeeds only if the body looks valid. resolve -> resolve with the
     * actual data; reject -> reject the deferred and log.
     */
    resolve(deferred: any, msg?: string) {
      return function (data: any, status?: number, headers?: any, config?: any) {
        if (isValid(data, status)) {
          deferred.resolve(getActualData(data));
        } else {
          Request.reject(deferred, msg)(data, status, headers, config);
        }
      };
    },

    reject(deferred: any, msg?: string) {
      return function (data: any, status?: number, headers?: any, config?: any) {
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

  return Request;
};

export type Request = ReturnType<typeof makeRequest>;
