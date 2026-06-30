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
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';

import { FeedbackFileList } from '../../assignmentFeedback/directives/feedbackFileList.tsx';
import { HtmlWithMathJax } from '../../components/HtmlWithMathjax.tsx';
import { openConfirmModal } from '../../directives/modalHost/ConfirmModal.tsx';
import { fromNow } from '../../filters/pure/fromNow.ts';
import { useTranslation } from '../../i18n/translationContext';
import { currentUser } from '../../utilities/currentUser.ts';
import settingsSvc from '../../utilities/settingsService';
import * as postActions from '../actions/DiscussionPostActions.ts';
import * as writingActions from '../actions/DiscussionWritingActions.ts';
import { discussionAutoUnreadService } from './pure/discussionAutoUnreadService.ts';
import { DiscussionReplyEditor } from './discussionReply.tsx';

interface DiscussionItemProps {
  discussionId: string;
  item: any;
  thread: any;
  isThread?: boolean;
  setAllExpansion?: (expansion: boolean) => void;
  setAllViewed?: (viewed: boolean) => void;
  settings: any;
}

/**
 * Native React port of the former `discussion-auto-unread` directive: two off-screen sentinels that
 * auto-mark a post read once it's been on screen long enough. The original used `angular-inview`; this
 * uses `IntersectionObserver` — a "midpoint" sentinel (bottom of the viewport shrunk by
 * `screenCenterOffset`, matching the old `viewportOffset:[0,0,-offset,0]`) and a "viewing" sentinel
 * that must stay visible for 2s. When both fire, it calls the (still-Angular) `DiscussionAutoUnreadService`
 * — the redux batching/aggregation layer — via lojector. Only mounted when the post is unread/new, so it
 * fires once and unmounts when the parent re-renders.
 */
const AUTO_UNREAD_DWELL_MS = 2000;

const DiscussionAutoUnread: React.FC<{
  postId: any;
  threadId: any;
  discussionId: string;
  item: any;
}> = ({ postId, threadId, discussionId, item }) => {
  const midRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<HTMLDivElement>(null);
  const midReached = useRef(false);
  const dwelled = useRef(false);
  const queued = useRef(false);

  useEffect(() => {
    const isInstructor = settingsSvc.isFeatureEnabled('TeachCourseRight');
    // half the screen, capped — so posts on tall screens can still be auto-marked.
    const screenCenterOffset = Math.min(window.innerHeight * 0.5, 100);
    let dwellTimer: ReturnType<typeof setTimeout> | null = null;

    const tryUpdate = () => {
      if (!midReached.current || !dwelled.current || queued.current) return;
      const shouldUpdate = isInstructor ? item.isUnread : item.isNew;
      if (shouldUpdate) {
        discussionAutoUnreadService.updateViewed(discussionId, threadId, postId);
        queued.current = true;
      }
    };

    const midObs = new IntersectionObserver(
      ([e]) => {
        if (e.isIntersecting) {
          midReached.current = true;
          tryUpdate();
        }
      },
      { rootMargin: `0px 0px -${screenCenterOffset}px 0px` }
    );
    const viewObs = new IntersectionObserver(([e]) => {
      if (e.isIntersecting && !dwellTimer) {
        dwellTimer = setTimeout(() => {
          dwellTimer = null;
          dwelled.current = true;
          tryUpdate();
        }, AUTO_UNREAD_DWELL_MS);
      } else if (!e.isIntersecting && dwellTimer) {
        clearTimeout(dwellTimer);
        dwellTimer = null;
      }
    });

    if (midRef.current) midObs.observe(midRef.current);
    if (viewRef.current) viewObs.observe(viewRef.current);
    return () => {
      midObs.disconnect();
      viewObs.disconnect();
      if (dwellTimer) clearTimeout(dwellTimer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <>
      <div
        role="presentation"
        ref={midRef}
      />
      <div
        role="presentation"
        ref={viewRef}
      />
    </>
  );
};

// Shared "close on outside click / Escape" for the two footer dropdowns (the old `uib-dropdown`s).
const useOutsideClose = (open: boolean, close: () => void) => {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) close();
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close();
    };
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open, close]);
  return ref;
};

/**
 * React port of the `discussionItem` component (B2, discussion subsystem — the heavy core post): one
 * post — header (track/expand toggles, title/preview, thread reply summary, pin/bookmark), body
 * (status tags, rich content via `HtmlWithMathJax`, attachments via the React `FeedbackFileList`, the
 * auto-unread sentinel), the footer (reply / replies dropdown / more-actions dropdown), and the inline
 * reply & edit editors (the React `DiscussionReplyEditor`). The three former scope-sharing
 * sub-directives (header/content/footer) are folded into this one component; the `connectToCtrl(null,
 * …)` action wiring → redux `useDispatch` + the action creators via `lojector`; the report-post
 * confirm opens the React `openConfirmModal` directly. Rendered natively by `DiscussionThread`
 * (#1489), so no react2angular bridge is needed.
 *
 * Selenide contract: rendered inside a literal `<discussion-item>` host element (located by
 * `.discussion-thread>discussion-item` and `replyArea.$$("discussion-item")`); preserves
 * `.discussion-item[-post|-header|-content|-content-status|-content-area|-footer|-info]`, the header
 * `.unread-toggle`/`.new-post-indicator`/`.expansion-toggle`/`.title-info-row`/`.no-title-text`/
 * `.content-info-row`(+`.show-when-*`)/`.toggle-pinned`/`.toggle-bookmark`/`.toggle-dropdown`/
 * `i.viewed-icon`, and the footer `.text-reply`/`.text-reply-count`/`.replies-dropdown-toggle`/
 * `.discussion-replies-dropdown`/`.discussion-footer-dropdown`(`.text-dropdown`)/
 * `.discussion-footer-optional` with `.text-edit`/`.text-mark-inappropriate`/
 * `.text-report-inappropriate`/`.text-deleted`/`.text-restore`/`.text-post-date`. The replies-menu
 * items, formerly located by their `translate` attribute, now carry class hooks
 * (`read-all`/`unread-all`/`expand-all`/`collapse-all`) — the page object was updated to match.
 */
export const DiscussionItem: React.FC<DiscussionItemProps> = ({
  discussionId,
  item,
  thread,
  isThread,
  setAllExpansion,
  setAllViewed,
  settings,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const userSvc = currentUser();

  const showTitle = settingsSvc.isFeatureEnabled('DiscussionTitle');
  const isInstructor = userSvc.isStrictlyInstructor();
  const isOwnPost = userSvc.handle === item.author?.handle;
  const automarkable = settings.autoMarkUnread && !isOwnPost;
  const markableUnread = settings.canMarkUnread && !isOwnPost;
  const markableNew = !settings.canMarkUnread && !isOwnPost;
  const canPinThread = !!isThread && isInstructor;

  const [statusAsDropdown, setStatusAsDropdown] = useState(false);
  const [repliesMenuOpen, setRepliesMenuOpen] = useState(false);
  const [moreMenuOpen, setMoreMenuOpen] = useState(false);
  const repliesRef = useOutsideClose(repliesMenuOpen, () => setRepliesMenuOpen(false));
  const moreRef = useOutsideClose(moreMenuOpen, () => setMoreMenuOpen(false));

  const threadId = thread && thread.id;
  const itemId = item.id;

  // The old `connectToCtrl(null, actions)` — only action creators, dispatched.
  const actions = useMemo(() => {
    const a: any = {
      setViewed: postActions.makeSetViewedActionCreator(discussionId, threadId, itemId),
      setBookmarked: postActions.makeSetBookmarkedActionCreator(discussionId, itemId),
      setInappropriate: postActions.makeSetInappropriateActionCreator(discussionId, itemId),
      setRemoved: postActions.makeSetRemovedActionCreator(discussionId, itemId),
      editStart: writingActions.makeEditStartActionCreator(discussionId, itemId),
      editSave: writingActions.makeEditSaveActionCreator(discussionId, itemId),
      editDiscard: writingActions.makeEditDiscardActionCreator(discussionId, itemId),
      replyStart: writingActions.makeReplyStartActionCreator(discussionId, threadId, itemId),
      replySave: writingActions.makeReplySaveActionCreator(discussionId, threadId, itemId),
      replyDiscard: writingActions.makeReplyDiscardActionCreator(discussionId, threadId, itemId),
      toggleExpandPost: postActions.makeToggleExpandPostActionCreator(discussionId, itemId),
      reportInappropriate: postActions.makeReportInappropriateActionCreator(discussionId, itemId),
    };
    if (thread) {
      a.toggleExpandReplies = postActions.makeToggleExpandRepliesActionCreator(discussionId, itemId);
      a.setPinned = postActions.makeSetPinnedActionCreator(discussionId, itemId);
    }
    return a;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [discussionId, threadId, itemId]);

  const run = (creator: any, ...args: any[]) => dispatch(creator(...args));

  const setViewed = (v: boolean) => run(actions.setViewed, v);
  const setBookmarked = (v: boolean) => run(actions.setBookmarked, v);
  const setInappropriate = (v: boolean) => run(actions.setInappropriate, v);
  const setRemoved = (v: boolean) => run(actions.setRemoved, v);
  const toggleExpandPost = () => run(actions.toggleExpandPost);
  const toggleExpandReplies = () => run(actions.toggleExpandReplies);
  const setPinned = (v: boolean) => run(actions.setPinned, v);
  const editStart = () => run(actions.editStart);
  const editSave = (...args: any[]) => run(actions.editSave, ...args);
  const editDiscard = () => run(actions.editDiscard);
  const replyStart = (id: any) => run(actions.replyStart, id);
  const replySave = (...args: any[]) => run(actions.replySave, ...args);
  const replyDiscard = () => run(actions.replyDiscard);

  const reportPost = () =>
    openConfirmModal({ message: 'DISCUSSION_CONFIRM_REPORT_POST' } as any).then(() =>
      run(actions.reportInappropriate)
    );

  const showAutoUnread =
    automarkable && !item.viewedManuallyToggled && (isInstructor ? item.isUnread : item.isNew);

  const Host = 'discussion-item' as any;

  return (
    <Host>
      <div
        className={classnames('discussion-item', { 'no-title': !showTitle })}
        id={`discussion-item-${item.id}`}
      >
        <div className={classnames('discussion-item-post', item.classes)}>
          {/* ===== header ===== */}
          <div className="discussion-item-header top-level-header">
            {markableUnread && (
              // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
              <div
                className="discussion-header-column unread-toggle show-when-available d-print-none"
                onClick={() => setViewed(!item.track?.viewed)}
                title={translate(
                  item.track?.viewed ? 'DISCUSSION_POST_TRACKING_VIEWED' : 'DISCUSSION_POST_TRACKING_NOT_VIEWED'
                )}
              >
                <i className="viewed-icon" />
              </div>
            )}

            {markableNew && (
              <div
                className="discussion-header-column new-post-indicator show-when-available d-print-none"
                title={translate(item.isNew ? 'DISCUSSION_POST_TRACKING_NEW' : 'DISCUSSION_POST_TRACKING_NOT_NEW')}
              >
                <i className="viewed-icon" />
              </div>
            )}

            {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
            <div
              className="discussion-header-column expansion-toggle d-print-none"
              aria-label={translate('DISCUSSION_THREAD_TOGGLE_EXPAND')}
              onClick={toggleExpandPost}
            >
              <i className="icon icon-chevron-down hide-when-collapsed" />
              <i className="icon icon-chevron-right show-when-collapsed" />
            </div>

            {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
            <div
              className="discussion-header-column title-column"
              onClick={toggleExpandPost}
            >
              <div className="user-info-row font-italic">
                <span>{translate('DISCUSSION_POST_POST_BY', { name: item.user?.fullName })}</span>
                {item.moderatorPost && (
                  <span className="instructor-label">{translate('DISCUSSION_POST_MODERATOR_TAG')}</span>
                )}
              </div>

              {showTitle && item.depth === 0 && (
                <div className="title-info-row">
                  {item.title ? (
                    <span className="font-weight-bold show-when-available hide-when-reported">{item.title}</span>
                  ) : (
                    <span className="no-title-text font-weight-bold show-when-available hide-when-reported">
                      {translate('DISCUSSION_POST_NO_TITLE')}
                    </span>
                  )}
                </div>
              )}

              <div className="content-info-row">
                <span className="show-when-inappropriate">{translate('DISCUSSION_POST_INAPPROPRIATE_TAG')}</span>
                <span className="show-when-removed">{translate('DISCUSSION_POST_DELETED_TAG')}</span>
                <span className="show-when-reported">
                  <i className="icon icon-warning" />
                  <span>{translate('DISCUSSION_POST_REPORTED_TAG')}</span>
                </span>
                {!showTitle && (
                  <HtmlWithMathJax
                    className="content-preview show-when-available hide-when-reported"
                    html={item.contentPreview}
                  />
                )}
              </div>
            </div>

            {isThread && (
              <div
                className={classnames('discussion-header-column discussion-item-info', {
                  'as-dropdown': statusAsDropdown,
                })}
                style={item.expandPost ? { display: 'none' } : undefined}
              >
                {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
                <span
                  className="item-info-row info-replies"
                  onClick={toggleExpandPost}
                >
                  <span>{translate('DISCUSSION_POST_REPLY_COUNT', { count: thread.repliesCount })}</span>
                  &ndash;{' '}
                  {!settings.canMarkUnread ? (
                    <span className={classnames({ new: thread.newRepliesCount })}>
                      {translate('DISCUSSION_POST_NEW_REPLY_COUNT', { count: thread.newRepliesCount })}
                    </span>
                  ) : (
                    <span className={classnames({ unread: thread.unreadRepliesCount })}>
                      {translate('DISCUSSION_POST_UNREAD_REPLY_COUNT', { count: thread.unreadRepliesCount })}
                    </span>
                  )}
                </span>

                <span className="item-info-row info-date">
                  <span>{translate('DISCUSSION_POST_LAST_ACTIVITY_DATE')}</span>{' '}
                  <span>{fromNow(item.lastActivityTime)}</span>
                </span>
              </div>
            )}

            <div className="discussion-header-column status-toggles d-print-none">
              {isThread && (
                <button
                  className="status-toggle toggle-dropdown icon-btn show-when-collapsed"
                  type="button"
                  onClick={() => setStatusAsDropdown(d => !d)}
                >
                  <span className="sr-only">{translate('DISCUSSION_POST_DROPDOWN')}</span>
                  <i className="icon icon-more-horizontal" />
                </button>
              )}
              {(canPinThread || item.pinned) && (
                <button
                  className="status-toggle toggle-pinned icon-btn icon-btn-primary icon-btn-circle"
                  type="button"
                  onClick={() => canPinThread && setPinned(!item.pinned)}
                  title={translate(item.pinned ? 'DISCUSSION_POST_PINNED' : 'DISCUSSION_POST_NOT_PINNED')}
                >
                  <i className="icon icon-pushpin" />
                </button>
              )}
              <button
                className="status-toggle toggle-bookmark icon-btn icon-btn-primary icon-btn-circle"
                type="button"
                onClick={() => setBookmarked(!item.track?.bookmarked)}
              >
                <span className="sr-only">
                  {translate(item.track?.bookmarked ? 'DISCUSSION_POST_BOOKMARKED' : 'DISCUSSION_POST_NOT_BOOKMARKED')}
                </span>
                <i className="icon icon-bookmark" />
              </button>
            </div>
          </div>

          {/* ===== content area (status + body/edit + footer) ===== */}
          {item.expandBody && (
            <div className="discussion-item-content-area">
              <div className="discussion-item-content-status">
                {item.inappropriate && !item.removed && (
                  <span>{translate('DISCUSSION_POST_INAPPROPRIATE_TAG')}</span>
                )}
                {item.removed && <span>{translate('DISCUSSION_POST_DELETED_TAG')}</span>}
                <span className="show-when-reported">
                  <i className="icon icon-warning" />
                  <span>{translate('DISCUSSION_POST_REPORTED_TAG')}</span>
                </span>
              </div>

              {item.showContent && !item.edit?.editing && (
                <div className="discussion-item-content">
                  {showAutoUnread && (
                    <span className="hidden-element">
                      <DiscussionAutoUnread
                        postId={item.id}
                        threadId={item.threadId}
                        discussionId={discussionId}
                        item={item}
                      />
                    </span>
                  )}

                  <HtmlWithMathJax
                    className="item-rich-content"
                    html={item.content}
                  />

                  <div className="item-attachments">
                    <FeedbackFileList rawFiles={item.attachments} />
                  </div>
                </div>
              )}

              {item.showContent && item.edit?.editing && (
                <DiscussionReplyEditor
                  cannotEdit={!item.editable}
                  state={item.edit}
                  post={item}
                  attachments={item.attachments}
                  saveAction={editSave}
                  discardAction={editDiscard}
                  showTitle={showTitle && item.depth === 0}
                />
              )}

              {/* ===== footer ===== */}
              {/*
               * The two menus stay in the DOM always (like the old `uib-dropdown-menu`s) — their
               * visibility is driven by CSS: `.discussion-footer-optional` is shown inline on desktop
               * and, on small screens, toggled by `.discussion-item-footer.open`; the replies menu
               * (`.discussion-replies-dropdown.dropdown-menu`, hidden by default) is shown by
               * `.discussion-footer-primary.open`. So we toggle those `.open` classes rather than
               * mount/unmount, and the Selenide selectors (`.text-edit` etc.) stay reachable.
               */}
              <div
                className={classnames('discussion-item-footer', { open: moreMenuOpen })}
                ref={moreRef}
              >
                <div
                  className={classnames('discussion-footer-primary', { open: repliesMenuOpen })}
                  ref={repliesRef}
                >
                  {settings.canWriteReplies() && item.showContent && item.depth <= 20 && (
                    // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                    <span
                      className="footer-text text-button text-reply"
                      onClick={() => replyStart(item.id)}
                    >
                      <span className="icon icon-bubble-plus" />
                      <span>{translate('DISCUSSION_POST_REPLY_BTN')}</span>
                    </span>
                  )}

                  {isThread && thread.repliesCount > 0 && (
                    <div>
                      {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
                      <span
                        className="footer-text text-button text-reply-count"
                        onClick={toggleExpandReplies}
                      >
                        <span>{translate('DISCUSSION_POST_REPLY_COUNT', { count: thread.repliesCount })}</span>
                        {!!thread.newRepliesCount && !settings.canMarkUnread && (
                          <span>
                            {' '}
                            &ndash;{' '}
                            <span className="new">
                              {translate('DISCUSSION_POST_NEW_REPLY_COUNT', { count: thread.newRepliesCount })}
                            </span>
                          </span>
                        )}
                        {!!thread.unreadRepliesCount && settings.canMarkUnread && (
                          <span>
                            {' '}
                            &ndash;{' '}
                            <span className="unread">
                              {translate('DISCUSSION_POST_UNREAD_REPLY_COUNT', {
                                count: thread.unreadRepliesCount,
                              })}
                            </span>
                          </span>
                        )}
                      </span>

                      <button
                        className="footer-text icon-btn replies-dropdown-toggle"
                        type="button"
                        title={translate('DISCUSSION_POST_REPLIES_DROPDOWN')}
                        onClick={() => setRepliesMenuOpen(o => !o)}
                      >
                        <span className="icon icon-chevron-down" />
                        <span className="sr-only">{translate('DISCUSSION_POST_REPLY_BTN')}</span>
                      </button>

                      <span className="discussion-replies-dropdown dropdown-menu">
                        {settings.canMarkUnread && (
                          // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                          <p
                            className="replies-dropdown-item read-all"
                            onClick={() => {
                              setAllViewed?.(true);
                              setRepliesMenuOpen(false);
                            }}
                          >
                            {translate('DISCUSSION_POST_READ_ALL')}
                          </p>
                        )}
                        {settings.canMarkUnread && (
                          // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                          <p
                            className="replies-dropdown-item unread-all"
                            onClick={() => {
                              setAllViewed?.(false);
                              setRepliesMenuOpen(false);
                            }}
                          >
                            {translate('DISCUSSION_POST_UNREAD_ALL')}
                          </p>
                        )}
                        {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
                        <p
                          className="replies-dropdown-item expand-all"
                          onClick={() => {
                            setAllExpansion?.(true);
                            setRepliesMenuOpen(false);
                          }}
                        >
                          {translate('DISCUSSION_POST_EXPAND_ALL')}
                        </p>
                        {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
                        <p
                          className="replies-dropdown-item collapse-all"
                          onClick={() => {
                            setAllExpansion?.(false);
                            setRepliesMenuOpen(false);
                          }}
                        >
                          {translate('DISCUSSION_POST_COLLAPSE_ALL')}
                        </p>
                      </span>
                    </div>
                  )}

                  {isThread && item.descendantCount === 0 && (
                    <div>
                      <span className="footer-text disabled">
                        <span>{translate('DISCUSSION_POST_NO_REPLIES')}</span>
                      </span>
                    </div>
                  )}
                </div>

                {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
                <div
                  className="discussion-footer-dropdown"
                  onClick={() => setMoreMenuOpen(o => !o)}
                >
                  <span
                    className="footer-text text-button text-dropdown"
                    title={translate('DISCUSSION_POST_FOOTER_DROPDOWN')}
                  >
                    <span className="icon icon-more-horizontal" />
                  </span>
                </div>

                <div className="discussion-footer-optional dropdown-menu">
                  {isInstructor && !item.removed && (
                    // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                    <span
                      className="footer-text text-button text-mark-inappropriate"
                      onClick={() => {
                        setInappropriate(!item.inappropriate);
                        setMoreMenuOpen(false);
                      }}
                    >
                      {translate('DISCUSSION_POST_TOGGLE_INAPPROPRIATE')}
                    </span>
                  )}
                  {isInstructor && !item.removed && (
                    // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                    <span
                      className="footer-text text-button text-deleted"
                      onClick={() => {
                        setRemoved(true);
                        setMoreMenuOpen(false);
                      }}
                    >
                      {translate('DISCUSSION_POST_TOGGLE_DELETED')}
                    </span>
                  )}
                  {isInstructor && item.removed && (
                    // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                    <span
                      className="footer-text text-button text-restore"
                      onClick={() => {
                        setRemoved(false);
                        setMoreMenuOpen(false);
                      }}
                    >
                      {translate('DISCUSSION_POST_TOGGLE_RESTORE')}
                    </span>
                  )}
                  {item.editable && !item.editInfo?.inEditMode && (
                    // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                    <span
                      className="footer-text text-button text-edit"
                      onClick={() => {
                        editStart();
                        setMoreMenuOpen(false);
                      }}
                    >
                      {translate('DISCUSSION_POST_EDIT')}
                    </span>
                  )}
                  {!isInstructor && (
                    // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
                    <span
                      className="footer-text text-button text-report-inappropriate"
                      onClick={() => {
                        reportPost();
                        setMoreMenuOpen(false);
                      }}
                    >
                      {translate('DISCUSSION_POST_REPORT_INAPPROPRIATE')}
                    </span>
                  )}

                  <span className="footer-text text-post-date">
                    <span className="create-date">
                      {translate('DISCUSSION_POST_CREATED_DATE', { createTime: item.createTime })}
                    </span>
                    <span className="edit-date">
                      {translate('DISCUSSION_POST_EDITED_DATE', { editTime: item.lastModified })}
                    </span>
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        {thread && thread.reply?.editing && thread.reply.replyToId === item.id && (
          <div className="discussion-write-reply">
            <DiscussionReplyEditor
              replyToName={item.user?.fullName}
              state={thread.reply}
              saveAction={replySave}
              discardAction={replyDiscard}
            />
          </div>
        )}
      </div>
    </Host>
  );
};

