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

import axios from 'axios';
import { createUrl } from '../bootstrap/loConfig';
import { queryClient } from './queryClient';
import { QueryFunction, useQuery } from 'react-query';
import {
  EnsuredQueryKey,
  QueryKey,
  QueryObserverSuccessResult,
} from 'react-query/types/core/types';
import { UseQueryOptions } from 'react-query/types/react/types';
import { StrictParamsObject } from '../api/templateTypes';

export type StrictUrlParamsKey<T extends string> = EnsuredQueryKey<[StrictParamsObject<T>, T]>;

/**
 *
 * Resource parent class.
 *
 * There are only two required methods: getKey and read. I suppose even read
 * could have a default implementation.
 *
 * TData is the shape of date you expect out of this resource.
 * TArgs is the arguments needed to build a key, usually [courseId, userId]
 * TKey is the shape of the key, usually [{ ...TArgs }, urlTemplate]
 *
 * The big cheat in this pattern is the "config" object which is generally untyped and
 * used to alter behavior conditionally. This adds complexity but avoids regressions
 * where the angular code was doing things we need to keep doing.
 * */
export abstract class Resource<
  TData,
  TArgs extends unknown[],
  TKey extends EnsuredQueryKey<[Record<string, any>, ...string[]]> = EnsuredQueryKey<
    [Record<string, any>, string]
  >,
> {
  abstract getKey(...args: TArgs): TKey;

  urlTemplate?: string;

  fetch(key: TKey, config?: Record<string, any>): Promise<TData> {
    return queryClient.fetchQuery(key, this.fetcher(config ?? {}));
  }

  pushToRedux?(data: TData, config: Record<string, any>): void;

  refetch?(...args: TArgs): Promise<TData>;

  transform?(key: TKey, data: any): unknown;

  /**
   * Default fetcher accepts a key which is simply param values for createUrl.
   * If your key cannot be sent directly to createUrl you need to implement your
   * own fetcher.
   * @param configObj allows passing of details down. This is an abomination but
   *                  helpful for computing odd redux shapes and dispatching them.
   * @return (QueryFunctionContext) => Promise<TData>
   * */
  fetcher =
    (config: Record<string, any>): QueryFunction<TData, TKey> =>
    context => {
      const [params, template, ..._] = context.queryKey;
      return axios
        .get(createUrl(template, params))
        .then(({ data }) => data as TData)
        .then(data => {
          if (config.redux && this.pushToRedux) {
            this.pushToRedux(data, { ...config, ...params });
          }
          return data;
        });
    };

  invalidate(...args: TArgs): Promise<void> {
    return queryClient.invalidateQueries({ queryKey: this.getKey(...args) });
  }

  /**
   * Uses the url to invalidate every query in the resource.
   * */
  invalidateAll() {
    if (this.urlTemplate) {
      queryClient.invalidateQueries({
        predicate: query => {
          // invalidate all alerts resource queries since we don't know what changed.
          if (query.queryKey.includes(this.urlTemplate!)) {
            return true;
          }
          return false;
        },
      });
    } else {
      throw 'Resource must implement its own invalidateAll.';
    }
  }

  read(...args: TArgs): {
    promise: Promise<TData>;
    data: TData | undefined;
    fetching: number;
    key: TKey;
  } {
    const key = this.getKey(...args);
    const promise = this.fetch(key);
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }
}

export const useSuspenseQuery = <
  TQueryFnData = unknown,
  TError = unknown,
  TData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey,
>(
  queryKey: TQueryKey,
  queryFn: QueryFunction<TQueryFnData, TQueryKey>,
  options: Omit<
    UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
    'queryKey' | 'queryFn'
  > = {}
) => {
  const { data } = useQuery(
    queryKey,
    queryFn,
    Object.assign(options, { suspense: true }) // preserving options instance in case it was memoized.
  ) as QueryObserverSuccessResult<TData>;
  return data;
};
