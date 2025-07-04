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

import {
  makeSortActionCreator,
  makeVerbatimSearchActionCreator,
  makePagingActionCreator,
} from '../../../list/makeListActionCreators';

export const sortWithWorkActionCreator = (contentId, sortConfig) =>
  makeSortActionCreator(
    {
      sliceName: 'activityOverviewWithWorkListStateByContent',
      contentId,
    },
    sortConfig
  )();

export const sortWithoutWorkActionCreator = (contentId, sortConfig) =>
  makeSortActionCreator(
    {
      sliceName: 'activityOverviewWithoutWorkListStateByContent',
      contentId,
    },
    sortConfig
  )();

export const searchWithWorkActionCreator = (contentId, searchString, searchConfig) =>
  makeVerbatimSearchActionCreator(
    {
      sliceName: 'activityOverviewWithWorkListStateByContent',
      contentId,
    },
    searchConfig
  )(searchString);

export const searchWithoutWorkActionCreator = (contentId, searchString, searchConfig) =>
  makeVerbatimSearchActionCreator(
    {
      sliceName: 'activityOverviewWithoutWorkListStateByContent',
      contentId,
    },
    searchConfig
  )(searchString);

export const paginateWithWorkActionCreator = (contentId, pageNumber) =>
  makePagingActionCreator({
    sliceName: 'activityOverviewWithWorkListStateByContent',
    contentId,
  })(pageNumber);

export const paginateWithoutWorkActionCreator = (contentId, pageNumber) =>
  makePagingActionCreator({
    sliceName: 'activityOverviewWithoutWorkListStateByContent',
    contentId,
  })(pageNumber);
