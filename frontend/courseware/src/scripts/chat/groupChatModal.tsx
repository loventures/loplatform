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

import React, { useEffect, useState } from 'react';

import { ModalControls, openReactModal } from '../directives/modalHost/reactModalHost.tsx';
import { useTranslation } from '../i18n/translationContext';
import ChatMessages from '../landmarks/chat/ChatMessages.tsx';
import { presentConversations } from '../presence/presentConversationsImpl.ts';

/**
 * React port of the `groupChatModal` component (chat subsystem): the course group chat window. Was a
 * thin Angular `$uibModal` shell around the already-React `ChatMessages`; now a native React modal body
 * opened through the B0 modal host (`openReactModal`). On open it asks the `presentConversations`
 * singleton for the group room and flags the chat window open; on unmount it flags it closed. DOM
 * preserved for Selenide (`ChatModal.GroupChat`): `#group-chat`, `.modal-header.chat-header`,
 * `.btn-close`, and the `ChatMessages` internals.
 */
const GroupChatModalBody: React.FC<ModalControls & { scene: any }> = ({ close, scene }) => {
  const translate = useTranslation();
  const [roomId, setRoomId] = useState<any>(null);

  useEffect(() => {
    const pc: any = presentConversations;
    let openedRoomId: any = null;
    pc.openOrCreateGroupChat(scene.id).then((conversation: any) => {
      openedRoomId = conversation.roomId;
      setRoomId(conversation.roomId);
      pc.updateChatWindowStatus(conversation.roomId, true);
    });
    return () => {
      if (openedRoomId) pc.updateChatWindowStatus(openedRoomId, false);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div id="group-chat">
      <div className="modal-header chat-header">
        <div className="flex-row-content w-100">
          <h3 className="modal-title flex-col-fluid">{translate('GROUP_CHAT_TITLE', scene)}</h3>
          <button
            className="btn-close"
            type="button"
            onClick={() => close()}
            title={translate('MODAL_CLOSE')}
            aria-label={translate('MODAL_CLOSE')}
          />
        </div>
      </div>
      {roomId && (
        <ChatMessages
          roomId={roomId}
          chatToUser={false}
        />
      )}
    </div>
  );
};

/** Open the course group chat window. Rejection (ESC/backdrop) is swallowed — callers fire-and-forget. */
export const openGroupChatModal = (scene: any) =>
  openReactModal(controls => (
    <GroupChatModalBody
      {...controls}
      scene={scene}
    />
  ), { size: 'lg', backdrop: 'static' }).catch(() => {});

export default GroupChatModalBody;
