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

import { Content, buildContentTree } from '../api/contentsApi';
import Course from '../bootstrap/course';
import User from '../bootstrap/user';
import { Tree } from '../instructorPages/customization/Tree';
import contentsResource, { UrlAndParamsKey } from './ContentsResource';
import { queryClient } from './queryClient';
import { Resource, useSuspenseQuery } from './Resource';
import {
  CONTENT_TYPE_LESSON,
  CONTENT_TYPE_MODULE,
  CONTENT_TYPE_UNIT,
} from '../utilities/contentTypes';
import { EnsuredQueryKey, QueryFunctionContext } from 'react-query/types/core/types';

export type ContentWithAncestors = Content & {
  lesson?: ContentWithAncestors;
  module?: ContentWithAncestors;
  unit?: ContentWithAncestors;
};

export type ModulePath = {
  content: ContentWithAncestors;
  elements: ContentWithAncestors[];
  effectiveElements: ContentWithAncestors[]; // this allows us to give units and top-level content an effective elements list without screwing up the sidebar
};
export type LearningPathContent = {
  // These are not just modules.
  // * You will find Unit with no elements, but with effectiveElements
  // * You will find Module with elements and effectiveElements
  // * You will find Content with no elements and themselves as effectiveElements
  //   - These contents are the top-lever children of units
  modules: ModulePath[]; // These include empty Units, Unit Modules, Modules
};

type LPKey = EnsuredQueryKey<[Record<string, any>, string, 'learningPath']>;

// these should be recursive, not crazy typed
const toModule = (module: Tree<Content>): ModulePath => {
  if (module.value.typeId === CONTENT_TYPE_MODULE) {
    // mutate the lesson into the content because progress is achieved by mutating the content items in the
    // incoming tree so if i make copies i will no longer see progress
    const elements = module.children.flatMap<ContentWithAncestors>(
      ({ value: content, children }) => {
        if (content.typeId === CONTENT_TYPE_LESSON) {
          const elements = children.map<ContentWithAncestors>(element => element.value);
          elements.forEach(element => {
            element.lesson = content;
          });
          return elements;
        } else {
          return [content];
        }
      }
    );
    elements.forEach(element => {
      element.module = module.value;
    });
    return {
      content: module.value,
      elements: elements,
      effectiveElements: elements,
    };
  } else {
    return {
      // top-level contents in the unit
      content: module.value,
      elements: [],
      effectiveElements: [module.value],
    };
  }
};

/**
 * React-Query Resource for Learning Path as derived from contents.
 * */
class LearningPathResource<TData extends LearningPathContent> extends Resource<
  TData,
  [number, number],
  LPKey
> {
  getKey(context: number, user: number): LPKey {
    const ct = contentsResource.getKey(context, user);
    return [ct[0], ct[1], 'learningPath'];
  }

  fetcher =
    (config: Record<string, any>) =>
    ({ queryKey }: QueryFunctionContext<LPKey>): Promise<TData> => {
      const contentsKey: UrlAndParamsKey = [queryKey[0], queryKey[1]];
      return contentsResource.fetch(contentsKey, config).then(({ objects: contentList }) => {
        /**
         * Using the tree construct to retrieve a depth-first ordering. We remove lessons cuz
         * they aren't really content.
         *
         * TODO: mutation - What happens when we move forward, how do we ensure these internal values change?
         *
         * */
        const tree = buildContentTree(contentList);
        const modules: ModulePath[] = tree.children.flatMap(moduleOrUnit => {
          if (moduleOrUnit.value.typeId === CONTENT_TYPE_UNIT) {
            const modules = moduleOrUnit.children.map(toModule);
            modules.forEach(({ content, effectiveElements }) => {
              content.unit = moduleOrUnit.value;
              effectiveElements.forEach(element => {
                element.unit = moduleOrUnit.value;
              });
            });
            const unit: ModulePath = {
              content: moduleOrUnit.value,
              elements: [],
              effectiveElements: modules.flatMap(m => m.effectiveElements),
            };
            return [unit, ...modules];
          } else {
            return [toModule(moduleOrUnit)];
          }
        });
        return { modules } as TData;
      });
    };

  read(courseId: number, userId: number) {
    const key = this.getKey(courseId, userId);
    const promise = queryClient.fetchQuery(key, this.fetcher({ redux: true }));
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }

  getFlatLearningPath(courseId: number, userId: number) {
    const { promise } = this.read(courseId, userId);
    return promise.then((data: LearningPathContent) =>
      data.modules.flatMap(module => module.elements)
    );
  }
}

export const lpResource = new LearningPathResource();

export const useLearningPathResource = (userId: number = User.id, courseId: number = Course.id) => {
  const key = lpResource.getKey(courseId, userId);
  const fetcher = lpResource.fetcher({ redux: true });
  return useSuspenseQuery(key, fetcher);
};

export const useFlatLearningPathResource = (
  userId: number = User.id,
  courseId: number = Course.id
): ContentWithAncestors[] => {
  const key = lpResource.getKey(courseId, userId);
  const fetcher = lpResource.fetcher({ redux: true });
  // Units have their contents in the learning path too. This is pretty awful.
  return useSuspenseQuery(key, fetcher, {
    select: learningPath =>
      learningPath.modules.flatMap(module =>
        module.content.typeId === CONTENT_TYPE_UNIT ? [] : module.effectiveElements
      ),
  });
};
