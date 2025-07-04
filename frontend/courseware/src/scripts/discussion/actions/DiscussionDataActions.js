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

export default angular
  .module('lo.discussionBoard.DiscussionDataActions', [])
  .service('DiscussionDataActions', function DiscussionDataActions() {
    const service = {};

    service.createThreadsUpdateAction = (threads, discussionId) => ({
      type: 'DATA_LIST_UPDATE_MERGE',
      sliceName: 'discussionThreads',
      data: { list: threads, discussionId },
    });

    service.createThreadUpdateAction = (thread, discussionId) => ({
      type: 'DATA_ITEM_UPDATE',
      sliceName: 'discussionThreads',
      id: thread.id,
      data: { item: thread, discussionId },
    });

    service.createPostsUpdateAction = (posts, discussionId) => ({
      type: 'DATA_LIST_UPDATE_MERGE',
      sliceName: 'discussionPosts',
      data: { list: posts, discussionId },
    });

    service.createPostUpdateAction = (post, discussionId) => ({
      type: 'DATA_ITEM_UPDATE',
      sliceName: 'discussionPosts',
      id: post.id,
      data: { item: post, discussionId },
    });

    return service;
  });
