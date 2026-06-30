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
import React, { useEffect, useReducer, useRef } from 'react';
import { useDispatch } from 'react-redux';

import { openUserChatModal } from '../../chat/userChatModal.tsx';
import { LoCheckbox } from '../../directives/LoCheckbox';
import { useTranslation } from '../../i18n/translationContext';
import { CourseState, useCourseSelector } from '../../loRedux';
import { presenceService } from '../../presence/presenceServiceImpl.ts';
import { presenceSession } from '../../presence/presenceSessionImpl.ts';
import { presentConversations } from '../../presence/presentConversationsImpl.ts';
import { presentUsers as presentUsersService } from '../../presence/presentUsersImpl.ts';

const PROFILE_FALLBACK = 'assets/images/Profile.png';

/**
 * React port of the `presentUsers` directive (B2): the right-hand sidebar panel listing the users
 * present in the current course, each a click-to-chat avatar with an unread-message badge, plus a
 * "visible to others" toggle. Previously an Angular component bridged into React via angular2react;
 * now native React (its renderers — `ERContentContainer`, `PageContainer` — are React). The panel's
 * data lives in the pure-TS presence singletons (`presentUsers.orderedPresentUsers`,
 * `presentConversations`, `presenceSession`), imported directly; it re-renders on the
 * `ScenePresence` (roster) and `ChatMessage` (unread) presence events. The jQuery `lo-autofocus`
 * directive becomes a ref/timeout focus and `err-src` becomes an `onError` fallback. DOM preserved
 * (`#present-users`, `.present-users-panel`, `.present-user*`, `.unread-messages`).
 */
export const OnlineUsersPanel: React.FC = () => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const isOpen = useCourseSelector((state: CourseState) => state.ui.presentUsersPanelOpen.status);
  const [, forceRender] = useReducer((x: number) => x + 1, 0);
  const innerRef = useRef<HTMLDivElement>(null);

  // Re-render when the roster (ScenePresence) or unread counts (ChatMessage) change.
  useEffect(() => {
    const deregisterPresence = presenceService.on('ScenePresence', forceRender);
    const deregisterChat = presenceService.on('ChatMessage', forceRender);
    return () => {
      deregisterPresence?.();
      deregisterChat?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // The old lo-autofocus="300": focus the panel shortly after it opens.
  useEffect(() => {
    if (!isOpen) return;
    const handle = window.setTimeout(() => innerRef.current?.focus(), 300);
    return () => window.clearTimeout(handle);
  }, [isOpen]);

  const presentUsers: any[] = presentUsersService.orderedPresentUsers;
  const conversationsByContext: Record<string, any> = presentConversations.conversationByContext;

  const closeUsers = (e: React.MouseEvent) => {
    e.preventDefault();
    dispatch({
      type: 'STATUS_FLAG_TOGGLE',
      sliceName: 'presentUsersPanelOpen',
      data: { status: false },
    });
    presentConversations.togglePanel(false);
  };

  const openChat = (e: React.MouseEvent, user: any) => {
    e.preventDefault();
    openUserChatModal(user);
  };

  const setVisible = (visible: boolean) => {
    presenceService.setVisibleToOthers(visible);
    forceRender();
  };

  return (
    <div className={classnames('panel', 'panel-right', 'present-users-panel', { open: isOpen })} id="present-users">
      {isOpen ? (
        <div
          className="panel-inner"
          ref={innerRef}
          tabIndex={0}
          aria-label={translate('PRESENCE_ONLINE_USERS')}
        >
          <header className="panel-header">
            <h1 className="panel-title">{translate('PRESENCE_ONLINE_USERS')}</h1>
            <button
              className="close-btn btn-close"
              onClick={closeUsers}
              title={translate('MODAL_CLOSE')}
            />
          </header>

          <div className="panel-block present-users-block">
            <ul className="present-users list-unstyled">
              {presentUsers.map(user => {
                const unreadCount = conversationsByContext[user.handle]?.unreadCount;
                return (
                  <li
                    key={user.handle}
                    className={classnames('present-user', user.presence)}
                  >
                    {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events */}
                    <div
                      className="present-user-circle"
                      onClick={e => openChat(e, user)}
                      title={user.fullName}
                    >
                      <div
                        className="present-user-photo"
                        style={{ backgroundColor: user.presenceColour }}
                        role="presentation"
                      >
                        {user.imageUrl ? (
                          <img
                            className="user-photo"
                            src={user.imageUrl}
                            alt=""
                            onError={e => {
                              const img = e.currentTarget;
                              if (img.src.indexOf(PROFILE_FALLBACK) === -1) img.src = PROFILE_FALLBACK;
                            }}
                          />
                        ) : (
                          <div className="user-letter">{user.presenceLetter}</div>
                        )}

                        <div
                          className={classnames('present-user-message-count', {
                            'unread-messages': !!unreadCount,
                          })}
                        >
                          {unreadCount}
                        </div>

                        <div
                          className="present-user-presence-indicator"
                          role="presentation"
                        />
                      </div>
                    </div>

                    <div className="present-user-name">{user.givenName || user.fullName}</div>
                  </li>
                );
              })}
            </ul>
          </div>

          <div className="panel-block">
            <LoCheckbox
              checkboxFor="presence-visible"
              checkboxLabel={translate('PRESENCE_VISIBLE')}
              onToggle={setVisible}
              state={presenceSession.visibility}
            />
          </div>
        </div>
      ) : null}
    </div>
  );
};

