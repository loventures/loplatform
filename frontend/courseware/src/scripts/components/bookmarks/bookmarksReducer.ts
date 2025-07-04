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
  addBookmark,
  fetchBookmarks,
  removeBookmark,
} from '../../components/bookmarks/bookmarksApi';
import { CourseState, useCourseSelector } from '../../loRedux';
import { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { Reducer } from 'redux';
import { ThunkAction } from 'redux-thunk';
import { createAction, isActionOf } from 'typesafe-actions';

export type Bookmarks = Record<string, string>;

export type BookmarksState = {
  loading: boolean;
  loaded: boolean;
  bookmarks: Bookmarks;
};

const initialState: BookmarksState = {
  loading: false,
  loaded: false,
  bookmarks: {},
};

const loadingBookmarksAction = createAction('LOADING_BOOKMARKS')();

const loadedBookmarksAction = createAction('LOADED_BOOKMARKS')<Bookmarks>();

const addedBookmarkAction = createAction('ADDED_BOOKMARK')<{
  id: string;
  note: string;
}>();

const removedBookmarkAction = createAction('REMOVED_BOOKMARK')<{
  id: string;
}>();

export const fetchBookmarksAction =
  (): ThunkAction<void, CourseState, any, any> => (dispatch, getState) => {
    const { bookmarks, course } = getState();
    if (!bookmarks.loading && !bookmarks.loaded) {
      dispatch(loadingBookmarksAction());
      fetchBookmarks(course.id).then(bookmarks => dispatch(loadedBookmarksAction(bookmarks)));
    }
  };

export const addBookmarkAction =
  (id: string, note: string): ThunkAction<void, CourseState, any, any> =>
  (dispatch, getState) => {
    const { course } = getState();
    return addBookmark(course.id, id, note).then(() => dispatch(addedBookmarkAction({ id, note })));
  };

export const removeBookmarkAction =
  (id: string): ThunkAction<void, CourseState, any, any> =>
  (dispatch, getState) => {
    const { course } = getState();
    removeBookmark(course.id, id).then(() => dispatch(removedBookmarkAction({ id })));
  };

export const selectBookmarks = (state: CourseState): Bookmarks => state.bookmarks.bookmarks;

export const selectBookmark =
  (id: string) =>
  (state: CourseState): string | undefined =>
    state.bookmarks.bookmarks[id];

export const useBookmarks = (): Bookmarks => {
  const dispatch = useDispatch();
  useEffect(() => {
    dispatch(fetchBookmarksAction());
  }, []);
  return useCourseSelector(selectBookmarks);
};

export const bookmarksReducer: Reducer<BookmarksState> = (state = initialState, action) => {
  if (isActionOf(loadingBookmarksAction, action)) {
    return {
      ...state,
      loading: true,
    };
  } else if (isActionOf(loadedBookmarksAction, action)) {
    return {
      loading: false,
      loaded: true,
      bookmarks: action.payload,
    };
  } else if (isActionOf(addedBookmarkAction, action)) {
    return {
      ...state,
      bookmarks: { ...state.bookmarks, [action.payload.id]: action.payload.note },
    };
  } else if (isActionOf(removedBookmarkAction, action)) {
    const { [action.payload.id]: _, ...rest } = state.bookmarks;
    return {
      ...state,
      bookmarks: rest,
    };
  } else {
    return state;
  }
};
