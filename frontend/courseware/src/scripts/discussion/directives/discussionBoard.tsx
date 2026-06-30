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
import { isFunction, mapValues } from 'lodash';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';

import { openConfirmModal } from '../../directives/modalHost/ConfirmModal.tsx';
import { useTranslation } from '../../i18n/translationContext';
import { TranslationProvider } from '../../i18n/translationContext';
import settingsSvc from '../../utilities/settingsService';
import { Roles } from '../../utilities/pure/roles';
import { useCourseSelector } from '../../loRedux';
import { withNgReduxProvider } from '../../utilities/ngReduxProvider.jsx';
import * as boardActions from '../actions/DiscussionBoardActions.ts';
import * as viewActions from '../actions/DiscussionViewActions.ts';
import * as writingActions from '../actions/DiscussionWritingActions.ts';
import * as sortActionsSvc from '../actions/DiscussionSortActions.ts';
import { createBoardSelector } from '../selectors.js';
import discussionOrders from '../services/discussionOrders.js';
import { DiscussionBoardSearch } from './discussionBoardSearch.tsx';
import { DiscussionBoardSettings } from './discussionBoardSettings.tsx';
import { DiscussionBoardSingleThreadView } from './discussionBoardSingleThreadView.tsx';
import { DiscussionBoardThreadsView } from './discussionBoardThreadsView.tsx';
import { DiscussionReplyEditor } from './discussionReply.tsx';
import { DiscussionBoardJumpBar } from './jumpBar.tsx';

// Plain data (were Angular `.constant`s; only this component injected them).
const DiscussionViewHeaderTranslationKeys: Record<string, string> = {
  unread: 'DISCUSSION_VIEW_UNREAD_POSTS',
  new: 'DISCUSSION_VIEW_NEW_POSTS',
  bookmarked: 'DISCUSSION_VIEW_BOOKMARKED_POSTS',
  unresponded: 'DISCUSSION_VIEW_UNRESPONDED_POSTS',
  'user-posts': 'DISCUSSION_VIEW_USER_POSTS',
  search: 'DISCUSSION_VIEW_SEARCH_POSTS',
  'reported-inappropriate-posts': 'DISCUSSION_VIEW_REPORTED_INAPPROPRIATE_POSTS',
};
const DiscussionBoardMessages = {
  closeDiscussion: 'CONFIRM_CLOSE_DISCUSSION',
  openDiscussion: 'CONFIRM_OPEN_DISCUSSION',
};

interface DiscussionBoardProps {
  discussionId: string;
  contentItemId: string;
  afterNewThread?: () => void;
  grading?: boolean;
  isOpen: boolean;
  isClosed: boolean;
  gatingPolicies: any;
  printView?: boolean;
}

// Native "Sort By" dropdown (was the Angular `<list-sort>` directive, rendered via angular2react).
// Selenide locates the toggle by `.btn-primary.dropdown-toggle`.
const SortDropdown: React.FC<{
  sortActions: Record<string, (params: any) => void>;
  params: any;
  disable?: boolean;
  className?: string;
}> = ({ sortActions, params, disable, className }) => {
  const translate = useTranslation();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, [open]);

  return (
    <div
      className={classnames('discussion-sort', className)}
      ref={ref}
    >
      <button
        type="button"
        className="btn btn-primary dropdown-toggle"
        disabled={disable}
        onClick={() => setOpen(o => !o)}
      >
        <span>{translate('Sort By')}</span>
        <span className="lo-icon icon-chevron-down" />
      </button>
      {open && (
        <ul className="dropdown-menu show">
          {Object.entries(sortActions).map(([key, action]) => (
            <li key={key}>
              <button
                type="button"
                className="dropdown-item"
                onClick={() => {
                  action(params);
                  setOpen(false);
                }}
              >
                {translate(key)}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

/**
 * React port of the `discussionBoard` orchestrator (B2, discussion subsystem — the final piece): the
 * board shell that composes all the now-React children — the jump bar, the new-thread editor, the
 * search box, the view header (back button / title / sort / settings), and the threads /
 * single-thread views. Previously an Angular component (`connectToCtrl(createBoardSelector, …)` +
 * `angular2react` export); now native React (selector via `useCourseSelector`, dispatch + the
 * board/view/writing/sort action creators via `lojector`, the close-discussion + report confirms via
 * the React `openConfirmModal`). Cached user settings via `Settings.get/setUserContext`. The "Sort By"
 * dropdown is the native `SortDropdown` below (the generic Angular `list-sort` stays for `basicList`).
 *
 * Consumed directly by the React `DiscussionActivity` (via the `DiscussionBoard` export below, which
 * supplies the ngRedux + i18n providers). DOM preserved: `.discussion-board`,
 * `.discussion-board-write-post`, `.discussion-view-header`/`.back-to-threads-button`/`.view-title`,
 * `#discussion-list-top`/`.flash-message`, and the `.btn-primary.dropdown-toggle` sort control.
 */
const DiscussionBoard: React.FC<DiscussionBoardProps> = ({
  discussionId,
  contentItemId,
  afterNewThread,
  grading,
  isOpen,
  isClosed,
  gatingPolicies,
  printView,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const state = useCourseSelector<any>(createBoardSelector(discussionId)) || {};
  const {
    courseEnded,
    showJumpBar,
    readyToLoadThreads,
    lastVisitedTime,
    displayView,
    viewInfo,
    settings,
    newThread,
    loadMessage,
    notification,
    inViewThreadId,
    lastVisitedError,
  } = state;

  const isTrialLearner = Roles.isUnderTrialAccess();
  const isInstructor = settingsSvc.isFeatureEnabled('TeachCourseRight');
  const showTitle = settingsSvc.isFeatureEnabled('DiscussionTitle');

  const canWritePosts = () =>
    !courseEnded && !grading && !isTrialLearner && (isOpen || isInstructor) && !printView;
  const canCreateThreads = () =>
    !courseEnded && !grading && !isTrialLearner && (isOpen || isInstructor) && !printView;
  // evidently the instructor can reply while grading.
  const canWriteReplies = () =>
    !courseEnded && !isTrialLearner && (isOpen || isInstructor) && !printView;

  // The old `connectToCtrl(null, actionCreators)` — dispatched action creators.
  const ac = useMemo(
    () => ({
      setVisited: boardActions.makeVisitBoardActionCreator(discussionId),
      updateSettings: boardActions.makeUpdateSettingsActionCreator(discussionId),
      closeDiscussionAC: boardActions.makeCloseDiscussionActionCreator(contentItemId, discussionId, gatingPolicies),
      setInViewPost: viewActions.makeViewPostActionCreator(discussionId),
      setInViewRepliedPost: viewActions.makeViewRepliedToPostActionCreator(discussionId),
      setInViewInappropriatePost: viewActions.makeViewInappropriatePostActionCreator(discussionId),
      threadSaveAC: writingActions.makeThreadSaveActionCreator(discussionId),
      threadDiscard: writingActions.makeWritingDiscardActionCreator({ discussionId }),
      threadKeepWorking: writingActions.makeWritingKeepWorkingActionCreator({ discussionId }),
      toThreadsView: viewActions.makeRestoreDefaultActionCreator(discussionId),
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId, contentItemId, gatingPolicies]
  );

  // dispatch-bound `sortActions` map for the `SortDropdown`.
  const sortActions = useMemo(
    () =>
      mapValues(discussionOrders, (order: any) => {
        const creator = sortActionsSvc.makeSortActionCreator(discussionId, order);
        return (params: any) => dispatch(creator(params));
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [discussionId]
  );

  const updateSettings = (s: any) => dispatch(ac.updateSettings(s));
  const setVisited = (g: any) => dispatch(ac.setVisited(g));
  const toThreadsView = () => dispatch(ac.toThreadsView());

  const getCachedSettings = () => settingsSvc.getUserContext('discussionBoardUserSettings');
  const setCachedSettings = (s: any) => settingsSvc.setUserContext('discussionBoardUserSettings', s);
  const updateCachedSettings = (update: any) => {
    setCachedSettings({ ...getCachedSettings(), ...update });
    updateSettings(update);
  };

  const confirm = (msg: string) => openConfirmModal({ message: msg } as any);
  const closeDiscussion = (s: any) => {
    const msg = s.closeDiscussion ? DiscussionBoardMessages.closeDiscussion : DiscussionBoardMessages.openDiscussion;
    confirm(msg).then(() => dispatch(ac.closeDiscussionAC(s)));
  };

  const updateSettingsFns = {
    autoMarkUnread: updateCachedSettings,
    closeDiscussion,
  };

  const threadSave = (...args: any[]) => {
    const revisit = lastVisitedError && lastVisitedError.type === 'UNAUTHORIZED_ERROR';
    dispatch(ac.threadSaveAC(...args, revisit));
    if (isFunction(afterNewThread)) afterNewThread();
  };
  const threadDiscard = () => dispatch(ac.threadDiscard());
  const threadKeepWorking = () => dispatch(ac.threadKeepWorking());

  const setInView = (post: any, info?: any) => dispatch(ac.setInViewPost(post, inViewThreadId, info));

  // The old `$onInit`: seed settings (cached over defaults), resolve notification deep-links, and
  // record the visit. Runs once on mount.
  useEffect(() => {
    const initialSettings = {
      autoMarkUnread: true,
      canMarkUnread: isInstructor,
      closeDiscussion: isClosed,
      canWriteReplies: () => canWriteReplies(),
      ...getCachedSettings(),
    };
    updateSettings(initialSettings);

    if (notification) {
      if (notification.inappropriate) {
        dispatch(ac.setInViewInappropriatePost(notification.id, inViewThreadId));
      } else {
        dispatch(ac.setInViewRepliedPost(notification.id, inViewThreadId));
      }
    }

    if (!printView) setVisited(grading);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="discussion-board">
      {showJumpBar && readyToLoadThreads && !printView && (
        <DiscussionBoardJumpBar
          discussionId={discussionId}
          setInView={setInView}
          backAction={toThreadsView}
          displayingView={displayView}
          lastVisitedTime={lastVisitedTime}
        />
      )}

      {canWritePosts() && (
        <div className="discussion-board-write-post">
          <h2 className="h4 write-post">
            <i
              className="icon icon-bubble-plus d-print-none"
              aria-hidden="true"
            />
            <span>{translate('DISCUSSION_CREATE_THREAD')}</span>
          </h2>

          {canCreateThreads() ? (
            <div>
              <DiscussionReplyEditor
                state={newThread}
                saveAction={threadSave}
                discardAction={threadDiscard}
                keepWorkingAction={threadKeepWorking}
                showTitle={showTitle}
              />
            </div>
          ) : (
            <div className="asset-final-pending">
              <span>{translate('DISCUSSION_THREAD_LIMIT_MESSAGE')}</span>
            </div>
          )}
        </div>
      )}

      <DiscussionBoardSearch
        discussionId={discussionId}
        setInView={setInView}
      />

      <div id="discussion-list-top">
        {loadMessage && (
          <div className="lo-well flash-message">
            <span
              className="icon icon-info"
              aria-hidden="true"
            />
            <span>{translate(loadMessage)}</span>
          </div>
        )}

        <header className="discussion-view-header my-2">
          {displayView && (
            <button
              className="icon-btn back-to-threads-button"
              type="button"
              title={translate('DISCUSSION_BACK_TO_THREADS')}
              onClick={toThreadsView}
            >
              <span className="lo-icon icon-circle-up-left" />
            </button>
          )}
          <h3 className="h4 m-0 view-title">
            {!displayView ? (
              <span>{translate('DISCUSSION_THREADS_VIEW')}</span>
            ) : (
              <span>{translate(DiscussionViewHeaderTranslationKeys[viewInfo?.viewType])}</span>
            )}
          </h3>
          <SortDropdown
            className="board-control mx-1 d-print-none"
            sortActions={sortActions}
            params={{ discussionId, lastVisitedTime }}
            disable={displayView}
          />
          {settings?.canMarkUnread && (
            <DiscussionBoardSettings
              settings={settings}
              updateSettings={updateSettingsFns}
            />
          )}
        </header>

        {readyToLoadThreads && !displayView && (
          <DiscussionBoardThreadsView
            discussionId={discussionId}
            settings={settings}
          />
        )}

        {readyToLoadThreads && displayView && (
          <DiscussionBoardSingleThreadView
            discussionId={discussionId}
            settings={settings}
          />
        )}
      </div>
    </div>
  );
};

// Rendered directly by the React `DiscussionActivity`; supply the ngRedux + i18n providers (the whole
// subtree — board + all children — reads them from this context).
const Wrapped = withNgReduxProvider((props: DiscussionBoardProps) => (
  <TranslationProvider>
    <DiscussionBoard {...props} />
  </TranslationProvider>
));

export { Wrapped as DiscussionBoard };

