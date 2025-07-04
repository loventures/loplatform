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

import { loConfig } from '../bootstrap/loConfig.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular.module('lo.services.ChatAPI', []).service('ChatAPI', [
  'Request',
  function ChatAPI(Request) {
    const service = {};

    /**
     * Open a chat.
     * @param(object) o the user or course with which to open a chat
     */
    service.openUserChat = userId => {
      return Request.promiseRequest(loConfig.presence.openChat, 'post', {
        _type: 'user',
        handle: userId,
      });
    };

    service.openGroupChat = contextId => {
      return Request.promiseRequest(loConfig.presence.openChat, 'post', {
        _type: 'context',
        context: contextId,
      });
    };

    /**
     * Get chat room information.
     * @param(number) chatRoomId
     */
    service.getChatRoom = chatRoomId => {
      const url = new UrlBuilder(loConfig.presence.getChat, {
        id: chatRoomId,
      });
      return Request.promiseRequest(url, 'get');
    };

    /**
     * Send a chat message to a chat room.
     * @param(number) chatRoomId
     * @param(object) chatMessage
     * @return a promise of the message being sent
     */
    service.sendChatMessage = (chatRoomId, chatMessage) => {
      const url = new UrlBuilder(loConfig.presence.chatMessages, {
        id: chatRoomId,
      });
      return Request.promiseRequest(url, 'post', chatMessage);
    };

    service.notifyTypingStatus = (chatRoomId, isTyping) => {
      return service.sendChatMessage(chatRoomId, { typing: isTyping });
    };

    /**
     * Get chat messages.
     * @param(number) chatRoomId
     * @return a promise of the chat messages
     */
    service.getChatMessages = chatRoomId => {
      const url = new UrlBuilder(
        loConfig.presence.chatMessages,
        {
          id: chatRoomId,
        },
        {
          limit: 12,
        }
      );
      return Request.promiseRequest(url, 'get');
    };

    return service;
  },
]);
