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

import { get, map, indexOf, groupBy, mapValues } from 'lodash';

import { createSelector } from 'reselect';

import htmlPreview from '../utilities/htmlPreview.jsx';

import { createInstanceSelector } from '../utilities/reduxify.js';
import { selectCurrentUser, selectPreferences, selectCourse } from '../utilities/rootSelectors.js';

import {
  boardsStateSlice,
  postsStateSlice,
  threadsStateSlice,
  jumpersSlice,
  searchSlice,
  viewsSlice,
  discussionsSlice,
  threadsDataSlice,
  postsDataSlice,
} from './sliceNames.js';

import { processNotificationData, processItemClasses } from './services/selectorUtils.js';

export const selectPosts = state => state.api[postsDataSlice];

export const selectThreads = state => state.api[threadsDataSlice];

export const selectPostsState = state => state.ui[discussionsSlice][postsStateSlice];

export const selectThreadsState = state => state.ui[discussionsSlice][threadsStateSlice];

export const selectBoardsState = state => state.ui[discussionsSlice][boardsStateSlice];

export const selectViewsState = state => state.ui[discussionsSlice][viewsSlice];

export const selectJumpersState = state => state.ui[discussionsSlice][jumpersSlice];

export const selectSearchState = state => state.ui[discussionsSlice][searchSlice];

export const selectRouterState = state => state.router;

export const selectPostDetails = createSelector(
  [selectPosts, selectPostsState],
  (posts, postsState) =>
    mapValues(posts, post => {
      const postState = postsState[post.id] || {};

      const isAvailable = !post.inappropriate && !post.removed;

      const isUnread = !(post.track && post.track.viewed) && isAvailable;

      return {
        ...post,
        ...postState,
        isUnread,
        isAvailable,
        contentPreview: htmlPreview(post.content),
        isThread: post.depth === 0,
      };
    })
);

export const selectPostDetailsWithUserInfo = createSelector(
  [selectPostDetails, selectCurrentUser, selectRouterState, selectCourse],
  (posts, currentUser, router, course) =>
    mapValues(posts, post => {
      const isCurrentUserPost = post.author.handle === currentUser.handle;
      const itemClasses = processItemClasses(post);
      const stateParams = router.searchParams;
      const notification = processNotificationData(stateParams);
      const isAvailable = post.isAvailable || currentUser.id === post.userId;
      return {
        ...post,
        isAvailable,
        isCurrentUserPost,
        isUnread: post.isUnread && (!isCurrentUserPost || post.depth === 0),
        canPinPost: post.depth === 0 && currentUser.isInstructor,
        classes: {
          ...itemClasses,
          'by-current-user': isCurrentUserPost,
          reported: notification
            ? notification.id === post.id && notification.inappropriate
            : false,
        },
        editable: !course.isEnded && post.editable && !post.removed,
        showContent: post.isAvailable || currentUser.isInstructor,
      };
    })
);

const selectPostDetailsWithUserInfoByThread = createSelector(
  [selectPostDetailsWithUserInfo],
  posts => groupBy(posts, 'threadId')
);

export const selectThreadsWithChildren = createSelector(
  [selectThreads, selectThreadsState, selectPostDetailsWithUserInfoByThread],
  (threads, threadsState, postsByThread) =>
    mapValues(threads, thread => {
      const threadState = threadsState[thread.id] || {};
      const threadPosts = postsByThread[thread.id];
      const {
        rootPostGroup,
        replies = [],
        orphans = [],
      } = groupBy(threadPosts, post =>
        indexOf(threadState.replies, post.id) !== -1
          ? 'replies'
          : post.depth > 0
            ? 'orphans'
            : 'rootPostGroup'
      );

      const rootPost = rootPostGroup[0];

      const expandReplies =
        typeof threadState.expandReplies === 'boolean'
          ? threadState.expandReplies
          : rootPost.expandBody;

      const repliesCount = thread.postCount - 1;

      return {
        ...thread,
        ...threadState,
        rootPost,
        replies,
        orphans,
        repliesCount,
        repliesRemaining: repliesCount - replies.length,
        unreadRepliesCount: thread.unreadPostCount,
        newRepliesCount: thread.newPostCount,
        isChildrenVisible: rootPost.expandBody && expandReplies,
      };
    })
);

export const selectViewDetails = createSelector(
  [selectViewsState, selectThreadsWithChildren],
  (views, threads) =>
    mapValues(views, view => {
      const inViewThread = threads[view.inViewThreadId];

      if (!inViewThread) {
        return view;
      }

      const rootPost = {
        ...inViewThread.rootPost,
        expandBody: true,
      };

      rootPost.classes = {
        ...rootPost.classes,
        ...processItemClasses(rootPost),
      };

      return {
        ...view,
        inViewThread: {
          ...inViewThread,
          isChildrenVisible: true,
          rootPost: rootPost,
        },
      };
    })
);

export const selectSearchDetails = createSelector(
  [selectSearchState, selectPostDetailsWithUserInfo],
  (searchStates, posts) =>
    mapValues(searchStates, search => {
      const searchResultPosts = map(search.searchResultIds, id => posts[id]);
      return {
        ...search,
        searchResultPosts,
      };
    })
);

export const selectBoardStates = createSelector(
  [selectBoardsState, selectViewsState, selectRouterState, selectPreferences, selectCourse],
  (boards, views, router, preferences, course) =>
    mapValues(boards, (board, discussionId) => {
      const displayView = views[discussionId] && views[discussionId].displayView;
      const inViewThreadId = views[discussionId] && views[discussionId].inViewThreadId;
      const viewInfo = views[discussionId] && views[discussionId].viewInfo;
      const stateParams = router.searchParams;
      const showJumpBar = preferences && preferences.discussionJumpBar;
      const courseEnded = course.isEnded;

      const notification = processNotificationData(stateParams);
      return {
        ...board,
        showJumpBar,
        displayView,
        viewInfo,
        inViewThreadId,
        stateParams,
        notification,
        readyToLoadThreads: !!board.lastVisitedTime,
        courseEnded,
      };
    })
);

export const selectBoardThreads = createSelector(
  [selectBoardsState, selectThreadsWithChildren],
  (boards, threadsWithChildren) =>
    mapValues(boards, board => {
      const boardThreads = map(board.threads.list, threadId => threadsWithChildren[threadId]);

      return {
        lastVisitedTime: board.lastVisitedTime,
        order: board.order,
        threadsLoaded: board.threads.list.length,
        threadsTotal: board.threads.filterCount,
        threadsRemaining: board.threads.filterCount - board.threads.list.length,
        threads: boardThreads,
      };
    })
);

export const selectDiscussionJumpers = createSelector(
  [selectJumpersState, selectPostDetailsWithUserInfo],
  (discussionsJumpersState, posts) =>
    mapValues(discussionsJumpersState, jumpersState => {
      return mapValues(jumpersState, jumper => {
        const currentPostIndex = jumper.currentPostId
          ? jumper.list.indexOf(jumper.currentPostId)
          : 0;
        const currentPostId = jumper.currentPostId || jumper.list[currentPostIndex];

        const prevId = jumper.list[currentPostIndex - 1];
        const nextId = jumper.list[currentPostIndex + 1];

        return {
          ...jumper,
          loadedCount: jumper.list.length,
          userId: jumper.userId,
          currentPostId,
          currentPostIndex,
          currentPost: posts[currentPostId],
          prevPost: prevId && posts[prevId],
          nextPost: nextId && posts[nextId],
        };
      });
    })
);

export const createJumperSelector = (discussionId, jumperKey) =>
  createSelector(
    [selectDiscussionJumpers, selectCurrentUser, selectBoardsState],
    (discussionsJumpers, currentUser, boards) => {
      const data = get(discussionsJumpers, `[${discussionId}][${jumperKey}]`, {});
      const board = boards[discussionId];
      const user = data.user || currentUser;
      return {
        ...data,
        lastVisitedTime: board?.lastVisitedTime,
        userHandle: user.handle,
        userId: user.id,
        userName: user.fullName,
        isSelf: user.id === currentUser.id,
      };
    }
  );

export const createBoardSelector = discussionId =>
  createInstanceSelector(selectBoardStates, discussionId);
export const createThreadsSelector = discussionId =>
  createInstanceSelector(selectBoardThreads, discussionId);
export const createViewSelector = discussionId =>
  createInstanceSelector(selectViewDetails, discussionId);
export const createSearchSelector = discussionId =>
  createInstanceSelector(selectSearchDetails, discussionId);
