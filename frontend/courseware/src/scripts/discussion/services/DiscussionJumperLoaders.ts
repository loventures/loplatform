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

import { forEach } from 'lodash';

import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';
import discussionOrders from './discussionOrders.js';
import { nativeQ as $q } from '../../utilities/nativeQ.ts';

/**
 * Pure TS port of the AngularJS `DiscussionJumperLoaders` service: builds the
 * jumper view-type → loader map plus `getLoader`/`getSummaryLoader`, all routed
 * through the pure (axios) `discussionBoardAPI` singleton (replacing the injected
 * Angular `DiscussionBoardAPI`). The `$q` runtime is the native drop-in.
 */
const loadUserPosts = (discussionId: any, limit: any, offset: any, { userHandle }: any) =>
  discussionBoardAPI.loadUserPosts(discussionId, userHandle, {
    limit,
    offset,
    order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
  });

const loadNewPosts = (discussionId: any, limit: any, offset: any, { lastVisitedTime }: any) =>
  discussionBoardAPI.loadNewPosts(discussionId, {
    limit,
    offset,
    order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
    previousVisit: lastVisitedTime,
  });

const loadUnreadPosts = (discussionId: any, limit: any, offset: any) =>
  discussionBoardAPI.loadUnreadPosts(discussionId, {
    limit,
    offset,
    order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
  });

const loadBookmarkedPosts = (discussionId: any, limit: any, offset: any) =>
  discussionBoardAPI.loadBookmarkedPosts(discussionId, {
    limit,
    offset,
    order: discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC,
  });

const loadUnrespondedThreads = (discussionId: any, limit: any, offset: any) =>
  discussionBoardAPI.loadUnrespondedPosts(discussionId, {
    limit,
    offset,
    order: [discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC],
  });

export const discussionJumperLoaders: any = {
  'user-posts': loadUserPosts,
  new: loadNewPosts,
  unread: loadUnreadPosts,
  bookmarked: loadBookmarkedPosts,
  unresponded: loadUnrespondedThreads,
};

discussionJumperLoaders.getSummaryLoader = (includeTypes: any) => {
  return (discussionId: any, lastVisitedTime: any, userHandle: any) => {
    const includeMap: any = {};
    forEach(includeTypes, (includeType: any) => (includeMap[includeType.postType] = true));
    if (includeMap['newPosts']) {
      includeMap['newPosts'] = lastVisitedTime;
    }
    if (includeMap['userPosts']) {
      includeMap['userPosts'] = userHandle;
    }

    return discussionBoardAPI.loadJumpbarSummaryPosts(discussionId, includeMap);
  };
};

discussionJumperLoaders.getLoader = (viewType: any) => {
  const loader = discussionJumperLoaders[viewType];

  if (!loader) {
    console.error('no loader for this jumper:', viewType);
    return () => $q.when([]);
  }

  return loader;
};
