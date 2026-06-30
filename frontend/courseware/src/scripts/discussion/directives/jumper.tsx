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

import classnames from 'classnames';
import { map } from 'lodash';
import React, { useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';

import { DiscussionStudentPickerModalBody } from '../modals/DiscussionStudentPickerModalBody.tsx';
import { openReactModal } from '../../directives/modalHost/reactModalHost';
import { discussionSummaryAPI } from '../../services/discussionSummaryAPI.ts';
import { useTranslation } from '../../i18n/translationContext';
import { useCourseSelector } from '../../loRedux';
import { LocalResourceStore } from '../../srs/pure/localResourceStore.ts';
import { searchByProps, sortByProps } from '../../users/usersConfig.js';
import * as jumperActions from '../actions/DiscussionJumperActions.ts';
import { createJumperSelector } from '../selectors.js';

const LOAD_LIMIT = 5;
const FETCH_TRIGGER_SIZE = 2;

// Plain data (was an Angular `.constant`); shared with the jump bar's category lists.
export const DiscussionJumperNames: Record<string, string> = {
  new: 'DISCUSSION_NEW_POSTS',
  bookmarked: 'DISCUSSION_BOOKMARKED_POSTS',
  unread: 'DISCUSSION_UNREAD_POSTS',
  unresponded: 'DISCUSSION_UNRESPONDED_POSTS',
  'user-posts': 'DISCUSSION_MINE_POSTS',
};

export const DiscussionJumperCategories = {
  instructor: ['user-posts', 'unread', 'bookmarked', 'unresponded'],
  student: ['user-posts', 'new', 'bookmarked'],
};

interface JumperProps {
  discussionId: string;
  viewType: string;
  setInView: (...args: any[]) => void;
}

/**
 * React port of the `discussionBoardJumper` component (B2, discussion subsystem — leaf 3, the real
 * leaf inside the jump bar): one nav jumper (unread / new / bookmarked / unresponded / user-posts) —
 * the count, prev/next post navigation, the activate button, and (for user-posts) the change-user
 * picker. Previously Angular (redux-connected, with two `$watch`es); now native React. Its only
 * consumer is `jumpBar.html` (still Angular), so it's bridged back via react2angular with the redux +
 * i18n providers. DOM preserved: `.nav-bar-item.<viewType>`, `.item-count`, the `.nav-button`
 * prev/next, `.item-label`, `.item-special` (change-user).
 */
export const DiscussionBoardJumper: React.FC<JumperProps> = ({ discussionId, viewType, setInView }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const selector = useMemo(() => createJumperSelector(discussionId, viewType), [discussionId, viewType]);
  const state = useCourseSelector<any>(selector) || {};
  const {
    totalCount,
    active,
    prevPost,
    nextPost,
    currentPost,
    currentPostIndex,
    loadedCount,
    loading,
    loadedOnce,
    isSelf,
    userName,
    userHandle,
    lastVisitedTime,
  } = state;

  const loadAction = useMemo(
    () => jumperActions.makeLoadActionCreator(discussionId, viewType),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, viewType]
  );
  const viewJumperAction = useMemo(
    () => jumperActions.makeViewJumperActionCreator(discussionId, viewType, setInView),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, viewType, setInView]
  );
  const viewPostAction = useMemo(
    () => jumperActions.makeViewPostActionCreator(discussionId, viewType, setInView),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, viewType, setInView]
  );
  const setUserAction = useMemo(
    () => (viewType === 'user-posts' ? jumperActions.makeSetUserActionCreator(discussionId, viewType) : null),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, viewType]
  );

  const loadMore = () => dispatch(loadAction(LOAD_LIMIT, loadedCount, { userHandle, lastVisitedTime }));
  const setJumperActive = (post: any) => dispatch(viewJumperAction(post));
  const setCurrentPost = (post: any) => dispatch(viewPostAction(post));

  const jumperName =
    viewType === 'user-posts' && !isSelf ? userName : translate(DiscussionJumperNames[viewType]);

  const changeCurrentPost = (post: any) => {
    setCurrentPost(post);
    // incrementally load when nearing the end (state here is pre-change, which is fine periodically)
    if (!loading && loadedCount - currentPostIndex < FETCH_TRIGGER_SIZE && loadedCount < totalCount) {
      loadMore();
    }
  };

  const pickUser = () => {
    const loader = () =>
      discussionSummaryAPI.getSummary(discussionId).then((userCounts: any[]) =>
        map(userCounts, userCount => ({ ...userCount.user, totalPosts: userCount.totalPosts }))
      );
    const store: any = new (LocalResourceStore as any)(loader);
    store.searchByProps = searchByProps;
    store.sortByProps = sortByProps;

    openReactModal<any>(controls => (
      <DiscussionStudentPickerModalBody
        {...controls}
        store={store}
      />
    ))
      .then((user: any) => {
        if (user && user.handle !== userHandle && setUserAction) dispatch(setUserAction(user));
      })
      .catch(() => {});
  };

  // The old user-posts `$watch(!loading && !loadedOnce)`: load once on mount (and again when the
  // user changes, which resets loadedOnce).
  useEffect(() => {
    if (viewType === 'user-posts' && !loading && !loadedOnce) loadMore();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewType, loading, loadedOnce]);

  return (
    <div className={classnames('nav-bar-item', viewType)}>
      <span className="item-count">{totalCount}</span>

      {active && (
        <button
          className="nav-button nav-button-next icon-btn"
          type="button"
          onClick={() => changeCurrentPost(prevPost)}
          disabled={!prevPost}
          title={translate('DISCUSSION_NAVBAR_PREVIOUS')}
        >
          <span className="icon icon-chevron-up" />
          <span className="sr-only">
            {translate('DISCUSSION_NAVBAR_PREVIOUS')} {translate(viewType)}
          </span>
        </button>
      )}

      {active && (
        <button
          className="nav-button nav-button-previous icon-btn"
          type="button"
          onClick={() => changeCurrentPost(nextPost)}
          disabled={!nextPost}
          title={translate('DISCUSSION_NAVBAR_NEXT')}
        >
          <span className="icon icon-chevron-down" />
          <span className="sr-only">
            {translate('DISCUSSION_NAVBAR_NEXT')} {translate(viewType)}
          </span>
        </button>
      )}

      <button
        className="item-label"
        type="button"
        onClick={() => setJumperActive(currentPost)}
        disabled={!totalCount}
      >
        {jumperName}
      </button>

      {viewType === 'user-posts' && (
        // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
        <span
          className="item-special d-flex align-items-center"
          onClick={pickUser}
          title={translate('DISCUSSION_NAVBAR_CHANGE_USER')}
        >
          <span className="icon icon-users" />
        </span>
      )}
    </div>
  );
};

