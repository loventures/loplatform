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

import { request } from '../utilities/request.ts';
import { makeDiscussionSummaryAPI } from './pure/discussionSummaryAPI.ts';

/**
 * Native (axios) DiscussionSummaryAPI for React callers — replaces `lojector.get('DiscussionSummaryAPI')`.
 * `getSummary` returns the discussion's per-user post counts (roster-level, instructor-facing), so the
 * native `request` (no X-UserId) is equivalent to the Angular `Request` adapter — impersonation-irrelevant.
 */
export const discussionSummaryAPI = makeDiscussionSummaryAPI((url, method, ...rest) =>
  request.promiseRequest(url, method, ...rest)
);
