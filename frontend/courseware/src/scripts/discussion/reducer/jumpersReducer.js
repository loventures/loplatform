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

import {
  each,
  uniq,
  concat,
  without,
  groupBy,
  map,
  indexOf,
  intersection,
  transform,
  get,
  filter,
} from 'lodash';

import {
  DISCUSSION_BOARD_JUMPER_SET_JUMPER,
  DISCUSSION_BOARD_JUMPER_SET_POST,
  DISCUSSION_BOARD_JUMPER_SET_USER,
  DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_START,
  DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_SUCCESS,
  DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_ERROR,
  DISCUSSION_BOARD_JUMPER_LOAD_START,
  DISCUSSION_BOARD_JUMPER_LOAD_SUCCESS,
  DISCUSSION_BOARD_JUMPER_LOAD_ERROR,
  DISCUSSION_UPDATE_LAST_VISITED,
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_POST_SET_NOT_NEW,
  DISCUSSION_POST_VIEWED_AUTO_TOGGLED,
  DISCUSSION_POST_VIEWED_MANUAL_TOGGLED,
} from '../actionTypes.js';

const defaultJumperState = {
  list: [],
  loading: false,
  loadedOnce: false,
  loadedCount: 0,
  totalCount: 0,
};

const jumperReducer = (jumper = defaultJumperState, action) => {
  switch (action.type) {
    case DISCUSSION_BOARD_JUMPER_SET_USER:
      return {
        ...jumper,
        //TODO this is temp solution until the user data gets updated for this
        user: action.data.user,
        //TODO have separate slices for different users
        ...defaultJumperState,
      };

    case DISCUSSION_BOARD_JUMPER_SET_POST:
      return {
        ...jumper,
        currentPostId: action.data.postId,
      };

    case DISCUSSION_BOARD_JUMPER_LOAD_START:
      return {
        ...jumper,
        loading: true,
      };

    case DISCUSSION_BOARD_JUMPER_LOAD_SUCCESS:
      if (jumper.loadedOnce && action.jumperType === 'new') {
        // We don't want the count coming from the server after the
        // initial load since its inherently going to be wrong.
        return {
          ...jumper,
          loading: false,
          list: uniq(concat(jumper.list, action.data.list)),
        };
      }
      return {
        ...jumper,
        loading: false,
        list: uniq(concat(jumper.list, action.data.list)),
        totalCount: action.data.totalCount,
        loadedOnce: true,
      };

    case DISCUSSION_BOARD_JUMPER_LOAD_ERROR:
      return {
        ...jumper,
        loading: true,
        error: action.data.error,
        //prevents endless retry
        loadedOnce: true,
      };

    default:
      return jumper;
  }
};

const modifyList = (list, add, remove) =>
  uniq(without(concat(list, map(add, 'id')), ...map(remove, 'id')));

const updatedPostsReducer = (jumper, { add, remove }) => {
  const newList = modifyList(jumper.list, add, remove);
  return {
    ...jumper,
    list: newList,
    //recalibrate totalCount based on the difference between new/old lists
    totalCount: jumper.totalCount + (newList.length - jumper.list.length),
  };
};

const bookmarkedUpdateReducer = (bookmarked = defaultJumperState, action) => {
  if (bookmarked.loading) {
    return bookmarked;
  }
  const updatedPosts = groupBy(action.data.list, post =>
    post.track.bookmarked ? 'add' : 'remove'
  );
  return updatedPostsReducer(bookmarked, updatedPosts);
};

const jumpersUpdateReducer = (
  jumpers = {
    unread: defaultJumperState,
    unresponded: defaultJumperState,
    new: defaultJumperState,
    bookmarked: defaultJumperState,
  },
  action
) => {
  switch (action.type) {
    case DISCUSSION_POST_VIEWED_AUTO_TOGGLED:
      if (action.postIds.length) {
        const notNew = intersection(jumpers.unread.list, action.postIds);
        const newList = without(jumpers.unread.list, ...notNew);
        return {
          ...jumpers,
          unread: {
            ...jumpers.unread,
            list: newList,
            loadedCount: newList.length,
            totalCount: jumpers.unread.totalCount - action.postIds.length,
          },
        };
      } else {
        return jumpers;
      }
    case DISCUSSION_POST_VIEWED_MANUAL_TOGGLED:
      if (action.postIds.length) {
        const notNew = intersection(jumpers.unread.list, action.postIds);
        const newList = without(jumpers.unread.list, ...notNew);
        return {
          ...jumpers,
          unread: {
            ...jumpers.unread,
            list: newList,
            loadedCount: newList.length,
            totalCount:
              jumpers.unread.totalCount +
              (action.data.viewed ? -action.postIds.length : action.postIds.length),
          },
        };
      } else {
        return jumpers;
      }

    case DISCUSSION_POST_SET_NOT_NEW:
      if (action.data.postIds.length) {
        const notNew = intersection(jumpers.new.list, action.data.postIds);
        const newList = without(jumpers.new.list, ...notNew);
        return {
          ...jumpers,
          new: {
            ...jumpers.new,
            list: newList,
            loadedCount: newList.length,
            totalCount: jumpers.new.totalCount - action.data.postIds.length,
          },
        };
      } else {
        return jumpers;
      }

    case DISCUSSION_WRITING_SAVE_SUCCESS:
      if (
        action.threadId &&
        jumpers.unresponded &&
        !jumpers.unresponded.loading &&
        indexOf(jumpers.unresponded.list, action.threadId) !== -1
      ) {
        jumpers = {
          ...jumpers,
          unresponded: {
            ...jumpers.unresponded,
            list: without(jumpers.unresponded.list, action.threadId),
            totalCount: jumpers.unresponded.totalCount - 1,
          },
        };
      }

      if (
        jumpers['user-posts'] &&
        !jumpers['user-posts'].loading &&
        !jumpers['user-posts'].userId
      ) {
        //user jumper is the current user
        jumpers = {
          ...jumpers,
          'user-posts': {
            ...jumpers['user-posts'],
            list: concat(jumpers['user-posts'].list, action.data.newItemId),
            totalCount: jumpers['user-posts'].totalCount + 1,
          },
        };
      }

      return jumpers;

    case 'DATA_LIST_UPDATE_MERGE':
    case 'DATA_ITEM_UPDATE':
      if (action.sliceName === 'discussionPosts') {
        return {
          ...jumpers,
          bookmarked: bookmarkedUpdateReducer(jumpers.bookmarked, action),
        };
      } else {
        return jumpers;
      }

    default:
      return jumpers;
  }
};

const jumpersReducer = (jumpers = {}, action) => {
  switch (action.type) {
    case DISCUSSION_BOARD_JUMPER_SET_JUMPER:
      each(jumpers, (jumper, jumperType) => {
        const shouldBeActive = jumperType === action.data.jumperType;
        if (jumper.active !== shouldBeActive) {
          jumpers = {
            ...jumpers,
            [jumperType]: {
              ...jumpers[jumperType],
              active: shouldBeActive,
            },
          };
        }
      });
      return jumpers;

    case DISCUSSION_BOARD_JUMPER_SET_POST:
    case DISCUSSION_BOARD_JUMPER_SET_USER:
    case DISCUSSION_BOARD_JUMPER_LOAD_START:
    case DISCUSSION_BOARD_JUMPER_LOAD_SUCCESS:
    case DISCUSSION_BOARD_JUMPER_LOAD_ERROR:
      if (!action.jumperType) {
        return jumpers;
      } else {
        return {
          ...jumpers,
          [action.jumperType]: jumperReducer(jumpers[action.jumperType], action),
        };
      }

    case DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_SUCCESS:
      return {
        ...jumpers,
        ...transform(
          action.viewToDataTypes,
          (result, type) => {
            const jumper = action.data[type.postType];
            if (jumper) {
              result[type.jumperType] = {
                loading: false,
                list: filter(map(jumper.partialResults, 'id')),
                totalCount: jumper.total,
                loadedOnce: true,
                currentPostId: get(jumper, 'partialResults[0].id'),
              };
            }
            return result;
          },
          {}
        ),
      };

    case DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_START:
      return {
        ...jumpers,
        ...transform(
          action.viewToDataTypes,
          (result, type) => {
            result[type.jumperType] = {
              ...defaultJumperState,
              ...jumpers[type.jumperType],
              loading: true,
            };
            return result;
          },
          {}
        ),
      };
    case DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_ERROR:
      return {
        ...jumpers,
        ...transform(
          action.viewToDataTypes,
          (result, type) => {
            result[type.jumperType] = {
              ...defaultJumperState,
              ...jumpers[type.jumperType],
              loading: true,
              error: action.data.error,
              //prevents endless retry
              loadedOnce: true,
            };
            return result;
          },
          {}
        ),
      };

    default:
      return jumpers;
  }
};

const discussionsJumpersReducer = (discussionsJumpers = {}, action) => {
  switch (action.type) {
    case DISCUSSION_BOARD_JUMPER_SET_JUMPER:
    case DISCUSSION_BOARD_JUMPER_SET_POST:
    case DISCUSSION_BOARD_JUMPER_SET_USER:
    case DISCUSSION_BOARD_JUMPER_LOAD_START:
    case DISCUSSION_BOARD_JUMPER_LOAD_SUCCESS:
    case DISCUSSION_BOARD_JUMPER_LOAD_ERROR:
    case DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_START:
    case DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_SUCCESS:
    case DISCUSSION_BOARD_JUMPER_SUMMARY_LOAD_ERROR:
      if (!action.discussionId) {
        return discussionsJumpers;
      } else {
        return {
          ...discussionsJumpers,
          [action.discussionId]: jumpersReducer(discussionsJumpers[action.discussionId], action),
        };
      }

    case DISCUSSION_WRITING_SAVE_SUCCESS:
    case DISCUSSION_UPDATE_LAST_VISITED:
    case DISCUSSION_POST_SET_NOT_NEW:
    case DISCUSSION_POST_VIEWED_AUTO_TOGGLED:
    case DISCUSSION_POST_VIEWED_MANUAL_TOGGLED:
    case 'DATA_LIST_UPDATE_MERGE':
    case 'DATA_ITEM_UPDATE':
      if (action.data.discussionId || action.discussionId) {
        let discussionId = action.data.discussionId || action.discussionId;
        return {
          ...discussionsJumpers,
          [discussionId]: jumpersUpdateReducer(discussionsJumpers[discussionId], action),
        };
      } else {
        return discussionsJumpers;
      }

    default:
      return discussionsJumpers;
  }
};

export default discussionsJumpersReducer;
