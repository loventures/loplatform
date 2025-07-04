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

import { orderBy, filter, mapValues, compact, map } from 'lodash';
import { createSelector } from 'reselect';

import { CONTENT_TYPE_LESSON } from '../utilities/contentTypes.ts';
import { selectContentItemsWithNebulousDetails } from './selectContentItemsWithNebulousDetails.ts';
import { CourseState } from '../loRedux';

export const selectCompetencies = (state: CourseState) => state.api.competencies;

export const competenciesLinkedContentSelector = (state: CourseState) =>
  state.api.competenciesLinkedContentItems;

export const linkedContentStateByCompetencySelector = (state: CourseState) =>
  state.ui.linkedContentStateByCompetency;
export const linkedContentGradeStateByCompetencySelector = (state: CourseState) =>
  state.ui.linkedContentGradeStateByCompetency;
export const linkedContentProgressStateByCompetencySelector = (state: CourseState) =>
  state.ui.linkedContentProgressStateByCompetency;

export const competencyLinkedContentStateSelectorCreator = competencyId =>
  createSelector(
    [linkedContentStateByCompetencySelector],
    byCompetency => byCompetency[competencyId]
  );
export const competencyLinkedContentGradeStateSelectorCreator = competencyId =>
  createSelector(
    [linkedContentGradeStateByCompetencySelector],
    byCompetency => byCompetency[competencyId]
  );

export const competencyLinkedContentProgressStateSelectorCreator = competencyId =>
  createSelector(
    [linkedContentProgressStateByCompetencySelector],
    byCompetency => byCompetency[competencyId]
  );

export const competenciesByContentSelector = state => state.api.competenciesByContent;

export const selectContentRelationsByCompetency = state => state.api.contentRelationsByCompetency;

export const selectViewableRelatedContentsByCompetency = createSelector(
  selectContentRelationsByCompetency,
  selectContentItemsWithNebulousDetails,
  (contentRelationsByCompetency, contentItems) => {
    return mapValues(contentRelationsByCompetency, contentRelations => {
      const relatedContents = compact(map(contentRelations, contentId => contentItems[contentId]));
      return orderBy(
        filter(relatedContents, c => c.typeId != CONTENT_TYPE_LESSON),
        'learningPathIndex'
      );
    });
  }
);
