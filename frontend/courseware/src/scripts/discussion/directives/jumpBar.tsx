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
import { includes } from 'lodash';
import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';

import { useTranslation } from '../../i18n/translationContext';
import * as jumperActions from '../actions/DiscussionJumperActions.ts';
import { currentUser } from '../../utilities/currentUser.ts';
import { useCourseSelector } from '../../loRedux';
import { createJumperSelector } from '../selectors.js';
import { DiscussionBoardJumper, DiscussionJumperCategories } from './jumper.tsx';

const VIEW_TYPES = ['user-posts', 'unread', 'new', 'bookmarked', 'unresponded'];

interface JumpBarProps {
  discussionId: string;
  setInView: (...args: any[]) => void;
  displayingView: boolean;
  backAction: () => void;
  lastVisitedTime: any;
}

/**
 * React port of the `discussionBoardJumpBar` component (B2, discussion subsystem — leaf 4): the
 * discussion nav bar that renders one `DiscussionBoardJumper` (#1486, now native React) per category.
 * Previously Angular; now native React, bridged back via react2angular (redux+i18n providers) so the
 * still-Angular `discussionBoard.html` keeps rendering `<discussion-board-jump-bar>`. It loads the
 * initial per-type summary once on mount (the old `$onInit`). DOM preserved: `.discussion-board-nav-bar`,
 * `ul.nav-items-container` (`compact`), the compact-toggle, the back-to-threads button, and the
 * `li.nav-item-container` jumpers.
 */
export const DiscussionBoardJumpBar: React.FC<JumpBarProps> = ({
  discussionId,
  setInView,
  displayingView,
  backAction,
  lastVisitedTime,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const [compact, setCompact] = useState(false);

  const user = currentUser();
  const isInstructor = user.isStrictlyInstructor();
  const userJumperTypes = isInstructor ? DiscussionJumperCategories.instructor : DiscussionJumperCategories.student;

  const selector = useMemo(() => createJumperSelector(discussionId, 'user-posts'), [discussionId]);
  const { userHandle } = useCourseSelector<any>(selector) || {};

  // The old `$onInit`: load the initial summary for this user's jumper types, once.
  useEffect(() => {
    const loadInitSummary = jumperActions.makeSummaryLoadActionCreator(
      discussionId,
      userJumperTypes,
      lastVisitedTime
    );
    dispatch(loadInitSummary(userHandle || user.getHandle()));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const showJumper = (jumperType: string) => includes(userJumperTypes, jumperType);

  return (
    <nav className="discussion-board-nav-bar d-print-none">
      <ul className={classnames('nav-items-container', { compact })}>
        <button
          className="icon-btn compact-toggle"
          type="button"
          title={translate('DISCUSSION_NAVBAR_TOGGLE')}
          onClick={() => setCompact(c => !c)}
        >
          <span className={classnames('icon', compact ? 'icon-chevron-left' : 'icon-chevron-right')} />
        </button>

        {displayingView && (
          <button
            className="icon-btn back-to-threads-button"
            type="button"
            title={translate('DISCUSSION_BACK_TO_THREADS')}
            onClick={() => backAction()}
          >
            <span className="lo-icon icon-circle-up-left" />
          </button>
        )}

        {VIEW_TYPES.map(viewType => (
          <li
            key={viewType}
            className="nav-item-container"
          >
            {showJumper(viewType) && (
              <DiscussionBoardJumper
                viewType={viewType}
                discussionId={discussionId}
                setInView={setInView}
              />
            )}
          </li>
        ))}
      </ul>
    </nav>
  );
};

