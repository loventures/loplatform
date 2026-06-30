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

import { isNil, map } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import { toContentIdentifierForContext } from '../../utilities/contentIdentifier.js';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/**
 * Await an array of promises. The Angular adapter passes `$q.all` (so the
 * fan-out of per-post tracking writes settles inside a digest); native callers
 * pass `Promise.all`.
 */
export type AllFn = (promises: PromiseLike<any>[]) => PromiseLike<any[]>;

/**
 * Discussion post state (pin/inappropriate/remove/bookmark/viewed/report) API,
 * migrated verbatim from the AngularJS `DiscussionPostStateAPI` service to plain
 * TS taking the injected `Request` and an `all` runtime for the bulk tracking
 * fan-out.
 */
export const makeDiscussionPostStateAPI = (Request: RequestLike, all: AllFn) => {
  const toContentIdentifier = toContentIdentifierForContext(Course.id);

  const service: any = {};

  //backward compat with HW discussion
  //bookmarking is always done one post at a time
  //but viewed/unread could be done en-mass
  service.updateTracking = function (contentId: any, postIds: any, { bookmarked, viewed }: any = {}) {
    if (!isNil(bookmarked)) {
      return all(
        map(postIds, (id: any) => {
          return service.setBookmarked(contentId, id, bookmarked);
        })
      );
    } else if (!isNil(viewed)) {
      //@TODO batch
      return all(
        map(postIds, (id: any) => {
          return service.setViewed(contentId, id, viewed);
        })
      );
    }
  };

  service.setPinned = function (contentId: any, postId: any, pinned: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.togglePin,
      { postId },
      { discussion: toContentIdentifier(contentId) }
    );

    return Request.promiseRequest(url, 'post', {
      newState: pinned,
    }).then(() => ({
      id: postId,
      pinned,
    }));
  };

  service.setInappropriate = function (contentId: any, postId: any, inappropriate: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.toggleInappropriate,
      { postId },
      { discussion: toContentIdentifier(contentId) }
    );

    return Request.promiseRequest(url, 'post', {
      newState: inappropriate,
    }).then(() => ({
      id: postId,
      inappropriate,
    }));
  };

  service.setRemoved = function (contentId: any, postId: any, removed: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.toggleRemove,
      { postId },
      { discussion: toContentIdentifier(contentId) }
    );

    return Request.promiseRequest(url, 'post', {
      newState: removed,
    }).then(() => ({
      id: postId,
      removed,
    }));
  };

  service.setBookmarked = function (contentId: any, postId: any, bookmarked: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.toggleBookmark,
      { postId },
      { discussion: toContentIdentifier(contentId) }
    );

    return Request.promiseRequest(url, 'post', {
      newState: bookmarked,
    }).then(() => ({
      postId,
      bookmarked,
    }));
  };

  service.setViewed = function (contentId: any, postId: any, viewed: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.toggleViewed,
      { postId },
      { discussion: toContentIdentifier(contentId) }
    );

    return Request.promiseRequest(url, 'post', {
      newState: viewed,
    }).then(() => ({
      postId,
      viewed,
    }));
  };

  service.reportInappropriate = function (contentId: any, postId: any, reason: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.reportInappropriate,
      { postId },
      { discussion: toContentIdentifier(contentId) }
    );

    return Request.promiseRequest(url, 'post', {
      reason,
    }).then(() => ({
      id: postId,
      reason,
    }));
  };

  return service;
};

export type DiscussionPostStateAPI = ReturnType<typeof makeDiscussionPostStateAPI>;
