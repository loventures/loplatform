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

import { createSelector, createStructuredSelector } from 'reselect';
import { createInstanceSelector } from '../../../utilities/reduxify';
import { selectRouter } from '../../../utilities/rootSelectors';
import { selectLocalListDataAndState } from '../../../list/createLocalListSelectors';
import { createListStateSelector } from '../../../list/createListSelectors';

import { InstructorAssignmentOverviewPageLink } from '../../../utils/pageLinks';

import { CONTENT_TYPE_OBSERVATION_ASSESSMENT } from '../../../utilities/contentTypes';

import {
  selectActivityOverviewContentId,
  selectPageActivityOverviewSplit,
  selectPageActivityOverviewLoadingState,
  selectActivityOverviewSendMessageLoadingState,
} from '../../../selectors/activityOverviewSelectors';

const selectPageContentId = createSelector(selectRouter, router => {
  const match = InstructorAssignmentOverviewPageLink.match(router.path);
  return match && match.params.contentId;
});
const selectPageContentItem = createInstanceSelector(
  state => state.api.contentItems,
  selectPageContentId
);

export const selectActivityOverviewLoaderComponent = createStructuredSelector({
  loadingState: selectPageActivityOverviewLoadingState,
});

export const selectOverviewModalComponent = createStructuredSelector({
  contentId: selectActivityOverviewContentId,
  content: selectPageContentItem,
  loadingState: selectActivityOverviewSendMessageLoadingState,
  modalState: state => state.ui.activityOverviewMessageModalState,
});

const selectActivityOverviewWithWorkListState = createListStateSelector(
  createInstanceSelector(
    state => state.ui.activityOverviewWithWorkListStateByContent,
    selectActivityOverviewContentId
  )
);

const selectActivityOverviewWithoutWorkListState = createListStateSelector(
  createInstanceSelector(
    state => state.ui.activityOverviewWithoutWorkListStateByContent,
    selectActivityOverviewContentId
  )
);

export const selectActivityOverviewPageComponent = createSelector(
  selectPageContentId,
  selectPageContentItem,
  selectPageActivityOverviewSplit,
  selectActivityOverviewWithWorkListState,
  selectActivityOverviewWithoutWorkListState,
  (contentId, content, { withWork, withoutWork }, withWorkState, withoutWorkState) => {
    const withWorkList = selectLocalListDataAndState(withWorkState, withWork);
    const withoutWorkList = selectLocalListDataAndState(withoutWorkState, withoutWork);

    return {
      contentId,
      content,
      isObservation: content && content.typeId === CONTENT_TYPE_OBSERVATION_ASSESSMENT,
      withWork: withWorkList.list,
      withWorkState: withWorkList.listState,
      withoutWork: withoutWorkList.list,
      withoutWorkState: withoutWorkList.listState,
    };
  }
);
