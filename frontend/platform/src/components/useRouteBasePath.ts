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

import { useContext } from 'react';
import { UNSAFE_RouteContext as RouteContext } from 'react-router-dom';

/**
 * The pathname where the current route is mounted, excluding the current route's own splat (`*`) —
 * the v5 `match.url` / the v6 value of `useResolvedPath('.')`.
 *
 * react-router v7 enables `v7_relativeSplatPath`, so `useResolvedPath('.')` now includes the
 * matched splat; code that used it as a stable base for child links/redirects then compounds the
 * splat. We use the current match's `pathnameBase` instead, which the router already computes as
 * "the matched pathname up to but excluding this route's splat". Crucially this is per-match, so —
 * unlike stripping `useParams()['*']` — it does NOT over-strip a leaf route (e.g. `:userId`) that
 * merely sits under a splat-route ancestor (that bug swapped nested breadcrumbs).
 */
export const useRouteBasePath = (): string => {
  const { matches } = useContext(RouteContext);
  const match = matches[matches.length - 1];
  return match?.pathnameBase || '/';
};

export default useRouteBasePath;
