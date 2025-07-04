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

import { filter, get, map, orderBy } from 'lodash';
import { selectContentItemRelations } from '../../../courseContentModule/selectors/contentEntrySelectors';
import { createListSelectorFromSelectors } from '../../../list/createListSelectors';
import { selectContentItems } from '../../../selectors/contentItemSelectors';
import { selectProgressByContentByUser } from '../../../selectors/progressSelectors';
import {
  CONTENT_TYPE_LESSON,
  CONTENT_TYPE_MODULE,
  CONTENT_TYPE_UNIT,
} from '../../../utilities/contentTypes';
import { selectCourse, selectRouter } from '../../../utilities/rootSelectors';
import { createSelector, createStructuredSelector } from 'reselect';

const selectUsers = state => state.api.users;
const selectCompactHeaders = state => state.ui.progressReport.compactHeaders.status;

const selectPageContentItemId = createSelector(
  selectRouter,
  router => router.searchParams.contentId
);

export const selectProgressReportListState = state => state.ui.progressReport.listState;

export const selectProgressReportLoaderComponent = state => ({
  loadingState: state.ui.progressReport.listState.status,
});

export const selectProgressReportListStateComponent = createListSelectorFromSelectors(
  selectProgressReportListState,
  selectUsers
);

const selectProgressReportBreadcrumbContents = createSelector(
  selectPageContentItemId,
  selectContentItems,
  selectContentItemRelations,
  (contentId, contentItems, contentItemRelations) => {
    if (!contentId) {
      return [];
    }
    return orderBy(
      map(contentItemRelations[contentId].ancestors, id => contentItems[id]),
      'depth'
    ).concat(contentItems[contentId]);
  }
);

const selectProgressReportContents = createSelector(
  selectPageContentItemId,
  selectContentItems,
  selectContentItemRelations,
  (contentId, contentItems, contentItemRelations) => {
    const contents = contentId
      ? map(contentItemRelations[contentId].elements, id => contentItems[id])
      : filter(contentItems, { depth: 1 });
    return orderBy(contents, 'index');
  }
);

const canDrill = content =>
  content.typeId === CONTENT_TYPE_UNIT ||
  content.typeId === CONTENT_TYPE_MODULE ||
  content.typeId === CONTENT_TYPE_LESSON;

const selectProgressReportTableHeaders = createSelector(
  selectProgressReportListStateComponent,
  selectProgressReportContents,
  (learnerList, contents) => {
    return map(contents, content => {
      return {
        id: content.id,
        name: content.name,
        canDrill: canDrill(content),
      };
    });
  }
);

const selectProgressReportTableRows = createSelector(
  selectProgressReportListStateComponent,
  selectProgressReportContents,
  selectProgressByContentByUser,
  (learnerList, contents, progressByContentByUser) => {
    return map(learnerList.list, learner => {
      return {
        learner,
        cells: map(contents, content => {
          return get(progressByContentByUser, [learner.id, content.id]);
        }),
      };
    });
  }
);

export const selectProgressReportTableComponent = createStructuredSelector({
  headers: selectProgressReportTableHeaders,
  rows: selectProgressReportTableRows,
  compactHeaders: selectCompactHeaders,
});

export const selectProgressReportBreadcrumbsComponent = createSelector(
  selectProgressReportBreadcrumbContents,
  selectCourse,
  (contents, course) => {
    const root = [{ id: '', name: course.name }];
    return {
      breadcrumbs: root.concat(map(contents, content => ({ id: content.id, name: content.name }))),
    };
  }
);

export const selectProgressReportCompactToggleComponent = createStructuredSelector({
  compactHeaders: selectCompactHeaders,
});
