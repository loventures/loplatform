/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import { Location } from 'history';
import { pick } from 'lodash';
import { history } from '../utilities/history';
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

// Replacement for history v4's removed `LocationDescriptorObject<S>`. react-router v6 / history v5
// keep navigation `state` separate from the path target, but our link builders still bundle it here;
// LoLink and the imperative nav helpers split `state` back out when handing off to v6.
export type LoLocationDescriptor<S = FromApp> = {
  pathname?: string;
  search?: string;
  hash?: string;
  state?: S;
};

export const getSearchParams = () => {
  return selectRouter(courseReduxStore.getState()).searchParams;
};

export const getRoleSegment = () => {
  // Read the current path from the history singleton, which is always defined and current —
  // the redux router mirror can lag (or be momentarily undefined) during a redirect.
  return /\/instructor\//.test(history.location.pathname) ? '/instructor' : '/student';
};

export const redirectPreserveParams = (pathname: string, location: Location) => {
  const search = qs.stringify(pick(qs.parse(location.search.slice(1)), searchParamsToPreserve));
  return { pathname, search };
};

export const createLink = (
  path: string,
  searchParams = {},
  hash = ''
): LoLocationDescriptor<FromApp> => {
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

export const location2String = (location: LoLocationDescriptor): string =>
  `${location.pathname}${location.search}${location.hash}`;
