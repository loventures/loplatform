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

import { Course } from '../../../loPlatform';
import { groupBy, map, orderBy, pickBy } from 'lodash';
import { ContentWithNebulousDetails } from './contentEntry';
import { ContentRelationsMap } from '../../utilities/contentResponse.ts';

export type CourseWithRelationships = Course & {
  elements?: ContentWithNebulousDetails[];
  discussions?: ContentWithNebulousDetails[];
};

export const assembleCourseRelations = (
  contentItems: Record<string, ContentWithNebulousDetails>,
  contentRelations: ContentRelationsMap
) => {
  const firstLevel = pickBy(contentItems, c => c.depth === 1);
  const modules: ContentWithNebulousDetails[] = map(firstLevel, content => {
    const relations = contentRelations[content.id];
    return {
      ...content,
      elements: relations?.elements ?? [],
    };
  });
  const courseContents = orderBy(modules, 'index');

  const courseContentsByGroup = groupBy(courseContents, c => c.logicalGroup) as unknown as {
    elements: ContentWithNebulousDetails[];
    discussions: ContentWithNebulousDetails[];
  };
  return {
    ...courseContentsByGroup,
  };
};
