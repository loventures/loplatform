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
import { nativeRuntime } from '../utilities/pure/callAggregator.ts';
import { request } from '../utilities/request.ts';
import { makeSubmissionActivityAPI } from './pure/submissionActivityAPI.ts';

/**
 * Native (axios) SubmissionActivityAPI for React/redux callers — replaces
 * `lojector.get('SubmissionActivityAPI')`. Resolves via native Promises (no
 * digest), which is correct for its redux action creators and the React
 * attachment-url consumer — none binds the result to an Angular `$scope`.
 *
 * The current-user id comes from the pure `currentUser` singleton
 * (utilities/currentUserData.ts). The object-form `$q.all` becomes the native
 * object-map `all` from pure/callAggregator.
 */
export const submissionActivityAPI = makeSubmissionActivityAPI(request, currentUser, nativeRuntime.all);
