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

import { loConfig } from '../../bootstrap/loConfig.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';
import type { RequestFn } from './discussionSummaryAPI.ts';

/**
 * Chat (presence) API, migrated verbatim from the AngularJS `ChatAPI` service to
 * plain TS taking an injected `request`.
 */
export const makeChatAPI = (request: RequestFn) => {
  const service = {
    /** Open a chat with a user. */
    openUserChat(userId: any) {
      return request(loConfig.presence.openChat, 'post', { _type: 'user', handle: userId });
    },

    openGroupChat(contextId: any) {
      return request(loConfig.presence.openChat, 'post', { _type: 'context', context: contextId });
    },

    getChatRoom(chatRoomId: any) {
      const url = new (UrlBuilder as any)(loConfig.presence.getChat, { id: chatRoomId });
      return request(url, 'get');
    },

    sendChatMessage(chatRoomId: any, chatMessage: any) {
      const url = new (UrlBuilder as any)(loConfig.presence.chatMessages, { id: chatRoomId });
      return request(url, 'post', chatMessage);
    },

    notifyTypingStatus(chatRoomId: any, isTyping: boolean) {
      return service.sendChatMessage(chatRoomId, { typing: isTyping });
    },

    getChatMessages(chatRoomId: any) {
      const url = new (UrlBuilder as any)(loConfig.presence.chatMessages, { id: chatRoomId }, { limit: 12 });
      return request(url, 'get');
    },
  };

  return service;
};

export type ChatAPI = ReturnType<typeof makeChatAPI>;
