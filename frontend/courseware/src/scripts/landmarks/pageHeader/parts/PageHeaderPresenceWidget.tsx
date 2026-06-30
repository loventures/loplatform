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
import React from 'react';
import { useDispatch } from 'react-redux';

// Aliasing Course to PresentScene
import PresentScene from '../../../bootstrap/course';
import { openGroupChatModal } from '../../../chat/groupChatModal.tsx';
import { useTranslation } from '../../../i18n/translationContext';
import { presenceService } from '../../../presence/presenceServiceImpl.ts';
import { presentConversations } from '../../../presence/presentConversationsImpl.ts';

/**
 * React port of the legacy `pageHeaderPresenceWidget` directive (B2): the course-nav presence
 * cluster (offline/reconnect indicator, group-chat button, present-users toggle). Previously an
 * Angular component bridged into React via angular2react; now native React (its only renderer is
 * the React `PageHeader`). Logic mirrors the already-native `ERHeaderPresenceWidget`, but this one
 * keeps the **legacy icon-font DOM** the Selenide suite targets (`#course-nav-offline`,
 * `#course-nav-group-chat`, `#course-nav-present-users`, `.presence-widget`, `.unread-count`,
 * `.unread-messages`, the `icon-connection` / `icon-bubble` / `icon-collaboration` glyphs). It re-renders
 * with its `connect`ed `PageHeader` parent as presence/conversation state dispatches land.
 */
export const PageHeaderPresenceWidget: React.FC<{
  showGroupChat: boolean;
  showPresenceChat: boolean;
}> = ({ showGroupChat, showPresenceChat }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const PresenceService = presenceService;
  const presenceState = PresenceService.state;
  const PresentConversations = presentConversations;
  const conversationState = PresentConversations.status;

  const reconnectPresence = (e: React.MouseEvent) => {
    e.preventDefault();
    PresenceService.reconnectPresence();
  };

  const openCourseChat = (e: React.MouseEvent) => {
    e.preventDefault();
    openGroupChatModal(PresentScene);
  };

  const toggleUserList = () => {
    dispatch({
      type: 'STATUS_FLAG_TOGGLE',
      sliceName: 'presentUsersPanelOpen',
    });
    PresentConversations.togglePanel();
  };

  return (
    <div className="d-flex">
      {presenceState.offline ? (
        // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
        <span
          className="presence-widget"
          id="course-nav-offline"
          onClick={reconnectPresence}
        >
          <span
            className={classnames('nav-icon', 'icon', {
              'icon-connection4': !presenceState.started,
              'icon-connection2': presenceState.started,
            })}
          />
        </span>
      ) : null}

      {presenceState.online && showGroupChat ? (
        // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
        <span
          className={classnames('presence-widget', {
            'unread-messages': conversationState.presentSceneUnreadCount,
          })}
          id="course-nav-group-chat"
          onClick={openCourseChat}
        >
          <span
            className="nav-icon icon icon-bubble"
            title={translate('GROUP_CHAT')}
          />
          <span className="sr-only">{translate('GROUP_CHAT')}</span>
          <span className="unread-count">{conversationState.presentSceneUnreadCount}</span>
        </span>
      ) : null}

      {presenceState.online && showPresenceChat ? (
        // eslint-disable-next-line jsx-a11y/no-static-element-interactions, jsx-a11y/click-events-have-key-events
        <span
          className={classnames('presence-widget', {
            'unread-messages': !!conversationState.unreadConversationCount,
          })}
          id="course-nav-present-users"
          onClick={toggleUserList}
        >
          <span
            className="nav-icon icon icon-collaboration"
            title={translate('VIEW_ONLINE_USERS')}
          />
          <span className="sr-only">{translate('VIEW_ONLINE_USERS')}</span>
          <span className="unread-count">{conversationState.unreadConversationCount}</span>
        </span>
      ) : null}
    </div>
  );
};

