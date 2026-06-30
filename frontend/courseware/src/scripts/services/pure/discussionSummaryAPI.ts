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

import { orderBy } from 'lodash';
import { toContentIdentifierForContext } from '../../utilities/contentIdentifier.js';
import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';
import Course from '../../bootstrap/course.ts';

/** The request fn injected by the adapter ($q-backed Request) or native (axios) `request`. */
export type RequestFn = (url: any, method: string, ...rest: any[]) => PromiseLike<any>;

/**
 * Discussion summary (per-user post counts), migrated verbatim from the AngularJS
 * `DiscussionSummaryAPI` service to plain TS taking an injected `request`.
 */
export const makeDiscussionSummaryAPI = (request: RequestFn) => {
  const toContentIdentifier = toContentIdentifierForContext(Course.id);

  return {
    getSummary(discussionId: any) {
      const url = new (UrlBuilder as any)(loConfig.discussionBoard.userPostCount, {
        discussion: toContentIdentifier(discussionId),
      });
      return request(url, 'get').then((data: any) => orderBy(data, 'user.fullName'));
    },
  };
};

export type DiscussionSummaryAPI = ReturnType<typeof makeDiscussionSummaryAPI>;
