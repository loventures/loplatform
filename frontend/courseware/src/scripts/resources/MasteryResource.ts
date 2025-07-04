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

import { ContentIdentifier } from '../api/commonTypes';
import Course from '../bootstrap/course';
import { loConfig } from '../bootstrap/loConfig';
import { queryClient } from './queryClient';
import { Resource, StrictUrlParamsKey, useSuspenseQuery } from './Resource';
import { sortCompetencyListActionCreator } from '../studentPages/courseCompetenciesPage/actions/listActions';
import { indexOf, keyBy, map, mapValues } from 'lodash';
import { createDataListUpdateMergeAction } from '../utilities/apiDataActions';
import { QueryFunction } from 'react-query';
import { courseReduxStore } from '../loRedux';

import User from '../bootstrap/user';
import { Competency } from './CompetencyResource';

export type CourseMastery = {
  competencies: Array<Competency>;
  mastered: Array<number>;
  relations: Record<number, Array<ContentIdentifier>>;
};

/**
 * React-Query Resource for Mastery
 * */
class MasteryResource<
  TData extends CourseMastery,
  TKey extends StrictUrlParamsKey<typeof loConfig.competencyStatus.mastery>,
> extends Resource<TData, [number, number], TKey> {
  urlTemplate = loConfig.competencyStatus.mastery;

  getKey(contextId: number, userId: number): TKey {
    return [
      {
        contextId: +contextId,
        viewAs: +userId,
      },
      this.urlTemplate,
    ] as TKey;
  }

  pushToRedux(data: TData, config: Record<string, any> = {}) {
    if (data && config.redux) {
      const { competencies, relations, mastered } = data;
      if (competencies) {
        courseReduxStore.dispatch(
          createDataListUpdateMergeAction('competencies', keyBy(competencies, 'id'))
        );
      }
      const contentRelationsByCompetency = mapValues(relations, contentIdentifiers =>
        map(contentIdentifiers, stripContext)
      );
      if (contentRelationsByCompetency) {
        courseReduxStore.dispatch(
          createDataListUpdateMergeAction(
            'contentRelationsByCompetency',
            contentRelationsByCompetency
          )
        );
      }
      if (mastered) {
        courseReduxStore.dispatch(
          createDataListUpdateMergeAction('competenciesMasteryStatusByUser', {
            [config.viewingAsId]: mastered,
          })
        );
      }
      const initialListSort = {
        property: 'title',
        order: 'asc',
        naturally: true,
      };
      courseReduxStore.dispatch(sortCompetencyListActionCreator(initialListSort));
    }
  }

  fetch(key: TKey, config: Record<string, any> = {}) {
    return queryClient.fetchQuery(key, this.fetcher(config));
  }

  read(contextId: number, userId: number, config?: Record<string, any>) {
    const key = this.getKey(contextId, userId);
    const promise = this.fetch(key, config);
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }

  isFetching(contextId: number, userId: number) {
    return queryClient.isFetching(this.getKey(contextId, userId));
  }

  prefetch(key: TKey, fetcher: QueryFunction<TData, TKey>) {
    return queryClient.prefetchQuery(key, fetcher);
  }
}

const masteryResource = new MasteryResource();

export default masteryResource;

export const useMasteryResource = (contextId = Course.id, userId = User.id) => {
  const key = masteryResource.getKey(contextId, userId);
  const fetcher = masteryResource.fetcher({ redux: true, viewingAsId: userId });
  return useSuspenseQuery(key, fetcher);
};

export const useMasteryStatus = (userId = User.id, contextId = Course.id) => {
  const key = masteryResource.getKey(contextId, userId);
  const fetcher = masteryResource.fetcher({ redux: true, viewingAsId: userId });
  return useSuspenseQuery(key, fetcher, {
    select: masteryForUser => masteryForUser.mastered,
  });
};

///// private

const stripContext = (contentIdentifier: string) =>
  indexOf(contentIdentifier, '.') > -1 ? contentIdentifier.split('.')[1] : contentIdentifier;
