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

import { createSelector } from 'reselect';
import { createLoaderComponent } from '../utilities/withLoader.js';

import { selectCurrentUserId } from '../utilities/rootSelectors.js';

import { loadContentPlayerActionCreator } from '../courseContentModule/actions/contentPageLoadActions.js';

export const selectPageContentLoaderComponent = createSelector(
  selectCurrentUserId,
  state => state.ui.contentPlayerLoadingState,
  (currentUserId, contentPlayerLoadingState) => {
    return {
      currentUserId,
      loaderKey: currentUserId,
      loadingState: contentPlayerLoadingState[currentUserId] || {},
    };
  }
);

const PageContentLoader = createLoaderComponent(
  selectPageContentLoaderComponent,
  ({ currentUserId }) => loadContentPlayerActionCreator(currentUserId),
  'PageContentLoader'
);

export default PageContentLoader;
