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
import { QueryClient, QueryClientProvider, QueryObserver } from 'react-query';
import { EnsuredQueryKey } from 'react-query/types/core/types';

/**
 * Top-level query client. We only have one for now. We could have more
 * to shard our caches if we start doing a great job of lazy loading sections of the app.
 * */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: Infinity,
      queryFn: context => {
        /** Our default fetcher using axios. You can also always pass in a custom fetcher.
         *  Resource uses it's own fetcher so it can push things to redux. */
        const [template, params] = context.queryKey as EnsuredQueryKey<
          [string, Record<string, any>]
        >;
        return axios.get(createUrl(template, params)).then(({ data }) => data);
      },
      retry: (failureCount, error) => {
        const isForbidden = axios.isAxiosError(error) ? error.response?.status === 403 : false;
        return failureCount < 3 && !isForbidden;
      },
    },
  },
});

/**
 * TODO: understand how query observers could help us with mutations and usage outside of react.
 *       Basic concept is that it will notify angular if a resource is updated triggering $digest.
 * */
const getQueryObserver = (key: any) => new QueryObserver(queryClient, { queryKey: key });

export { queryClient, QueryClientProvider, getQueryObserver };
