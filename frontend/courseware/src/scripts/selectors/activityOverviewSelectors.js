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

import { groupBy, values } from 'lodash';
import { createSelector } from 'reselect';
import { createInstanceSelector } from '../utilities/reduxify.js';

import { selectRouter } from '../utilities/rootSelectors.js';
import { selectContentItems } from './contentItemSelectors.js';
import { InstructorAssignmentOverviewPageLink } from '../utils/pageLinks.js';

export const selectActivityOverviewContentId = createSelector(selectRouter, router => {
  const match = InstructorAssignmentOverviewPageLink.match(router.path);
  return match && match.params.contentId;
});

const selectActivityOverviewByUserByContent = state => state.api.activityOverviewByUserByContent;
const selectActivityOverviewLoadingStateByContent = state =>
  state.ui.activityOverviewLoadingStateByContent;
export const selectActivityOverviewSendMessageLoadingState = state =>
  state.ui.activityOverviewSendMessageLoadingState;

export const selectPageActivityOverviewLoadingState = createInstanceSelector(
  selectActivityOverviewLoadingStateByContent,
  selectActivityOverviewContentId,
  {}
);

const selectPageActivityOverviewByUser = createInstanceSelector(
  selectActivityOverviewByUserByContent,
  selectActivityOverviewContentId,
  {}
);

const selectOverviewContent = createInstanceSelector(
  selectContentItems,
  selectActivityOverviewContentId
);

export const selectPageActivityOverviewSplit = createSelector(
  [selectPageActivityOverviewByUser, selectActivityOverviewContentId, selectOverviewContent],
  (activityOverview, contentId, content) => {
    const { withWork = [], withoutWork = [] } = groupBy(values(activityOverview), overview => {
      return overview.hasValidViewableAttempts ? 'withWork' : 'withoutWork';
    });

    return {
      contentId,
      content,
      withWork,
      withoutWork,
    };
  }
);
