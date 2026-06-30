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

import React, { useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';

import { useTranslation } from '../../i18n/translationContext';
import { useCourseSelector } from '../../loRedux';
import * as loadingActions from '../actions/DiscussionLoadingActions.ts';
import * as sortActions from '../actions/DiscussionSortActions.ts';
import { createThreadsSelector } from '../selectors.js';
import discussionOrders from '../services/discussionOrders.js';
import { DiscussionThread } from './discussionThread.tsx';

interface ThreadsViewProps {
  discussionId: string;
  settings: any;
}

/**
 * React port of the `discussionBoardThreadsView` component (B2, discussion subsystem — leaf 7a): the
 * default board view — the scrollable list of threads + a "load more threads" bar. Previously a thin
 * Angular redux container; now native React (selector via `useCourseSelector`, dispatch + the
 * load/sort action creators via `lojector`). Renders the already-React `DiscussionThread` (#1489)
 * directly. The vestigial duScroll `du-scrollspy`/`du-spy-context` and the unused `discussion-item-wrap`
 * class are dropped (nothing reads them; scroll-to-post operates on the still-Angular post element via
 * `$document.duScrollToElementAnimated`); the never-called `scrollTop` is dropped. Consumed by the
 * still-Angular `discussionBoard.html` via the react2angular bridge below. DOM preserved:
 * `.discussion-threads-view`, `ul.discussion-thread-list`, the per-thread `li#discussion-thread-item-…`,
 * `.discussion-threads-load-bar button.btn-primary`.
 */
export const DiscussionBoardThreadsView: React.FC<ThreadsViewProps> = ({ discussionId, settings }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const state = useCourseSelector<any>(createThreadsSelector(discussionId)) || {};
  const { threads = [], threadsRemaining, threadsLoaded, order, lastVisitedTime } = state;

  const loadThreadsAC = useMemo(
    () => loadingActions.makeLoadThreadsActionCreator(discussionId),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId]
  );
  const initialSortAC = useMemo(
    () => sortActions.makeSortActionCreator(discussionId, discussionOrders.SORT_DISCUSSION_ACTIVITY_DATE_DESC),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId]
  );

  // The old `ng-init="$ctrl.initial()"` on the list: apply the default activity-date sort once on mount.
  useEffect(() => {
    dispatch(initialSortAC({ lastVisitedTime }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadMoreThreads = () =>
    dispatch(loadThreadsAC({ offset: threadsLoaded, limit: 8, order, lastVisitedTime }));

  return (
    <div className="discussion-threads-view">
      <ul className="discussion-thread-list">
        {threads.map((thread: any) => (
          <li
            key={thread.id}
            id={`discussion-thread-item-${thread.id}`}
          >
            <DiscussionThread
              thread={thread}
              discussionId={discussionId}
              settings={settings}
            />
          </li>
        ))}
      </ul>

      <div className="discussion-threads-load-bar">
        {threadsRemaining > 0 && (
          <button
            className="btn btn-primary"
            type="button"
            onClick={loadMoreThreads}
          >
            <span>{translate('DISCUSSION_BOARD_LOAD_MORE_THREADS', { count: threadsRemaining })}</span>
          </button>
        )}
      </div>
    </div>
  );
};

