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

import { Content, ContentId, GatingInformation, isUngraded } from '../api/contentsApi.ts';
import {
  each,
  filter,
  find,
  findLast,
  flatMap,
  isNil,
  join,
  keyBy,
  map,
  mapValues,
  omit,
  omitBy,
  orderBy,
  range,
  slice,
  split,
} from 'lodash';
import { COURSE_ROOT } from './courseRootType';
import { EdgePath } from '../api/commonTypes.ts';
import { CONTENT_TYPE_UNIT } from './contentTypes.ts';

export type ContentRelations<T = string> = {
  ancestors: T[];
  elements?: T[];
  discussions?: T[];
  prev?: T;
  next?: T;
};

export type ContentRelationsMap<T = string> = Record<ContentId, ContentRelations<T>>;

export type ContentWithoutGatesGradesProgress = Omit<
  Content,
  'grade' | 'progress' | 'gatingInformation'
>;

export type ContentThinnedWithLearningIndex = ContentWithoutGatesGradesProgress & {
  learningPathIndex?: number;
  unit?: EdgePath; // currently unused
};

//Index contents in tree preorder
//that orders content in the natural learning path order
export const getContentsByLearningPathOrder = (
  contents: Record<string, ContentWithoutGatesGradesProgress>,
  contentRelations: ContentRelationsMap
) => {
  const orderedContents: ContentWithoutGatesGradesProgress[] = [];
  const orderContents = (currentContents: ContentWithoutGatesGradesProgress[], unit?: EdgePath) => {
    each(orderBy(currentContents, 'index'), content => {
      (content as ContentThinnedWithLearningIndex).unit = unit;
      orderedContents.push(content);
      const elements = map(contentRelations[content.id].elements, eId => contents[eId]);
      orderContents(elements, content.typeId === CONTENT_TYPE_UNIT ? content.id : unit);
    });
  };
  const firstLevelElements = filter(contents, { depth: 1 });
  orderContents(firstLevelElements);
  return orderedContents.map((c, idx) => {
    (c as ContentThinnedWithLearningIndex).learningPathIndex = idx;
    return c;
  }) as ContentThinnedWithLearningIndex[];
};

// crazy transforms that happen BEFORE loadedActionsCreator in contentPageLoadActions.js
export const contentsToContentResponse = (data: Content[], studentId: number) => {
  const contentList = data;

  const rawContentsWithCourse = keyBy(contentList, 'id');
  const { grade: overallGrade, progress: overallProgress } = rawContentsWithCourse._root_;

  const rawContents = omitBy(rawContentsWithCourse, c => c.id === COURSE_ROOT);
  const contents: Record<string, ContentWithoutGatesGradesProgress> = mapValues(rawContents, dto =>
    omit(dto, ['grade', 'progress', 'gatingInformation'])
  );

  const inflateGateInfos = (gateInfos: Record<string, GatingInformation>) =>
    mapValues(gateInfos, (info, id) => {
      const parentGates = flatMap(inits(split(id, '_')), pathArr => {
        const parentGate = gateInfos[join(pathArr, '_')];
        return parentGate && parentGate.gate ? [parentGate.gate] : [];
      });
      return { ...info, parentGates };
    });

  const contentUserData = {
    grades: omitBy(mapValues(rawContents, 'grade'), g => {
      return isNil(g) || isUngraded(g) || isNil(g.grade);
    }),
    progress: omitBy(mapValues(rawContents, 'progress'), isNil),
    gatingInformation: inflateGateInfos(omitBy(mapValues(rawContents, 'gatingInformation'), isNil)),
    dueDateExempt: omitBy(mapValues(rawContents, 'dueDateExempt'), isNil),
  };

  const contentRelations = getContentRelations(contents, contentList);

  const contentByLearningPathIndex = getContentsByLearningPathOrder(contents, contentRelations);

  const learningPathById = keyBy(contentByLearningPathIndex, c => c.id);
  const crazyContents: Record<string, ContentThinnedWithLearningIndex> = mapValues(
    contents,
    c => learningPathById[c.id] || c
  );

  const getNextContext = (content: ContentThinnedWithLearningIndex, direction: 1 | -1) => {
    // NOTE: more badness. We didn't add a learningpathindex to the course root but
    //       we just assume we never encounter that.
    const nextIndex = content.learningPathIndex + direction;
    if (content.depth === 1) {
      // TODO: UNIT WTF
      // This block of code handles logic around navigating between modules
      // If on the first module, previous (-1 direction) should go to the root
      // and if on the last module next (1 direction) should go to the root
      if (
        (nextIndex === -1 && direction === -1) ||
        (nextIndex > contentList.length - 1 && direction === 1)
      ) {
        return rawContentsWithCourse._root_;
      }

      const finder = direction === 1 ? find : findLast;
      return finder(contentByLearningPathIndex, { depth: 1 }, nextIndex);
    } else {
      return contentByLearningPathIndex[nextIndex];
    }
  };
  each(contentRelations, (contentRelation: ContentRelations, contentId) => {
    const content = crazyContents[contentId];
    const prev = getNextContext(content, -1);
    const next = getNextContext(content, 1);
    contentRelation.prev = prev && prev.id;
    contentRelation.next = next && next.id;
  });

  return {
    studentId,
    contents: crazyContents,
    contentRelations,
    contentUserData,
    overallGrade,
    overallProgress,
    rawData: data,
  };
};

const getContentRelations = (
  contents: Record<string, ContentWithoutGatesGradesProgress>,
  contentList: Content[]
) => {
  const contentRelations: ContentRelationsMap = mapValues(contents, content => {
    const ancestors = content.path.split('/').slice(0, -2);
    const childrenItems = contentList.filter(c => {
      return c.depth === content.depth + 1 && c.path.match(content.id + '/');
    });
    const childrenByLogicalGroup = childrenItems.reduce<{
      discussions: Content[];
      elements: Content[];
    }>(
      (acc, c) => {
        acc[c.logicalGroup].push(c);
        return acc;
      },
      { elements: [], discussions: [] }
    );

    const childIdsByRelation = mapValues(childrenByLogicalGroup, g => map(g, 'id'));
    return {
      ancestors,
      ...childIdsByRelation,
    };
  });
  return contentRelations;
};

const inits = (arr: any[]) => map(range(arr.length), i => slice(arr, i));
