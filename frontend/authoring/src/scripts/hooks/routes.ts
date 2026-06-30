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

import { matchPath } from 'react-router';

import { parseQueryParams } from '../router/ReactRouterService';
import allPaths from '../router/routes';
import { DcmState } from '../types/dcmState';
import { useDcmSelector } from './index';

export const selectRouterPathVariable = (name: string) => (state: DcmState) => {
  const pathname = state.router.location.pathname;
  // v6 matchPath takes (pattern, pathname), `end` replaces `exact`, and no longer accepts an array
  // of patterns — so match each in turn and take the first full match.
  for (const path of allPaths) {
    const match = matchPath({ path, end: true }, pathname);
    if (match) return match.params[name];
  }
  return undefined;
};

export const useRouterPathVariable = (name: string): string | undefined =>
  useDcmSelector(selectRouterPathVariable(name));

export const useNumericRouterPathVariable = (name: string): number | undefined => {
  const value = useDcmSelector(selectRouterPathVariable(name));
  return value ? parseInt(value) : undefined;
};

export const selectRouterQueryParam = (name: string) => (state: DcmState) => {
  const search = state.router.location.search;
  const params = parseQueryParams(search);
  return params[name] as string | undefined;
};

export const useRouterQueryParam = (name: string): string | undefined =>
  useDcmSelector(selectRouterQueryParam(name));

export const useNumericRouterQueryParam = (name: string): number | undefined => {
  const value = useDcmSelector(selectRouterQueryParam(name));
  return value != null ? parseInt(value) : undefined;
};
