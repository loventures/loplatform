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

import { ContentLite } from '../api/contentsApi';
import { courseReduxStore } from '../loRedux';
import { Location, LocationDescriptorObject } from 'history';
import { pick } from 'lodash';
import { selectRouter } from '../utilities/rootSelectors';
import qs from 'qs';

const searchParamsToPreserve = [
  'previewAsUserId', //will be explicitly removed when needed
  'contentItemRoot', //meek mode, will stay in for lifetime of app
];

// intended as a type for the location state for history's LocationDescriptor
export type FromApp = {
  // Whether or not the location comes from in-app navigation.
  // For external navigation, (i.e. browser back/forward buttons, address bar)
  // this state is not set.
  fromApp?: boolean;
};

export const getSearchParams = () => {
  return selectRouter(courseReduxStore.getState()).searchParams;
};

export const getRoleSegment = () => {
  return selectRouter(courseReduxStore.getState()).path.match(/\/instructor\//)
    ? '/instructor'
    : '/student';
};

export const redirectPreserveParams = (pathname: string, location: Location) => {
  const search = qs.stringify(pick(qs.parse(location.search.slice(1)), searchParamsToPreserve));
  return { pathname, search };
};

export const createLink = (
  path: string,
  searchParams = {},
  hash = ''
): LocationDescriptorObject<FromApp> => {
  const newSearch = qs.stringify({
    ...pick(getSearchParams(), searchParamsToPreserve),
    ...searchParams,
  });

  return {
    pathname: path,
    search: newSearch ? '?' + newSearch : '',
    hash,
    state: {
      fromApp: true,
    },
  };
};

export const createLinkWithRole = (path: string, ...args: any[]) => {
  return createLink(getRoleSegment() + path, ...args);
};

export const createCompetencyContentLink = (
  content: ContentLite,
  competencyId: number,
  searchParams = {}
) => {
  const url = `${getRoleSegment()}/competencies/${competencyId}/content/${content.id}`;
  return createLink(url, searchParams);
};

export const createContentLink = (content: { id: string }, searchParams = {}) => {
  const prefix = `${getRoleSegment()}/content/`;

  return createLink(prefix + content.id, {
    anchor: undefined,
    ...searchParams,
  });
};

export const createPrintLink = (content: { id: string }, searchParams = {}) => {
  const prefix = `${getRoleSegment()}/print/`;
  return createLink(prefix + content.id, {
    anchor: undefined,
    ...searchParams,
  });
};

export const createDashboardLink = () => {
  const prefix = `${getRoleSegment()}/dashboard`;
  return createLink(prefix, { anchor: undefined });
};

export const location2String = (location: LocationDescriptorObject): string =>
  `${location.pathname}${location.search}${location.hash}`;
