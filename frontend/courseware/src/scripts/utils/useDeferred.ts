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

import { useState } from 'react';

export type DeferredReCreate<T> = (onDeferredReady: () => void) => Promise<T>;
export type DeferredResolve<T> = (value: T | PromiseLike<T>) => void;
export type DeferredReject = (reason?: any) => void;

export type DeferredTuple<T> = [DeferredReCreate<T>, DeferredResolve<T>, DeferredReject];

const uninitializedDeferrError = () => {
  throw 'Deferred not initialized yet';
};

/**
 * A hook that will maintain the state of a generic deferred promise.
 * Useful when you need to deferred execution and/or control when to resolve the
 * promise manually (i.e. do something after a confirmation modal)
 *
 * @returns {DeferredTuple}
 */
const useDeferred = <TPayload = void>(): DeferredTuple<TPayload> => {
  const [
    [resolve = uninitializedDeferrError, reject = uninitializedDeferrError],
    setResolveAndReject,
  ] = useState<[DeferredResolve<TPayload> | void, DeferredReject | void]>([undefined, undefined]);

  const deferPromise = (onDeferredReady: () => void) => {
    return new Promise<TPayload>((resolve, reject) => {
      setResolveAndReject([resolve, reject]);
      onDeferredReady();
    });
  };

  return [deferPromise, resolve, reject];
};

export default useDeferred;
