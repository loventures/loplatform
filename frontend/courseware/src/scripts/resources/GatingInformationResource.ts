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

import { Content, ContentGate, ContentId, GatingInformation } from '../api/contentsApi';
import Course from '../bootstrap/course';
import contentsResource, { UrlAndParamsKey } from './ContentsResource';
import { queryClient } from './queryClient';
import { LOCKED_STATUS, OPEN_STATUS, READ_ONLY_STATUS } from '../utilities/gatingPolicyConstants';
import { EnsuredQueryKey, QueryFunctionContext } from 'react-query/types/core/types';

import { Resource, useSuspenseQuery } from './Resource';

export type GatingInformationMap = Record<ContentId, GatingInfoWithComputedProperties>;

type IntermediateGatingInfo = GatingInformation & {
  name: string;
  isOpen: boolean;
  isLocked: boolean;
  isReadOnly: boolean;
  parents: string[];
};

export type GatingInfoWithComputedProperties = IntermediateGatingInfo & {
  isGated: boolean;
  selfGated: boolean; // does this specific content have a gate on it
  allGates: ContentGate[];
};

type GIKey = EnsuredQueryKey<[Record<string, any>, string, 'gatingInfo']>;

/**
 * Approximates the gating information shapes built in the gatingInformationSelectors.
 * This resource consumes ContentsResource to compute the values. We do this to get
 * suspension behavior but these resources act as selectors.
 * */
class GatingInformationResource<TData extends GatingInformationMap> extends Resource<
  TData,
  [number, number],
  GIKey
> {
  getKey(context: number, user: number): GIKey {
    const ct = contentsResource.getKey(context, user);
    return [ct[0], ct[1], 'gatingInfo'];
  }

  fetcher =
    (_: Record<string, any>) =>
    ({ queryKey }: QueryFunctionContext<GIKey>): Promise<TData> => {
      // @_@
      const contentsKey: UrlAndParamsKey = [queryKey[0], queryKey[1]];
      return contentsResource.fetch(contentsKey).then(({ objects: contentList }) => {
        const intermediateGatingInfos = getIntermediateGatingInfos(contentList);
        return getEffectiveAvailability(intermediateGatingInfos) as TData;
      });
    };

  read(courseId: number, userId: number) {
    const key = this.getKey(courseId, userId);
    const promise = queryClient.fetchQuery(key, this.fetcher({}));
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, data, fetching, key };
  }
}

export const gatingInformationResource = new GatingInformationResource();

/**
 *  NOTE: We must assert the return value as a Success. Since we use suspense, we know
 *  data is available. We could be in refetch but the hook returns stale data in that case.
 **/
export const useContentGatingInfoResource = (
  contentId: string,
  userId: number,
  courseId = Course.id
) => {
  const key = gatingInformationResource.getKey(courseId, userId);
  const fetcher = gatingInformationResource.fetcher({});
  return useSuspenseQuery(key, fetcher, {
    select: data => data[contentId],
  });
};

export const useAllGatingInfoResource = (userId: number, courseId = Course.id) => {
  const key = gatingInformationResource.getKey(courseId, userId);
  const fetcher = gatingInformationResource.fetcher({});
  return useSuspenseQuery(key, fetcher);
};

/********** Private ********/

function getIntermediateGatingInfos(contentList: Content[]) {
  const contentInfos = contentList.reduce<Record<string, { name: string; typeId: string }>>(
    (acc, { id, name, typeId }) => ((acc[id] = { name, typeId }), acc),
    {}
  );
  // doing some precomputations on the current gating info.
  return contentList.reduce<Record<string, IntermediateGatingInfo>>((acc, content) => {
    const { gatingInformation, id, name } = content;

    const isCourseEnded = Course.hasEnded;

    const isOpen = gatingInformation.gateStatus === OPEN_STATUS && !isCourseEnded;
    const isLocked = gatingInformation.gateStatus === LOCKED_STATUS;
    const isReadOnly = gatingInformation.gateStatus === READ_ONLY_STATUS || isCourseEnded;

    const parents = content.path.split('/').slice(0, -2);

    // absolute hell copy-paste so that assignment gates have the assignment name stuck in for i18n of the gate info
    // see policyMessages.js#19
    const addAssignments = (gv: ContentGate) => {
      const { activityGatingPolicy: agp } = gv;
      const activityGates1 =
        !!agp &&
        !!agp.gates &&
        agp.gates.map(g => ({
          ...g,
          ...contentInfos[g.assignmentId],
        }));
      const agp1 = { ...agp, gates: activityGates1 || [] };
      return { ...gv, activityGatingPolicy: agp1 || { gates: [] } };
    };
    const thisGate = gatingInformation.gate && addAssignments(gatingInformation.gate);

    acc[id] = {
      gateStatus: gatingInformation.gateStatus,
      gate: thisGate,
      name,
      isOpen,
      isLocked,
      isReadOnly,
      parents,
    };
    return acc;
  }, {});
}

const isNonTrivialGate = (g: ContentGate | null | undefined): g is ContentGate =>
  !!g?.rightsGatingPolicy || !!g?.temporalGatingPolicy || !!g?.activityGatingPolicy?.gates.length;

function getEffectiveAvailability(intermediateGatingInfos: Record<string, IntermediateGatingInfo>) {
  // Using parent gates to determine each content item's effective availability.
  return Object.entries(intermediateGatingInfos).reduce<
    Record<string, GatingInfoWithComputedProperties>
  >((acc, [id, shape]) => {
    const allGateInfos = [...shape.parents, id].map(pid => intermediateGatingInfos[pid]);
    const isOpen = allGateInfos.every(g => g.isOpen);
    const isLocked = allGateInfos.some(g => g.isLocked);
    const isReadOnly = !isLocked && allGateInfos.some(g => g.isReadOnly);
    const onlyGates = allGateInfos.map(g => g.gate).filter(isNonTrivialGate);
    const contentGate = intermediateGatingInfos[id].gate;
    acc[id] = {
      ...shape,
      isOpen,
      isLocked,
      isReadOnly,
      selfGated: isNonTrivialGate(contentGate),
      isGated: onlyGates.length > 0,
      allGates: onlyGates,
    };
    return acc;
  }, {});
}
