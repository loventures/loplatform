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

import { SrsList } from '../api/commonTypes';
import { Content } from '../api/contentsApi';
import Course from '../bootstrap/course';
import { loConfig } from '../bootstrap/loConfig';
import User from '../bootstrap/user';
import { queryClient } from './queryClient';
import { Resource, useSuspenseQuery } from './Resource';
import { keyBy } from 'lodash';
import { ProgressUpdates } from '../courseActivityModule/actions/activityActions';
import { loadedActionsCreator } from '../courseContentModule/actions/contentPageLoadActions';
import { contentsToContentResponse } from '../utilities/contentResponse';
import { QueryFunction, QueryKey } from 'react-query';
import { EnsuredQueryKey } from 'react-query/types/core/types';
import { batchActions } from 'redux-batched-actions';
import { courseReduxStore } from '../loRedux';

export type UrlAndParamsKey = EnsuredQueryKey<[Record<string, any>, string]>;

/**
 * React-Query Resource for pristine Course Contents
 * */
class ContentsResource<TData extends SrsList<Content>> extends Resource<
  TData,
  [number, number],
  UrlAndParamsKey
> {
  urlTemplate = loConfig.courseContents.contents;

  pushToRedux(srs: TData, config: Record<string, any> = {}) {
    const intermediateResponse = contentsToContentResponse(srs.objects, config.user || User.id);
    const actions = loadedActionsCreator(intermediateResponse);
    courseReduxStore.dispatch(batchActions([...actions]));
  }

  getKey(context: number, user: number): UrlAndParamsKey {
    return [{ context: +context, user: +user }, this.urlTemplate];
  }

  fetch(key: UrlAndParamsKey, config: Record<string, any> = {}) {
    return queryClient.fetchQuery(key, this.fetcher(config));
  }

  read(courseId: number, userId: number, config?: Record<string, any>) {
    const key = this.getKey(courseId, userId);
    const promise = this.fetch(key, config);
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }

  async refetch(courseId: number, userId: number) {
    const key = this.getKey(courseId, userId);
    await queryClient.refetchQueries(key);
    return this.fetch(key);
  }

  transform(key: UrlAndParamsKey, data: ProgressUpdates): any {
    queryClient.setQueryData<TData | undefined>(key, old => {
      if (old) {
        /* do we want to do mutations? */
        old.objects = old.objects.map(content => {
          content.progress = data.progress[content.id] ?? content.progress;
          return content;
        });
        return old;
      } else {
        console.log('no previous query data found, this might be an error.');
        // invalidate query??
      }
    });
  }

  isFetching(courseId: number, userId: number) {
    return queryClient.isFetching(this.getKey(courseId, userId));
  }

  invalidate(courseId = Course.id, userId = window.lo_platform.user.id): Promise<void> {
    return super.invalidate(courseId, userId);
  }
}

const contentsResource = new ContentsResource();

export default contentsResource;

export const useContentsResource = (userId: number = User.id, courseId: number = Course.id) => {
  const key = contentsResource.getKey(courseId, userId);
  const fetcher = contentsResource.fetcher({ redux: true });
  return useSuspenseQuery(key, fetcher);
};

export const useContentResource = (
  contentId: string,
  userId: number = User.id,
  courseId: number = Course.id
) => {
  const key = contentsResource.getKey(courseId, userId) as QueryKey;
  const fetcher = contentsResource.fetcher({}) as QueryFunction<SrsList<Content>, QueryKey>;
  return useSuspenseQuery(key, fetcher, {
    select: data => {
      const map = keyBy(data.objects, 'id');
      return map[contentId];
    },
  });
};
