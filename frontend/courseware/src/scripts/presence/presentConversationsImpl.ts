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

import { each, defaultTo, reduce, reverse, isNumber, map } from 'lodash';

import { courseReduxStore } from '../loRedux/index.ts';
// Aliasing Course to PresentScene
import PresentScene from '../bootstrap/course.ts';
import { setChatRoomLastUpdatedAction } from '../landmarks/chat/chatReducer.ts';
import { chatAPI } from '../services/chatAPI.ts';
import { currentUser } from '../utilities/currentUserData.ts';

import { Conversation } from './models/Conversation.ts';
import { presenceService } from './presenceServiceImpl.ts';
import { presentUsers } from './presentUsersImpl.ts';

/**
 * Pure-TS singleton port of the AngularJS `PresentConversations`. Monitors chat
 * conversations; registers for `ChatMessage` events.
 *
 * The current user's `handle` comes from the pure `currentUser` singleton
 * (utilities/currentUserData.ts); `ChatAPI` is the pure `chatAPI` singleton, imported
 * directly. `$q.when/all` become native Promises; `$ngRedux.dispatch` becomes
 * `courseReduxStore.dispatch`.
 */

export type ConversationStatus = {
  isPanelOpen: boolean;
  unreadConversationCount: number;
  presentSceneUnreadCount: number;
};

class PresentConversationsImpl {
  conversationByRoom: Record<string, any> = {};
  conversationByContext: Record<string, any> = {};
  senderProfiles: Record<string, any> = {};
  status: ConversationStatus = {
    isPanelOpen: false, // whether to show the user list
    unreadConversationCount: 0, // the number of unseen conversations
    presentSceneUnreadCount: 0, // unseen count of scene conversation
  };

  /**
   * get the conversation associated with a room
   */
  getConversationByRoom = (roomId: number): any => {
    if (!this.conversationByRoom[roomId]) {
      this.conversationByRoom[roomId] = new Conversation(roomId);
      // now look up the chat room so we can identify which course/user this is associated with
      chatAPI
        .getChatRoom(roomId)
        .then((room: any) => {
          if (room.chatType._type === 'user') {
            this.conversationByContext[room.chatType.handle] = this.conversationByRoom[roomId];
          } else if (room.chatType._type === 'context') {
            this.conversationByContext[room.chatType.context] = this.conversationByRoom[roomId];
          }
          this.updateUnreadConversationCount();
        });
    }
    return this.conversationByRoom[roomId];
  };

  /**
   * get the unread message count associated with a context (course or user).
   */
  getUnreadCountByContext = (context: any): number => {
    const conversation = this.conversationByContext[context.id];
    return conversation ? conversation.unreadCount : 0;
  };

  /**
   * Internal: Recompute state after an update, notify watchers of the new state.
   */
  updateUnreadConversationCount = (): void => {
    if (!this.status.isPanelOpen) {
      // If the user pane is not open, update the count of unseen conversations
      this.status.unreadConversationCount = reduce(
        this.conversationByContext,
        (total, convo, id) => {
          const scene = +id !== PresentScene.id && convo.hasNew() ? 1 : 0;
          return total + scene;
        },
        0
      );
    }
    const conversation = this.conversationByContext[PresentScene.id];
    this.status.presentSceneUnreadCount = conversation ? conversation.unreadCount : 0;
  };

  /**
   * Internal: Handle receipt of a chat message.
   */
  onChatMessage = (data: any): void => {
    if (data.message || data.sender !== currentUser.handle) {
      // ignore my own busy signals
      const roomId = data.room;
      const lastUpdated = new Date(data.timestamp).valueOf();
      const conversation = this.getConversationByRoom(data.room);
      conversation.addLine(data.id, data.sender, lastUpdated, data.message, data.typing);

      courseReduxStore.dispatch(setChatRoomLastUpdatedAction({ roomId, lastUpdated }));

      if (conversation === this.conversationByContext[PresentScene.id]) {
        this.status.presentSceneUnreadCount = conversation.unreadCount;
      }

      presentUsers.ensureProfile(data.sender);
      this.updateUnreadConversationCount();
    }
  };

  /**
   * Inform the present users service about a chat window opening or closing.
   */
  updateChatWindowStatus = (roomId: number, isOpen: boolean): void => {
    const conversation = this.getConversationByRoom(roomId);
    conversation.chatOpen = isOpen;
    if (isOpen) {
      conversation.unreadCount = 0;
      conversation.seenCount = 0;
      this.updateUnreadConversationCount();
    }
  };

  /**
   * Toggle the users pane.
   */
  togglePanel = (isOpen?: boolean): void => {
    this.status.isPanelOpen = defaultTo(isOpen, !this.status.isPanelOpen);
    // clear the unseen conversation count
    this.status.unreadConversationCount = 0;
    // reset the seen conversation count for each conversation
    each(this.conversationByRoom, c => (c.seenCount = c.unreadCount));
    // Delay caring about presence in the course until the pane is opened...
    presenceService.followScene({ context: PresentScene.id });
  };

  openOrCreateUserChat = (userHandle: string): PromiseLike<any> => {
    if (this.conversationByContext[userHandle]) {
      return Promise.resolve(this.conversationByContext[userHandle]);
    }

    return chatAPI
      .openUserChat(userHandle)
      .then((room: any) => {
        return this.getConversationByRoom(room.id);
      });
  };

  openOrCreateGroupChat = (sceneId: number): PromiseLike<any> => {
    if (this.conversationByContext[sceneId]) {
      return Promise.resolve(this.conversationByContext[sceneId]);
    }

    return chatAPI
      .openGroupChat(sceneId)
      .then((room: any) => {
        return this.getConversationByRoom(room.id);
      });
  };

  ensureHistoryForChatRoom = (roomId: number): void => {
    const conversation = this.getConversationByRoom(roomId);

    if (conversation.historyLoaded) {
      return;
    }

    chatAPI
      .getChatMessages(roomId)
      .then((messages: any[]) => {
        let lastUpdated: number | undefined;
        each(reverse(messages), msg => {
          const timestamp = new Date(msg.timestamp).valueOf();
          conversation.addLine(msg.id, msg.sender, timestamp, msg.message);
          if (!isNumber(lastUpdated)) {
            lastUpdated = timestamp;
          } else if (lastUpdated > timestamp) {
            lastUpdated = timestamp;
          }
        });

        const profilePromises = map(conversation.getUniqueUserHandles(), (handle: string) =>
          presentUsers.ensureProfile(handle)
        );

        Promise.all(profilePromises).then(() => {
          conversation.historyLoaded = true;
          courseReduxStore.dispatch(setChatRoomLastUpdatedAction({ roomId, lastUpdated: lastUpdated as number }));
        });
      });
  };

  constructor() {
    // Register to hear chat messages
    presenceService.on('ChatMessage', this.onChatMessage);
  }
}

export type PresentConversations = PresentConversationsImpl;

/** The interface React consumers type the singleton with (was the sibling .d.ts NgPresentConversations). */
export default interface NgPresentConversations {
  status: ConversationStatus;
  togglePanel: () => void;
  openOrCreateGroupChat: (sceneId: number) => PromiseLike<any>;
  updateChatWindowStatus: (roomId: number, isOpen: boolean) => void;
  getConversationByRoom: (roomId: number) => any;
  ensureHistoryForChatRoom: (roomId: number) => void;
}

export const presentConversations = new PresentConversationsImpl();
