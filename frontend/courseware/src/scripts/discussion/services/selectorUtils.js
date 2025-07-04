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

import { get } from 'lodash';

import { b64Decode } from '../../utilities/b64Utils.js';

const threadExpansions = post => ({
  'replies-expanded': post.isChildrenVisible,
  'thread-expanded': post.isChildrenVisible,
  collapsed: !post.expandBody,
});

export const processNotificationData = stateParams => {
  try {
    const data = b64Decode(stateParams.data);
    const id = get(data, 'postId');
    const threadId = get(data, 'threadId');
    if (!id && !threadId) {
      return;
    }
    return { id, threadId };
  } catch (e) {
    //ignore garbage data
    return;
  }
};

export const processItemClasses = post => {
  const isThread = post.depth === 0;

  const expansions = isThread ? threadExpansions(post) : {};

  const isAvailable = !post.inappropriate && !post.removed;

  return {
    ...expansions,

    collapsed: !post.expandBody,

    'by-moderator': post.moderatorPost,
    'has-been-edited': post.hasBeenEdited,

    unread: !(post.track && post.track.viewed) && isAvailable,
    new: post.isNew && isAvailable,

    pinned: post.pinned,
    bookmarked: post.track && post.track.bookmarked,
    available: isAvailable,
    inappropriate: post.inappropriate,
    removed: post.removed,
  };
};
