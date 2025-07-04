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

import { ContentTypeAndName } from '../../api/contentsApi.ts';
import { map, mapValues } from 'lodash';
import { ContentWithNebulousDetails } from './contentEntry';
import {
  ContentRelations,
  ContentRelationsMap,
  ContentThinnedWithLearningIndex,
} from '../../utilities/contentResponse.ts';
import { CONTENT_TYPE_MODULE } from '../../utilities/contentTypes.ts';

const learningPathCompareFn = (
  a: ContentThinnedWithLearningIndex,
  b: ContentThinnedWithLearningIndex
) => a.learningPathIndex - b.learningPathIndex;

export type ContentWithRelationships<T extends string = string> = ContentWithNebulousDetails &
  ContentRelations<ContentWithNebulousDetails> &
  ContentTypeAndName<T>;

export type ModuleWithRelationships = ContentWithNebulousDetails &
  ContentRelations<ContentWithNebulousDetails & { elements: ContentWithNebulousDetails[] }>;

/*TODO: rename to hydrateContentRelations(andAddToContent)*/
export const assembleActivityView = (
  rootItem: ContentWithNebulousDetails,
  contentRelations: ContentRelationsMap,
  contentItems: Record<string, ContentThinnedWithLearningIndex>
): ContentWithRelationships => {
  const relationsForRoot = contentRelations[rootItem.id];
  const relatedContents = mapValues(relationsForRoot, relation => {
    if (Array.isArray(relation)) {
      return relation
        .map(id => contentItems[id])
        .filter(c => c)
        .sort(learningPathCompareFn);
    } else {
      return contentItems[relation as string];
    }
  }) as unknown as ContentRelations<ContentWithNebulousDetails>;
  return {
    ...rootItem,
    ...relatedContents,
  };
};

export const assembleModuleView = (
  rootItem: ContentWithNebulousDetails,
  contentRelations: ContentRelationsMap,
  contentItems: Record<string, ContentThinnedWithLearningIndex>
): ModuleWithRelationships => {
  const view = assembleActivityView(rootItem, contentRelations, contentItems);

  /** Here we hydrate the child elements of each child in the module. But only with elements.
   *  We don't go deeper into the rest of the contentRelations. why do this? it's hard to type.
   * */
  view.elements = map(view.elements, element => {
    const elementRelation = contentRelations[element.id] || { ancestors: [] };
    const elementElements = map(elementRelation.elements, elementId => contentItems[elementId]);
    const merged = {
      ...element,
      elements: elementElements,
    };
    return merged;
  });

  return view as ModuleWithRelationships;
};
export const assembleContentView = (
  rootItem: ContentWithNebulousDetails,
  contentRelations: ContentRelationsMap,
  contentItems: Record<string, ContentThinnedWithLearningIndex>
): ModuleWithRelationships | ContentWithRelationships => {
  if (rootItem.typeId === CONTENT_TYPE_MODULE) {
    return assembleModuleView(rootItem, contentRelations, contentItems);
  } else {
    return assembleActivityView(rootItem, contentRelations, contentItems);
  }
};
