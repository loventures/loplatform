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
import React, { useEffect, useReducer, useState } from 'react';

import { ModalControls, openReactModal } from '../directives/modalHost/reactModalHost.tsx';
import { useTranslation } from '../i18n/translationContext';
import ChatEmail from '../landmarks/chat/ChatEmail.tsx';
import ChatMessages from '../landmarks/chat/ChatMessages.tsx';
import { presenceService } from '../presence/presenceServiceImpl.ts';
import { presentConversations } from '../presence/presentConversationsImpl.ts';

/**
 * React port of the `userChatModal` component (chat subsystem): the 1:1 user chat window. Was a thin
 * Angular `$uibModal` shell around the already-React `ChatMessages` / `ChatEmail`; now a native React
 * modal body opened through the B0 modal host (`openReactModal`, which supplies the redux + query +
 * i18n providers `ChatMessages` needs). On open it asks the `presentConversations` singleton
 * for the room and flags the chat window open; on unmount it flags it closed. DOM preserved for
 * Selenide (`ChatModal.UserChat`): `#user-chat`, `.modal-header.chat-header`, `.btn-close`, the two
 * `.form-check-label input` radios (message / email), and the `ChatMessages` / `ChatEmail` internals.
 */
const UserChatModalBody: React.FC<ModalControls & { user: any }> = ({ close, user }) => {
  const translate = useTranslation();
  const [emailView, setEmailView] = useState(false);
  const [roomId, setRoomId] = useState<any>(null);
  const [, forceRender] = useReducer((c: number) => c + 1, 0);
  const context = user;
  const offlineMessage = translate('USER_CHAT_USER_OFFLINE', context);

  useEffect(() => {
    const pc: any = presentConversations;
    let openedRoomId: any = null;
    pc.openOrCreateUserChat(context.handle).then((conversation: any) => {
      openedRoomId = conversation.roomId;
      setRoomId(conversation.roomId);
      pc.updateChatWindowStatus(conversation.roomId, true);
    });
    return () => {
      if (openedRoomId) pc.updateChatWindowStatus(openedRoomId, false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // `context` is the live roster user object whose `.presence` is mutated by presence events; the old
  // Angular binding re-evaluated `context.presence === 'Offline'` on every digest. Mirror that by
  // re-rendering on presence changes so the offline alert appears when the other user goes invisible.
  useEffect(() => {
    const deregister = presenceService.on('ScenePresence', forceRender);
    return () => deregister?.();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div id="user-chat">
      <div
        id="user-chat-header"
        className="modal-header chat-header user-chat"
      >
        <div className="flex-row-content w-100">
          <div className={classnames('chat-header-present-user present-user', context.presence)}>
            <div
              className="present-user-circle"
              role="presentation"
            >
              <div
                className="present-user-photo"
                style={{ backgroundColor: context.presenceColour }}
              >
                {context.imageUrl ? (
                  <img
                    className="user-photo"
                    src={context.imageUrl}
                    alt=""
                    onError={e => {
                      e.currentTarget.onerror = null;
                      e.currentTarget.src = 'assets/images/Profile.png';
                    }}
                  />
                ) : (
                  <div className="user-letter">{context.presenceLetter}</div>
                )}
              </div>
              <div
                className="present-user-presence-indicator"
                role="presentation"
              />
            </div>
          </div>

          <h3 className="modal-title flex-col-fluid">{translate('USER_CHAT_TITLE', context)}</h3>

          <button
            className="btn-close"
            type="button"
            onClick={() => close()}
            title={translate('MODAL_CLOSE')}
            aria-label={translate('MODAL_CLOSE')}
          />
        </div>
      </div>

      <div className="modal-body chat-body flex-row-content m-2">
        <div className="form-check form-check-inline">
          <label className="form-check-label">
            <input
              className="form-check-input"
              name="chat-input-radio"
              type="radio"
              checked={!emailView}
              onChange={() => setEmailView(false)}
            />
            <span>{translate('CHAT_SEND_MESSAGE')}</span>
          </label>
        </div>
        <div className="form-check form-check-inline">
          <label className="form-check-label">
            <input
              className="form-check-input"
              name="chat-input-radio"
              type="radio"
              checked={emailView}
              onChange={() => setEmailView(true)}
            />
            <span>{translate('CHAT_SEND_EMAIL')}</span>
          </label>
        </div>
      </div>

      {!emailView && (
        <div>
          {roomId && (
            <ChatMessages
              roomId={roomId}
              chatToUser={true}
              isContextOffline={context.presence === 'Offline'}
              offlineMessage={offlineMessage}
            />
          )}
        </div>
      )}

      {emailView && (
        <div>
          <ChatEmail recipientId={context.handle} />
        </div>
      )}
    </div>
  );
};

/** Open the 1:1 user chat window. Rejection (ESC/backdrop) is swallowed — callers fire-and-forget. */
export const openUserChatModal = (user: any) =>
  openReactModal(controls => (
    <UserChatModalBody
      {...controls}
      user={user}
    />
  ), { size: 'lg', backdrop: 'static' }).catch(() => {});

export default UserChatModalBody;
