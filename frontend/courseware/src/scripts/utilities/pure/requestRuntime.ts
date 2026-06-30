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

import axios from 'axios';

/**
 * The HTTP/promise primitives the Request layer needs, factored out so they can
 * be supplied by Angular ($q / $http — digest-integrated; used by the Request
 * Angular adapter to preserve exact runtime behaviour) or by the browser (axios
 * / native Promise; the default for new TS/React callers).
 */
export interface Deferred<T = any> {
  promise: PromiseLike<T>;
  resolve(value?: T): void;
  reject(reason?: unknown): void;
}

export interface HttpConfig {
  url: string;
  method: string;
  params?: any;
  data?: any;
  headers?: any;
  [key: string]: any;
}

export interface HttpResponse {
  data: any;
  status?: number;
  headers?: any;
  config?: any;
}

export interface RequestRuntime {
  /** $q.defer() / a native deferred. */
  defer<T = any>(): Deferred<T>;
  /**
   * $http(config) / axios. Resolves with `{ data, status, ... }` on success and
   * **rejects with a value whose `.data` is the response body** (the `$http`
   * contract the Request success/error callbacks rely on).
   */
  http(config: HttpConfig): PromiseLike<HttpResponse>;
  /** $q.reject(reason) / Promise.reject(reason). */
  reject<T = any>(reason: unknown): PromiseLike<T>;
}

const nativeDeferred = <T = any>(): Deferred<T> => {
  let resolve!: (value?: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res as (value?: T) => void;
    reject = rej;
  });
  return { promise, resolve, reject };
};

/**
 * A native runtime backed by a specific axios-callable `http` (the global `axios`,
 * or a configured `axios.create()` instance). Normalises the axios success
 * response to `{ data, status, … }` and the AxiosError to the `$http` rejection
 * shape (the value's `.data` is the response body, which Request's error callbacks
 * rely on).
 */
export const makeAxiosRuntime = (http: (config: any) => PromiseLike<any>): RequestRuntime => ({
  defer: nativeDeferred,

  http(config: HttpConfig): PromiseLike<HttpResponse> {
    return http(config as any).then(
      response => ({
        data: response.data,
        status: response.status,
        headers: response.headers,
        config: response.config,
      }),
      error => {
        const response = error?.response;
        // eslint-disable-next-line no-throw-literal
        throw {
          data: response?.data,
          status: response?.status,
          headers: response?.headers,
          config: error?.config,
        };
      }
    );
  },

  reject<T = any>(reason: unknown): PromiseLike<T> {
    return Promise.reject(reason);
  },
});

/** Browser-native runtime: the global `axios` + native Promise. */
export const nativeRuntime: RequestRuntime = makeAxiosRuntime(config => axios(config));

/**
 * Wrap a runtime so every resolution/rejection calls `scheduleDigest` — the
 * Request-boundary digest bridge for the A4 axios cutover. The Angular adapter
 * passes `() => $rootScope.$applyAsync()` so that an Angular consumer doing a raw
 * `Request.x().then($scope = …)` (no `$q` in between) still re-renders after the
 * underlying runtime moved off digest-integrated `$q`/`$http` to axios.
 * `$applyAsync` coalesces, so the per-call cost is one scheduled (often no-op)
 * digest, and it is safe to call during an in-progress digest. (This is the
 * Request-layer analogue of the `$ngRedux.subscribe` bridge in bootstrap/ngRedux.)
 */
export const withDigest = (rt: RequestRuntime, scheduleDigest: () => void): RequestRuntime => ({
  http(config: HttpConfig): PromiseLike<HttpResponse> {
    return rt.http(config).then(
      res => {
        scheduleDigest();
        return res;
      },
      err => {
        scheduleDigest();
        throw err;
      }
    );
  },

  defer<T = any>(): Deferred<T> {
    const d = rt.defer<T>();
    return {
      promise: d.promise,
      resolve: (value?: T) => {
        d.resolve(value);
        scheduleDigest();
      },
      reject: (reason?: unknown) => {
        d.reject(reason);
        scheduleDigest();
      },
    };
  },

  reject<T = any>(reason: unknown): PromiseLike<T> {
    scheduleDigest();
    return rt.reject<T>(reason);
  },
});
