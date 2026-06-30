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

import { isEmpty, mapValues, each } from 'lodash';

/**
 * The async primitives CallAggregator needs, factored out so the behaviour can
 * be supplied by Angular ($q / $timeout — digest-integrated, used by the
 * AngularJS adapter to preserve exact runtime behaviour) or by the browser
 * (Promise / setTimeout — the default, used by React/TS callers and tests).
 */
export interface AggregatorRuntime {
  defer<T>(): { promise: PromiseLike<T>; resolve: (value: T) => void };
  when<T>(value: T): PromiseLike<T>;
  /** Like $q.all but over an object map: resolve each value, keep the keys. */
  all(map: Record<string, PromiseLike<unknown> | unknown>): PromiseLike<Record<string, unknown>>;
  setTimeout(fn: () => void, ms: number): unknown;
  clearTimeout(handle: unknown): void;
}

/** Browser-native runtime. */
export const nativeRuntime: AggregatorRuntime = {
  defer<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>(res => {
      resolve = res;
    });
    return { promise, resolve };
  },
  when<T>(value: T) {
    return Promise.resolve(value);
  },
  all(map) {
    const keys = Object.keys(map);
    return Promise.all(keys.map(k => map[k])).then(results => {
      const out: Record<string, unknown> = {};
      keys.forEach((k, i) => {
        out[k] = results[i];
      });
      return out;
    });
  },
  setTimeout(fn, ms) {
    return setTimeout(fn, ms);
  },
  clearTimeout(handle) {
    clearTimeout(handle as ReturnType<typeof setTimeout>);
  },
};

type CallAction = (argsMap: Record<string, unknown>) => PromiseLike<Record<string, unknown>> | unknown;

/**
 * Batches calls keyed by id, invoking `callAction` once per debounce window with
 * the merged argument map and fanning the results back out to each caller's
 * promise. Behaviour is preserved verbatim from the AngularJS `CallAggregator`.
 */
export class CallAggregator {
  waiting: Record<string, { defer: ReturnType<AggregatorRuntime['defer']>; args: unknown }> = {};
  ongoing: Record<string, { defer: ReturnType<AggregatorRuntime['defer']>; args: unknown }> | null = null;
  callAction: CallAction;
  delay: number;
  tryCallTimeout: unknown;
  protected rt: AggregatorRuntime;

  constructor(callAction: CallAction, delay?: number, runtime: AggregatorRuntime = nativeRuntime) {
    this.callAction = callAction;
    this.delay = delay || 500;
    this.rt = runtime;
  }

  _promiseCall(argsMap: Record<string, unknown>): PromiseLike<Record<string, unknown>> {
    const promise = this.callAction(argsMap) as any;
    return promise && promise.then ? promise : this.rt.when(argsMap);
  }

  _doCall() {
    if (isEmpty(this.waiting)) {
      return;
    }

    this.ongoing = this.waiting;
    this.waiting = {};

    const argsMap = mapValues(this.ongoing, info => info.args);

    this._promiseCall(argsMap).then((resultsMap: Record<string, unknown>) => {
      each(this.ongoing, (info, key) => {
        if (resultsMap[key]) {
          info.defer.resolve(resultsMap[key]);
        } else {
          info.defer.resolve(null);
        }
      });

      this.ongoing = null;

      this._tryCall();
    });
  }

  _tryCall() {
    this.rt.clearTimeout(this.tryCallTimeout);

    this.tryCallTimeout = this.rt.setTimeout(() => {
      if (this.ongoing === null) {
        this._doCall();
      }
    }, this.delay);
  }

  /**
   * Add keyed arguments to the aggregator; resolves a map of key -> result once
   * the next batch completes.
   */
  queueCalls(argumentMap: Record<string, unknown>): PromiseLike<Record<string, unknown>> {
    const promises = mapValues(argumentMap, (args, key) => {
      this.waiting[key] = this.waiting[key] || {
        defer: this.rt.defer(),
        args,
      };

      this._tryCall();

      return this.waiting[key].defer.promise;
    });

    return this.rt.all(promises as Record<string, PromiseLike<unknown>>);
  }
}

/** Keeps a CallAggregator per key, creating them lazily from `actionCreator`. */
export class CallAggregatorsSet {
  aggregators: Record<string, CallAggregator> = {};
  delay?: number;
  actionCreator: (key: string) => CallAction;
  private Aggregator: typeof CallAggregator;

  constructor(
    actionCreator: (key: string) => CallAction,
    delay?: number,
    Aggregator: typeof CallAggregator = CallAggregator
  ) {
    this.delay = delay;
    this.actionCreator = actionCreator;
    this.Aggregator = Aggregator;
  }

  getOrCreate(key: string): CallAggregator {
    if (!this.aggregators[key]) {
      const callAction = this.actionCreator(key);
      this.aggregators[key] = new this.Aggregator(callAction);
    }

    return this.aggregators[key];
  }
}
