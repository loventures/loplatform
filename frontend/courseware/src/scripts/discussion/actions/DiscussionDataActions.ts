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

export const createThreadsUpdateAction = (threads: any, discussionId?: any) => ({
  type: 'DATA_LIST_UPDATE_MERGE',
  sliceName: 'discussionThreads',
  data: { list: threads, discussionId },
});

export const createThreadUpdateAction = (thread: any, discussionId?: any) => ({
  type: 'DATA_ITEM_UPDATE',
  sliceName: 'discussionThreads',
  id: thread.id,
  data: { item: thread, discussionId },
});

export const createPostsUpdateAction = (posts: any, discussionId?: any) => ({
  type: 'DATA_LIST_UPDATE_MERGE',
  sliceName: 'discussionPosts',
  data: { list: posts, discussionId },
});

export const createPostUpdateAction = (post: any, discussionId?: any) => ({
  type: 'DATA_ITEM_UPDATE',
  sliceName: 'discussionPosts',
  id: post.id,
  data: { item: post, discussionId },
});
