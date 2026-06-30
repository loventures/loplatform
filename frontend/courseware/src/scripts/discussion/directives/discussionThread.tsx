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
import { filter } from 'lodash';
import React, { useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';

import * as loadingActions from '../actions/DiscussionLoadingActions.ts';
import * as postActions from '../actions/DiscussionPostActions.ts';
import LoadingSpinner from '../../directives/loadingSpinner';
import { useTranslation } from '../../i18n/translationContext';
import { currentUser } from '../../utilities/currentUser.ts';
import { DiscussionItem } from './discussionItem.tsx';

interface DiscussionThreadProps {
  discussionId: string;
  thread: any;
  showOrphan?: any;
  settings: any;
}

/**
 * React port of the `discussionThread` component (B2, discussion subsystem — leaf 6): one thread =
 * the root post plus, when expanded, its replies area (orphans, a loading/`load-more` bar, and the
 * reply items). Previously Angular (`connectToCtrl` bound only action creators — the thread data
 * arrives as a binding). Now native React: dispatch via redux, the action creators via `lojector`, and
 * each post rendered via the now-native React `DiscussionItem`. Its consumers —
 * `discussionBoardThreadsView` / `…SingleThreadView` — render `<discussion-thread>` via the
 * react2angular bridge below. DOM preserved: `.discussion-thread` (+ `replies-expanded`/
 * `thread-expanded`), `.discussion-thread>discussion-item` (root post, the `DiscussionItem` host
 * element), `.discussion-thread-replies-area`, `.discussion-reply-list-child`,
 * `.discussion-replies-load-bar button`.
 */
export const DiscussionThread: React.FC<DiscussionThreadProps> = ({
  discussionId,
  thread,
  showOrphan,
  settings,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const userSvc = currentUser();
  const isInstructor = userSvc.isStrictlyInstructor();

  const threadId = thread.id;
  const loadRepliesAC = useMemo(
    () => loadingActions.makeLoadRepliesActionCreator(discussionId, threadId, isInstructor),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, threadId, isInstructor]
  );
  const setAllExpansionAC = useMemo(
    () => postActions.makeSetAllExpansionActionCreator(discussionId, threadId),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, threadId]
  );
  const setAllViewedAC = useMemo(
    () => postActions.makeBatchSetViewedActionCreator(discussionId, threadId),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, threadId]
  );

  const loadReplies = (offset?: number) => dispatch(loadRepliesAC(offset));
  const loadMoreReplies = () => loadReplies(thread.replies.length);

  const setAllExpansion = (expansion: boolean) => dispatch(setAllExpansionAC(expansion, thread.replies));
  const setAllViewed = (viewed: boolean) =>
    dispatch(
      setAllViewedAC(
        viewed,
        // because unread and viewed are opposites
        filter(thread.replies, (post: any) => post.isUnread === viewed && !post.isCurrentUserPost)
      )
    );

  // The old replies-area `ng-init="$ctrl.initReplies()"`: when the thread first expands, load the
  // first page of replies if none are loaded yet.
  useEffect(() => {
    if (thread.isChildrenVisible && thread.replies.length === 0 && thread.repliesRemaining > 0) {
      loadReplies();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [thread.isChildrenVisible]);

  const orphans = (thread.orphans || []).filter((orphan: any) => showOrphan === orphan.id);

  return (
    <div
      className={classnames('discussion-thread', {
        'replies-expanded': thread.isChildrenVisible,
        'thread-expanded': thread.isChildrenVisible,
      })}
    >
      <DiscussionItem
        discussionId={discussionId}
        item={thread.rootPost}
        thread={thread}
        isThread={true}
        setAllExpansion={setAllExpansion}
        setAllViewed={setAllViewed}
        settings={settings}
      />

      {thread.isChildrenVisible && (
        <ul className="discussion-thread-replies-area">
          {orphans.map((orphan: any) => (
            <li
              className="discussion-reply-list-child"
              key={orphan.id}
            >
              <DiscussionItem
                item={orphan}
                thread={thread}
                discussionId={discussionId}
                settings={settings}
              />
            </li>
          ))}

          {thread.loadingReplies && (
            <div className="alert alert-info m-1">
              <LoadingSpinner />
            </div>
          )}
          {!thread.loadingReplies && thread.repliesRemaining ? (
            <div className="discussion-replies-load-bar my-3">
              <button
                className="btn btn-secondary"
                type="button"
                onClick={loadMoreReplies}
              >
                <span>{translate('DISCUSSION_BOARD_LOAD_MORE_REPLIES', { count: thread.repliesRemaining })}</span>
              </button>
            </div>
          ) : null}

          {thread.replies.map((child: any) => (
            <li
              className="discussion-reply-list-child"
              key={child.id}
            >
              <DiscussionItem
                item={child}
                thread={thread}
                discussionId={discussionId}
                settings={settings}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

