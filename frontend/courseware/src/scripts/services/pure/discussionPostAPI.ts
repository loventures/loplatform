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

import { find, identity, isNumber, map, mapValues, pickBy } from 'lodash';
import Course from '../../bootstrap/course.ts';
import { loConfig } from '../../bootstrap/loConfig.ts';
import { forPartialResults, withTransform } from '../../utilities/apiResults.js';
import { toContentIdentifierForContext } from '../../utilities/contentIdentifier.js';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/**
 * Discussion post/thread API, migrated verbatim from the AngularJS
 * `DiscussionPostAPI` service to plain TS taking the injected `Request`.
 */
export const makeDiscussionPostAPI = (Request: RequestLike) => {
  const toContentIdentifier = toContentIdentifierForContext(Course.id);

  const createAttachmentUrl = (
    contentId: any,
    postId: any,
    attachmentId: any,
    download = false,
    thumbnail = false
  ) => {
    const params: any = {
      postId,
      attachmentId,
      download,
    };
    if (thumbnail) {
      params.size = 'medium';
    }
    const discussion = toContentIdentifier(contentId);

    const url = new (UrlBuilder as any)(loConfig.discussionPost.attachment, params, {
      discussion,
    });

    return url.toString();
  };

  const service: any = {};

  service.loadPosts = function (contentId: any, query: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.list,
      {},
      {
        ...query,
        discussion: toContentIdentifier(contentId),
      }
    );

    return Request.promiseRequest(url, 'get')
      .then(forPartialResults)
      .then(withTransform((posts: any) => map(posts, (post: any) => service.toPost(post, contentId))));
  };

  service.loadNewPosts = function (contentId: any, query: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.listNew,
      {},
      {
        ...query,
        discussion: toContentIdentifier(contentId),
      }
    );

    return Request.promiseRequest(url, 'get')
      .then(forPartialResults)
      .then(withTransform((posts: any) => map(posts, (post: any) => service.toPost(post, contentId))));
  };

  service.loadUnreadPosts = function (contentId: any, query: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.listUnread,
      {},
      {
        ...query,
        discussion: toContentIdentifier(contentId),
      }
    );

    return Request.promiseRequest(url, 'get')
      .then(forPartialResults)
      .then(withTransform((posts: any) => map(posts, (post: any) => service.toPost(post, contentId))));
  };

  service.loadUnrespondedPosts = function (contentId: any, query: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.listUnresponded,
      {},
      {
        ...query,
        discussion: toContentIdentifier(contentId),
      }
    );

    return Request.promiseRequest(url, 'get')
      .then(forPartialResults)
      .then(withTransform((posts: any) => map(posts, (post: any) => service.toPost(post, contentId))));
  };

  service.loadUserPosts = function (contentId: any, userHandle: any, query: any) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionPost.listUserPosts,
      { userHandle },
      {
        ...query,
        discussion: toContentIdentifier(contentId),
      }
    );

    return Request.promiseRequest(url, 'get')
      .then(forPartialResults)
      .then(withTransform((posts: any) => map(posts, (post: any) => service.toPost(post, contentId))));
  };

  service.getOnePost = function (contentId: any, postId: any) {
    return service
      .loadPosts(contentId, {
        postIds: [postId],
      })
      .then((posts: any) => find(posts, { id: postId }));
  };

  service.loadJumpbarSummaryPosts = function (contentId: any, includeTypes: any = {}) {
    const url = new (UrlBuilder as any)(
      loConfig.discussionBoard.jumpbar,
      {
        discussion: toContentIdentifier(contentId),
      },
      {
        ...includeTypes,
      }
    );

    return Request.promiseRequest(url, 'get').then((jumpbarSummary: any) => {
      const trimmedSummary = pickBy(jumpbarSummary, identity);
      return mapValues(trimmedSummary, (jumper: any) => ({
        ...jumper,
        partialResults: map(jumper.partialResults, (post: any) => service.toPost(post, contentId)),
      }));
    });
  };

  //@TODO re-evaluate after CBLPROD-16391
  service.toPost = (post: any, contentId: any) => {
    return {
      ...post,
      user: post.author,
      track: {
        bookmarked: post.bookmarked,
        viewed: post.read,
      },
      editable: post.canEdit,
      lastActivityTime: post.descendantActivity,
      lastModified: post.updateTime,
      attachments: map(post.attachmentIds, (attachmentId: any) => {
        const attachment = post.attachmentInfos[attachmentId];
        return {
          ...attachment,
          viewUrl: createAttachmentUrl(contentId, post.id, attachment.id),
          downloadUrl: createAttachmentUrl(contentId, post.id, attachment.id, true),
          thumbnailUrl: createAttachmentUrl(contentId, post.id, attachment.id, false, true),
        };
      }),
    };
  };

  //@TODO re-evaluate after CBLPROD-16391
  service.toThread = (rootPost: any) => {
    return {
      rootPost: rootPost,
      postCount: rootPost.descendantCount + 1,
      newPostCount: rootPost.newDescendantCount || 0,
      unreadPostCount: rootPost.unreadDescendantCount || 0,
      id: rootPost.id,
    };
  };

  const fixOrderProp = ({ property }: any) => {
    switch (property) {
      case 'pinned':
        return 'pinnedOn';
      case 'createTime':
        return 'created';
      case 'lastActivityTime':
        return 'descendantActivity';
      default:
        return property;
    }
  };

  service.loadThreads = function (contentId: any, query: any, lastVisitedTime: any) {
    return service
      .loadPosts(contentId, {
        ...query,
        orders: map(query.orders, (order: any) => {
          return { ...order, property: fixOrderProp(order) };
        }),
        toDepth: 0,
        previousVisit: lastVisitedTime,
        includeCounts: true,
      })
      .then(withTransform((threads: any) => map(threads, (thread: any) => service.toThread(thread))));
  };

  service.getOneThread = function (contentId: any, threadId: any) {
    return service
      .loadThreads(contentId, {
        postIds: [threadId],
      })
      .then((threads: any) => find(threads, { id: threadId }));
  };

  service.loadReplies = function (contentId: any, threadId: any, offset: any, limit: any) {
    const queryConfig: any = {
      rootPostId: threadId,
      offset,
      orders: [
        {
          property: 'created',
          order: 'desc',
        },
      ],
    };
    if (isNumber(limit)) {
      queryConfig.limit = limit;
    }
    return service.loadPosts(contentId, queryConfig);
  };

  service.searchPosts = function (contentId: any, searchFor: any) {
    return service.loadPosts(contentId, {
      limit: 25,
      searchFor,
      orders: [
        {
          property: 'created',
          order: 'desc',
        },
      ],
    });
  };

  return service;
};

export type DiscussionPostAPI = ReturnType<typeof makeDiscussionPostAPI>;
