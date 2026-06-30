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

import React from 'react';

import LoadingSpinner from '../../directives/loadingSpinner';
import { useCourseSelector } from '../../loRedux';
import { createViewSelector } from '../selectors.js';
import { DiscussionThread } from './discussionThread.tsx';

interface SingleThreadViewProps {
  discussionId: string;
  settings: any;
}

/**
 * React port of the `discussionBoardSingleThreadView` component (B2, discussion subsystem — leaf 7b):
 * the single-thread (drill-in) board view — a loading spinner while the thread changes, then the one
 * in-view thread with its in-view orphan post. Previously a thin Angular redux container; now native
 * React (selector via `useCourseSelector`). Renders the already-React `DiscussionThread` (#1489)
 * directly, passing the in-view post id as `showOrphan`. The vestigial duScroll spy attributes and the
 * unused `discussion-item-wrap` class are dropped (scroll-to-post finds the still-Angular
 * `#discussion-item-…` element directly). Consumed by the still-Angular `discussionBoard.html` via the
 * react2angular bridge below. DOM preserved: `.discussion-single-thread-view`, the
 * `#discussion-thread-item-…` wrapper.
 */
export const DiscussionBoardSingleThreadView: React.FC<SingleThreadViewProps> = ({
  discussionId,
  settings,
}) => {
  const state = useCourseSelector<any>(createViewSelector(discussionId)) || {};
  const { changingThread, inViewThread, inViewPostId } = state;

  return (
    <div className="discussion-single-thread-view">
      {changingThread && <LoadingSpinner />}

      {inViewThread && !changingThread && (
        <div id={`discussion-thread-item-${inViewThread.id}`}>
          <DiscussionThread
            thread={inViewThread}
            discussionId={discussionId}
            showOrphan={inViewPostId}
            settings={settings}
          />
        </div>
      )}
    </div>
  );
};

