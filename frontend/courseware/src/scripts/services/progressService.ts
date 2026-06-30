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

import { currentUser } from '../utilities/currentUserData.ts';
import { CallAggregator } from '../utilities/pure/callAggregator.ts';
import { request } from '../utilities/request.ts';
import { makeProgressService } from './pure/progressService.ts';

/**
 * Native (axios) ProgressService singleton for React/redux callers — replaces
 * `lojector.get('ProgressService')`. Built from the pure `makeProgressService`
 * with the native `request` (axios) and the pure `CallAggregator` class (which
 * defaults to the native runtime). Mirrors `submissionActivityAPI.ts`.
 *
 * The current user's `getId()` / `recordActivity()` come from the pure
 * `currentUser` singleton (utilities/currentUserData.ts).
 */
export const progressService = makeProgressService(request, CallAggregator, currentUser);

export default progressService;
