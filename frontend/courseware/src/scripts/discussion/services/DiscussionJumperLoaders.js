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

import { forEach } from 'lodash';

import DiscussionBoardAPI from '../../services/DiscussionBoardAPI.js';

import discussionOrders from './discussionOrders.js';

export default angular
  .module('lo.discussion.DiscussionJumperLoaders', [DiscussionBoardAPI.name])
  .service('DiscussionJumperLoaders', [
    'DiscussionBoardAPI',
    '$q',
    function (DiscussionBoardAPI, $q) {
      const loadUserPosts = (discussionId, limit, offset, { userHandle }) =>
        DiscussionBoardAPI.loadUserPosts(discussionId, userHandle, {
          limit,
          offset,
          order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
        });

      const loadNewPosts = (discussionId, limit, offset, { lastVisitedTime }) =>
        DiscussionBoardAPI.loadNewPosts(discussionId, {
          limit,
          offset,
          order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
          previousVisit: lastVisitedTime,
        });

      const loadUnreadPosts = (discussionId, limit, offset) =>
        DiscussionBoardAPI.loadUnreadPosts(discussionId, {
          limit,
          offset,
          order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
        });

      const loadBookmarkedPosts = (discussionId, limit, offset) =>
        DiscussionBoardAPI.loadBookmarkedPosts(discussionId, {
          limit,
          offset,
          order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
        });

      const loadUnrespondedThreads = (discussionId, limit, offset) =>
        DiscussionBoardAPI.loadUnrespondedPosts(discussionId, {
          limit,
          offset,
          order: [discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC],
        });

      const service = {
        'user-posts': loadUserPosts,
        new: loadNewPosts,
        unread: loadUnreadPosts,
        bookmarked: loadBookmarkedPosts,
        unresponded: loadUnrespondedThreads,
      };

      service.getSummaryLoader = includeTypes => {
        return (discussionId, lastVisitedTime, userHandle) => {
          const includeMap = {};
          forEach(includeTypes, includeType => (includeMap[includeType.postType] = true));
          if (includeMap['newPosts']) {
            includeMap['newPosts'] = lastVisitedTime;
          }
          if (includeMap['userPosts']) {
            includeMap['userPosts'] = userHandle;
          }

          return DiscussionBoardAPI.loadJumpbarSummaryPosts(discussionId, includeMap);
        };
      };

      service.getLoader = viewType => {
        const loader = service[viewType];

        if (!loader) {
          console.error('no loader for this jumper:', viewType);
          return () => $q.when([]);
        }

        return loader;
      };

      return service;
    },
  ]);
