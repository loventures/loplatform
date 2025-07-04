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

import classnames from 'classnames';
import PresentScene from '../../../bootstrap/course';
import { useTranslation } from '../../../i18n/translationContext';
import { NgPresenceService } from '../../../presence/PresenceService';
import NgPresentConversations from '../../../presence/PresentConversations';
import React from 'react';
import { GrGroup } from 'react-icons/gr';
import { IoChatboxOutline, IoWifi, IoWifiOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { NgUibModal } from '../../../ng';
import { lojector } from '../../../loject';

const ERHeaderPresenceWidget: React.FC<{ showGroupChat: boolean; showPresenceChat: boolean }> = ({
  showGroupChat,
  showPresenceChat,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const PresenceService: NgPresenceService = lojector.get('PresenceService');
  const presenceState = PresenceService.state;
  const PresentConversations: NgPresentConversations = lojector.get('PresentConversations');
  const conversationStatus = PresentConversations.status;
  const $uibModal: NgUibModal = lojector.get('$uibModal');

  const reconnectPresence = () => {
    PresenceService.reconnectPresence();
  };

  const openCourseChat = () => {
    $uibModal.open({
      component: 'groupChatModal',
      resolve: {
        scene: () => PresentScene,
      },
      size: 'lg',
      backdrop: 'static',
    });
  };

  const toggleUserList = () => {
    dispatch({
      type: 'STATUS_FLAG_TOGGLE',
      sliceName: 'presentUsersPanelOpen',
    });
    PresentConversations.togglePanel();
  };

  /**
   * TODO: Icons aren't always centered vertically. d-flex on the button fixes but is that what we want?
   * */
  return (
    <div className="d-none d-md-flex align-items-center">
      {presenceState.offline ? (
        <button
          className="presence-widget btn btn-outline-primary border-white"
          id="course-nav-offline"
          onClick={() => reconnectPresence()}
          title={translate(presenceState.started ? 'PRESENCE_RECONNECTING' : 'PRESENCE_RECONNECT')}
        >
          {presenceState.started ? (
            <IoWifi
              size="1.5rem"
              aria-hidden={true}
            />
          ) : (
            <IoWifiOutline
              size="1.5rem"
              aria-hidden={true}
            />
          )}
          <span className="sr-only">
            {translate(presenceState.started ? 'PRESENCE_RECONNECTING' : 'PRESENCE_RECONNECT')}
          </span>
        </button>
      ) : null}

      {presenceState.online && showGroupChat ? (
        <button
          className={classnames('presence-widget', 'btn', 'btn-outline-primary', 'border-white', {
            'unread-messages': conversationStatus.presentSceneUnreadCount,
          })}
          id="course-nav-group-chat"
          onClick={() => openCourseChat()}
          title={translate('GROUP_CHAT')}
        >
          <IoChatboxOutline
            size="1.5rem"
            className="thin-chat-icon"
            aria-hidden={true}
          />
          <span className="sr-only">{translate('GROUP_CHAT')}</span>
          <span className="unread-count">{conversationStatus.presentSceneUnreadCount}</span>
        </button>
      ) : null}

      {presenceState.online && showPresenceChat ? (
        <button
          className={classnames('presence-widget', 'btn', 'btn-outline-primary', 'border-white', {
            'unread-messages': conversationStatus.unreadConversationCount,
          })}
          id="course-nav-present-users"
          onClick={() => toggleUserList()}
          title={translate('VIEW_ONLINE_USERS')}
        >
          <GrGroup
            size="1.5rem"
            className="thin-group-icon"
            aria-hidden={true}
          />
          <span className="sr-only">{translate('VIEW_ONLINE_USERS')}</span>
          <span className="unread-count">{conversationStatus.unreadConversationCount}</span>
        </button>
      ) : null}
    </div>
  );
};

export default ERHeaderPresenceWidget;
