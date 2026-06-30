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

import { makeProfileAPI } from './pure/profileAPI.ts';
import { request } from '../utilities/request.ts';
import { nativeRuntime } from '../utilities/pure/requestRuntime.ts';

/**
 * Native (axios) ProfileAPI singleton for the React/redux presence subsystem —
 * replaces `lojector.get('ProfileAPI')`.
 *
 * The pure factory needs an async runtime (http + defer + timeout/cancelTimeout).
 * `request.http(config)` carries the same interceptors (X-CSRF / X-UserId / 403 guard)
 * as the Angular `Request` instance but resolves via native Promises — no digest, which
 * the redux-driven presence consumers do not need. The deferred/timeout pieces come from
 * the shared native runtime ($q.defer / $timeout / $timeout.cancel → native equivalents).
 */
export const profileAPI = makeProfileAPI({
  http: config => request.http(config),
  defer: () => nativeRuntime.defer(),
  timeout: (fn, ms) => setTimeout(fn, ms),
  cancelTimeout: handle => clearTimeout(handle),
});
