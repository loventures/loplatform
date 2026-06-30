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

import { makeRequest } from './pure/request.ts';
import { nativeRuntime } from './pure/requestRuntime.ts';

/**
 * Browser-native (axios) Request, for new TS/React callers and the gradual
 * cutover off the Angular `Request` service. Same API as the AngularJS `Request`
 * (promiseRequest / promiseBuilderRequest / resolve / reject / validate / …) but
 * resolves via native Promises — so it does NOT run inside an Angular digest.
 * Use it from React/redux/react-query code; Angular code keeps injecting the
 * `Request` service (which preserves digest).
 */
export const request = makeRequest(nativeRuntime);

export default request;
