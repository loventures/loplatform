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

import { useCallback, useEffect, useReducer } from 'react';

/**
 * The subset of the Angular `ResourceStore`/`LocalResourceStore` API that the
 * React srs components read/drive. The stores mutate these fields in place and
 * relied on the Angular digest for reactivity; in React we drive their
 * promise-returning methods through `run()` and force a re-render (see below).
 */
export interface SrsStore {
  data: any[];
  loading?: boolean;
  totalCount?: number;
  filterCount?: number;
  count?: number;
  currentPage?: number;
  pageSize?: number;
  maxSize?: number;
  loadErrorMessage?: string;
  title?: string;
  iconCls?: string;
  sortByProps?: Record<string, any>;
  searchByProps?: Record<string, any> | string[];
  gotoPage(page: number): Promise<unknown>;
  search?(str: string, props?: any): Promise<unknown>;
  sort?(startNew: boolean, clearAfter: boolean, ...configs: any[]): Promise<unknown>;
  setPageSize?(size: number): void;
}

export interface SrsStoreController {
  store: SrsStore;
  /**
   * Drive a store mutation: re-render immediately (to capture the synchronous
   * `loading = true` the store sets inside `load()`) and again when the returned
   * promise settles (to capture the loaded data + `loading = false`). The store
   * mutates its arrays/counters in place, so a forced re-render is what surfaces
   * the change — there is no reference change to depend on.
   */
  run<T>(result: T): T;
  /** Force a re-render to re-read the store snapshot. */
  refresh(): void;
}

export interface UseSrsStoreOptions {
  /** Drive an initial `gotoPage` on mount (default true). */
  autoload?: boolean;
}

export function useSrsStore(store: SrsStore, opts: UseSrsStoreOptions = {}): SrsStoreController {
  const { autoload = true } = opts;
  const [, force] = useReducer((c: number) => c + 1, 0);

  const run = useCallback(<T,>(result: T): T => {
    force();
    const maybe = result as unknown as { then?: (a: () => void, b: () => void) => void };
    if (maybe && typeof maybe.then === 'function') {
      maybe.then(force, force);
    }
    return result;
  }, []);

  useEffect(() => {
    if (autoload) run(store.gotoPage(store.currentPage || 1));
    // re-run only if the store instance itself changes
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [store]);

  return { store, run, refresh: force };
}
