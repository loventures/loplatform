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

import { nativeRuntime } from './pure/callAggregator.ts';

/**
 * A native-Promise drop-in for AngularJS's `$q` service, for the A4 `$q` removal.
 * Files that previously injected `'$q'` import this as `$q` instead, so their
 * `$q.when` / `$q.all` / `$q.reject` / `$q.defer` call sites stay unchanged while
 * the dependency on the digest-integrated `$q` goes away.
 *
 * The digest these promises used to trigger is now supplied by the A4 bridges: the
 * Request-boundary shim (`Request.js` → `$rootScope.$applyAsync` after every axios
 * resolution) covers anything downstream of a `Request` call, and the `$ngRedux`
 * bridge (`bootstrap/ngRedux.js`) covers redux dispatches. Use this only where the
 * result is so covered (or doesn't drive Angular view state); a standalone
 * `$q.defer` whose `.then` mutates `$scope` outside both bridges still needs care.
 */

/** $q.all over an array (`Promise.all`) or an object map (keep the keys). */
const all = (things: any): Promise<any> =>
  Array.isArray(things) ? Promise.all(things) : (nativeRuntime.all(things) as Promise<any>);

const defer = <T = any>() => {
  let resolve!: (value?: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res as (value?: T) => void;
    reject = rej;
  });
  return { promise, resolve, reject };
};

export const nativeQ: any = {
  when: (value: any) => Promise.resolve(value),
  resolve: (value: any) => Promise.resolve(value),
  reject: (reason: any) => Promise.reject(reason),
  all,
  defer,
};

export default nativeQ;
