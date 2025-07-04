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

import { EdgePath, UUID } from '../api/commonTypes';
import Course from '../bootstrap/course';
import { loConfig } from '../bootstrap/loConfig';
import {
  ContentWithAncestors,
  lpResource,
  useFlatLearningPathResource,
} from './LearningPathResource';
import { queryClient } from './queryClient';
import { Resource, StrictUrlParamsKey, useSuspenseQuery } from './Resource';
import { useCourseSelector } from '../loRedux';
import { ContentWithNebulousDetails } from '../courseContentModule/selectors/contentEntry';
import { selectContentItemsWithNebulousDetails } from '../selectors/selectContentItemsWithNebulousDetails';
import { CONTENT_TYPE_LESSON } from '../utilities/contentTypes';
import { QueryFunction } from 'react-query';

import User from '../bootstrap/user';
import { Tree, mapWithContext, toList } from '../instructorPages/customization/Tree';
import { courseReduxStore } from '../loRedux';

export type Competency = {
  id: number;
  nodeName: UUID;
  title: string;
  level: number;
};

export type CourseCompetency = {
  competency: Competency;
  relations: Array<EdgePath>;
};

export type CompetencyStructure = {
  competencyStructure: Array<Tree<number>>;
  competencies: Array<CourseCompetency>;
};

export type CompetencyWithRelations = Competency & {
  relations: Array<ContentWithNebulousDetails>;
  ancestors: Competency[];
  hasChildren: boolean;
};

/**
 * React-Query Resource for CompetencyStructure
 * */
class CompetencyResource<
  TData extends CompetencyStructure,
  TKey extends StrictUrlParamsKey<typeof loConfig.competencyStatus.competencies>,
> extends Resource<TData, [number], TKey> {
  urlTemplate = loConfig.competencyStatus.competencies;

  getKey(contextId: number): TKey {
    return [
      {
        contextId: +contextId,
      },
      this.urlTemplate,
    ] as TKey;
  }

  pushToRedux(data: TData, config: Record<string, any> = {}) {
    if (data && config.redux) {
      console.log('nothing to do here');
    }
  }

  fetch(key: TKey, config: Record<string, any> = {}) {
    return queryClient.fetchQuery(key, this.fetcher(config));
  }

  read(contextId: number, config?: Record<string, any>) {
    const key = this.getKey(contextId);
    const promise = this.fetch(key, config);
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }

  getFlattenedTree(contextId: number, config?: Record<string, any>) {
    const { promise } = this.read(contextId, config);
    return promise.then(structure => {
      return lpResource.getFlatLearningPath(Course.id, User.id).then(learningPathFlat => {
        // Bleh, why do I need this monstrosity?
        const content = selectContentItemsWithNebulousDetails(courseReduxStore.getState());
        return computeFlattenedTrees(learningPathFlat, content)(structure);
      });
    });
  }

  isFetching(contextId: number) {
    return queryClient.isFetching(this.getKey(contextId));
  }

  prefetch(key: TKey, fetcher: QueryFunction<TData, TKey>) {
    return queryClient.prefetchQuery(key, fetcher);
  }
}

const competencyResource = new CompetencyResource();

export default competencyResource;

export const useCompetencyResource = (contextId = Course.id) => {
  const key = competencyResource.getKey(contextId);
  const fetcher = competencyResource.fetcher({ redux: true });
  return useSuspenseQuery(key, fetcher, { select: data => data });
};

export const useFlattenedCompetencyTree = (contextId = Course.id) => {
  const key = competencyResource.getKey(contextId);
  const fetcher = competencyResource.fetcher({ redux: true });
  competencyResource.prefetch(key, fetcher);

  const content = useCourseSelector(selectContentItemsWithNebulousDetails);

  const learningPathFlat = useFlatLearningPathResource();

  return useSuspenseQuery(key, fetcher, {
    select: computeFlattenedTrees(learningPathFlat, content),
  });
};

/******* Private Methods *******/

function computeFlattenedTrees(
  learningPathFlat: ContentWithAncestors[],
  content: Record<string, ContentWithNebulousDetails>
) {
  return (structure: CompetencyStructure) => {
    const { competencies, competencyStructure } = structure;

    const contentOrder: Record<string, number> = {};
    // Everything is awful. Is this truly all we have for in-order traversal?
    learningPathFlat.forEach((content, index) => {
      contentOrder[content.id] = index;
    });

    function populateNode(
      competencyId: number,
      ancestors: CompetencyWithRelations[],
      children: number[]
    ): CompetencyWithRelations {
      const { competency, relations } = competencies.find(cc => cc.competency.id === competencyId)!;

      const populatedRelations = relations
        .map(r => content[r])
        .filter(content => content.typeId !== CONTENT_TYPE_LESSON)
        .sort((a, b) => contentOrder[a.id] - contentOrder[b.id]);

      return {
        ...competency,
        relations: populatedRelations,
        ancestors,
        hasChildren: Boolean(children.length),
      };
    }

    const populatedTrees =
      competencyStructure?.map(c => {
        const populatedTree = mapWithContext(c, populateNode);
        return populatedTree;
      }) ?? [];

    const filteredTrees = populatedTrees.filter(p => Boolean(findNodeWithRelations(p)));

    return filteredTrees.flatMap(toList);
  };
}

/**
 * Searches depth-first for a node with non-zero relations. Returns the first node
 * found. This is used to filter out top-level competencies that have no links to content
 * in their entire tree.
 * */
function findNodeWithRelations<A extends { relations: any[] }>(tree: Tree<A>): A | undefined {
  const hasRels = tree.value.relations.length > 0;
  if (hasRels) {
    return tree.value;
  } else {
    for (const child of tree.children) {
      const hasOwnRels = findNodeWithRelations(child);
      if (hasOwnRels) {
        return child.value;
      }
    }
  }
}
