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

import { isNil, map } from 'lodash';
import { toContentIdentifierForContext } from '../utilities/contentIdentifier.js';
import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';
import Course from '../bootstrap/course.js';
export default angular
  .module('lo.service.DiscussionPostStateAPI', [])
  .service('DiscussionPostStateAPI', [
    'Request',
    '$q',
    function (Request, $q) {
      const service = {};

      const toContentIdentifier = toContentIdentifierForContext(Course.id);

      //backward compat with HW discussion
      //bookmarking is always done one post at a time
      //but viewed/unread could be done en-mass
      service.updateTracking = function (contentId, postIds, { bookmarked, viewed } = {}) {
        if (!isNil(bookmarked)) {
          return $q.all(
            map(postIds, id => {
              return service.setBookmarked(contentId, id, bookmarked);
            })
          );
        } else if (!isNil(viewed)) {
          //@TODO batch
          return $q.all(
            map(postIds, id => {
              return service.setViewed(contentId, id, viewed);
            })
          );
        }
      };

      service.setPinned = function (contentId, postId, pinned) {
        const url = new UrlBuilder(
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

      service.setInappropriate = function (contentId, postId, inappropriate) {
        const url = new UrlBuilder(
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

      service.setRemoved = function (contentId, postId, removed) {
        const url = new UrlBuilder(
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

      service.setBookmarked = function (contentId, postId, bookmarked) {
        const url = new UrlBuilder(
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

      service.setViewed = function (contentId, postId, viewed) {
        const url = new UrlBuilder(
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

      service.reportInappropriate = function (contentId, postId, reason) {
        const url = new UrlBuilder(
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
    },
  ]);
