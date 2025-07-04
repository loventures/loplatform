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

import { ContentLite } from '../api/contentsApi';
import Course from '../bootstrap/course';
import { loConfig } from '../bootstrap/loConfig';
import { queryClient } from './queryClient';
import { Resource, useSuspenseQuery } from './Resource';
import { createDataListUpdateMergeAction } from '../utilities/apiDataActions';
import { HtmlPart } from '../utilities/assetTypes';
import {
  CONTENT_TYPE_COURSE_LINK,
  CONTENT_TYPE_LTI,
  CONTENT_TYPE_RESOURCE,
} from '../utilities/contentTypes';
import { courseReduxStore } from '../loRedux';

import { Course as CourseT } from '../../loPlatform';
import { UrlAndParamsKey } from './ContentsResource';

// the type ids supported by this class
type GenericTypeId = CONTENT_TYPE_LTI | CONTENT_TYPE_RESOURCE | CONTENT_TYPE_COURSE_LINK;

export type LtiAssetInfo = {
  instructions: {
    parts: [HtmlPart];
    renderedHtml: string;
    partType: 'block';
  };
};

export type CourseLinkAssetInfo = {
  instructions: {
    parts: [HtmlPart];
    renderedHtml: string;
    partType: 'block';
  };
  newWindow: boolean;
  branch: number | undefined;
};

export type ResourceAssetInfo = {
  resourceType: 'readingMaterial' | 'audioEmbed' | 'videoEmbed';
  embedCode: string;
  instructions: {
    parts: [HtmlPart];
    renderedHtml: string;
    partType: 'block';
  };
  resourceFileName?: string;
  resourceUrl: string;
};

type GenericAssetInfos = {
  [CONTENT_TYPE_LTI]: LtiAssetInfo;
  [CONTENT_TYPE_RESOURCE]: ResourceAssetInfo;
  [CONTENT_TYPE_COURSE_LINK]: CourseLinkAssetInfo;
};

class GenericAssetInfoResource<TypeId extends GenericTypeId> extends Resource<
  GenericAssetInfos[TypeId],
  [CourseT, ContentLite<TypeId>],
  UrlAndParamsKey
> {
  urlTemplate = loConfig.contentItem.courseAssetInfo;

  getKey(course: CourseT, content: ContentLite<TypeId>): UrlAndParamsKey {
    return [
      {
        context: course.id,
        path: content.id,
      },
      this.urlTemplate,
    ];
  }

  pushToRedux(data: GenericAssetInfos[TypeId], config: Record<string, any> = {}) {
    if (data) {
      const action = createDataListUpdateMergeAction('assetInfoByContent', {
        [config.contentId]: data,
      });
      courseReduxStore.dispatch(action);
    }
  }

  fetch(key: UrlAndParamsKey, config: Record<string, any> = {}) {
    return queryClient.fetchQuery(key, this.fetcher(config));
  }

  read(course: CourseT, content: ContentLite<TypeId>, config?: Record<string, any>) {
    const key = this.getKey(course, content);
    const promise = this.fetch(key, config);
    const data = queryClient.getQueryData<GenericAssetInfos[TypeId]>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }

  isFetching(course: CourseT, content: ContentLite<TypeId>) {
    return queryClient.isFetching(this.getKey(course, content));
  }
}

type GenericAssetInfoResourceMap = { [K in GenericTypeId]: GenericAssetInfoResource<K> };

const genericAssetInfoResources: GenericAssetInfoResourceMap = {
  [CONTENT_TYPE_LTI]: new GenericAssetInfoResource<CONTENT_TYPE_LTI>(),
  [CONTENT_TYPE_RESOURCE]: new GenericAssetInfoResource<CONTENT_TYPE_RESOURCE>(),
  [CONTENT_TYPE_COURSE_LINK]: new GenericAssetInfoResource<CONTENT_TYPE_COURSE_LINK>(),
} as const;

export const useAssetInfo = <T extends GenericTypeId>(
  content: ContentLite<T>,
  course: CourseT = Course // Course.commitId
) => {
  const assetInfoResource = genericAssetInfoResources[
    content.typeId
  ] as GenericAssetInfoResource<T>; // Ugh.
  const key = assetInfoResource.getKey(course, content);
  const fetcher = assetInfoResource.fetcher({ redux: true, contentId: content.name });
  return useSuspenseQuery(key, fetcher);
};
