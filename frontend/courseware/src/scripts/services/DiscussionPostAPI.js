/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

import { map, mapValues, find, pickBy, identity, isNumber } from 'lodash';
import { toContentIdentifierForContext } from '../utilities/contentIdentifier.js';
import { forPartialResults, withTransform } from '../utilities/apiResults.js';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';

export default angular.module('lo.service.DiscussionPostAPI', []).service('DiscussionPostAPI', [
  'Request',
  function (Request) {
    const service = {};

    const toContentIdentifier = toContentIdentifierForContext(Course.id);

    const createAttachmentUrl = (
      contentId,
      postId,
      attachmentId,
      download = false,
      thumbnail = false
    ) => {
      let params = {
        postId,
        attachmentId,
        download,
      };
      if (thumbnail) {
        params.size = 'medium';
      }
      let discussion = toContentIdentifier(contentId);

      const url = new UrlBuilder(loConfig.discussionPost.attachment, params, {
        discussion,
      });

      return url.toString();
    };

    service.loadPosts = function (contentId, query) {
      var url = new UrlBuilder(
        loConfig.discussionPost.list,
        {},
        {
          ...query,
          discussion: toContentIdentifier(contentId),
        }
      );

      return Request.promiseRequest(url, 'get')
        .then(forPartialResults)
        .then(withTransform(posts => map(posts, post => service.toPost(post, contentId))));
    };

    service.loadNewPosts = function (contentId, query) {
      var url = new UrlBuilder(
        loConfig.discussionPost.listNew,
        {},
        {
          ...query,
          discussion: toContentIdentifier(contentId),
        }
      );

      return Request.promiseRequest(url, 'get')
        .then(forPartialResults)
        .then(withTransform(posts => map(posts, post => service.toPost(post, contentId))));
    };

    service.loadUnreadPosts = function (contentId, query) {
      var url = new UrlBuilder(
        loConfig.discussionPost.listUnread,
        {},
        {
          ...query,
          discussion: toContentIdentifier(contentId),
        }
      );

      return Request.promiseRequest(url, 'get')
        .then(forPartialResults)
        .then(withTransform(posts => map(posts, post => service.toPost(post, contentId))));
    };

    service.loadUnrespondedPosts = function (contentId, query) {
      var url = new UrlBuilder(
        loConfig.discussionPost.listUnresponded,
        {},
        {
          ...query,
          discussion: toContentIdentifier(contentId),
        }
      );

      return Request.promiseRequest(url, 'get')
        .then(forPartialResults)
        .then(withTransform(posts => map(posts, post => service.toPost(post, contentId))));
    };

    service.loadUserPosts = function (contentId, userHandle, query) {
      var url = new UrlBuilder(
        loConfig.discussionPost.listUserPosts,
        { userHandle },
        {
          ...query,
          discussion: toContentIdentifier(contentId),
        }
      );

      return Request.promiseRequest(url, 'get')
        .then(forPartialResults)
        .then(withTransform(posts => map(posts, post => service.toPost(post, contentId))));
    };

    service.getOnePost = function (contentId, postId) {
      return service
        .loadPosts(contentId, {
          postIds: [postId],
        })
        .then(posts => find(posts, { id: postId }));
    };

    service.loadJumpbarSummaryPosts = function (contentId, includeTypes = {}) {
      const url = new UrlBuilder(
        loConfig.discussionBoard.jumpbar,
        {
          discussion: toContentIdentifier(contentId),
        },
        {
          ...includeTypes,
        }
      );

      return Request.promiseRequest(url, 'get').then(jumpbarSummary => {
        const trimmedSummary = pickBy(jumpbarSummary, identity);
        return mapValues(trimmedSummary, jumper => ({
          ...jumper,
          partialResults: map(jumper.partialResults, post => service.toPost(post, contentId)),
        }));
      });
    };

    //@TODO re-evaluate after CBLPROD-16391
    service.toPost = (post, contentId) => {
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
        attachments: map(post.attachmentIds, attachmentId => {
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
    service.toThread = rootPost => {
      return {
        rootPost: rootPost,
        postCount: rootPost.descendantCount + 1,
        newPostCount: rootPost.newDescendantCount || 0,
        unreadPostCount: rootPost.unreadDescendantCount || 0,
        id: rootPost.id,
      };
    };

    const fixOrderProp = ({ property }) => {
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

    service.loadThreads = function (contentId, query, lastVisitedTime) {
      return service
        .loadPosts(contentId, {
          ...query,
          orders: map(query.orders, order => {
            return { ...order, property: fixOrderProp(order) };
          }),
          toDepth: 0,
          previousVisit: lastVisitedTime,
          includeCounts: true,
        })
        .then(withTransform(threads => map(threads, thread => service.toThread(thread))));
    };

    service.getOneThread = function (contentId, threadId) {
      return service
        .loadThreads(contentId, {
          postIds: [threadId],
        })
        .then(threads => find(threads, { id: threadId }));
    };

    service.loadReplies = function (contentId, threadId, offset, limit) {
      let queryConfig = {
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

    service.searchPosts = function (contentId, searchFor) {
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
  },
]);
