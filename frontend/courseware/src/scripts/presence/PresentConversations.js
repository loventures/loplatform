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

import { each, defaultTo, reduce, reverse, isNumber, map } from 'lodash';

import User from '../utilities/User.js';

import Conversation from './models/Conversation.js';
import ChatAPI from '../services/ChatAPI.js';

import PresenceService from './PresenceService.js';
import PresentUsers from './PresentUsers.js';
// Aliasing Course to PresentScene
import PresentScene from '../bootstrap/course.js';
import { setChatRoomLastUpdatedAction } from '../landmarks/chat/chatReducer.js';

export default angular
  .module('lo.presence.PresentConversations', [
    PresenceService.name,
    PresentUsers.name,
    User.name,
    ChatAPI.name,
    Conversation.name,
  ])
  /**
   * @ngdoc service
   * @alias PresentUsers
   * @memberof lo.presence.PresentConversations
   * @description Service for monitoring the conversations.
   */
  .service('PresentConversations', [
    'PresenceService',
    'PresentUsers',
    'ChatAPI',
    'User',
    'Conversation',
    '$q',
    '$ngRedux',
    function (PresenceService, PresentUsers, ChatAPI, User, Conversation, $q, $ngRedux) {
      const service = {
        conversationByRoom: {},
        conversationByContext: {},
        senderProfiles: {},
        status: {
          isPanelOpen: false, // whether to show the user list
          unreadConversationCount: 0, // the number of unseen conversations
          presentSceneUnreadCount: 0, //unseen count of scene conversation
        },
      };

      /**
       * @description get the conversation associated with a room
       * @params {number} id the room id
       * @returns the associated conversation
       */
      service.getConversationByRoom = roomId => {
        if (!service.conversationByRoom[roomId]) {
          service.conversationByRoom[roomId] = new Conversation(roomId);
          // now look up the chat room so we can identify which course/user this is associated with
          ChatAPI.getChatRoom(roomId).then(room => {
            if (room.chatType._type === 'user') {
              service.conversationByContext[room.chatType.handle] =
                service.conversationByRoom[roomId];
            } else if (room.chatType._type === 'context') {
              service.conversationByContext[room.chatType.context] =
                service.conversationByRoom[roomId];
            }
            service.updateUnreadConversationCount();
          });
        }
        return service.conversationByRoom[roomId];
      };

      /**
       * @description get the unread message count associated with a context (course or user).
       * @params {Object} context the context
       * @returns the unread message count
       */
      service.getUnreadCountByContext = context => {
        const conversation = service.conversationByContext[context.id];
        return conversation ? conversation.unreadCount : 0;
      };

      /**
       * @description Internal: Recompute state after an update, notify watchers of the new state.
       * @params {boolean} doScroll whether to scroll down any open chat windows
       */
      service.updateUnreadConversationCount = () => {
        if (!service.status.isPanelOpen) {
          // If the user pane is not open, update the count of unseen conversations
          service.status.unreadConversationCount = reduce(
            service.conversationByContext,
            (total, convo, id) => {
              const scene = +id !== PresentScene.id && convo.hasNew() ? 1 : 0;
              return total + scene;
            },
            0
          );
        }
        const conversation = service.conversationByContext[PresentScene.id];
        service.status.presentSceneUnreadCount = conversation ? conversation.unreadCount : 0;
      };

      /**
       * @description Internal: Handle receipt of a chat message.
       * @params {Object} data the chat message data
       */
      service.onChatMessage = data => {
        if (data.message || data.sender !== User.handle) {
          // ignore my own busy signals
          const roomId = data.room;
          const lastUpdated = new Date(data.timestamp).valueOf();
          const conversation = service.getConversationByRoom(data.room);
          conversation.addLine(data.id, data.sender, lastUpdated, data.message, data.typing);

          $ngRedux.dispatch(setChatRoomLastUpdatedAction({ roomId, lastUpdated }));

          if (conversation === service.conversationByContext[PresentScene.id]) {
            service.status.presentSceneUnreadCount = conversation.unreadCount;
          }

          PresentUsers.ensureProfile(data.sender);
          service.updateUnreadConversationCount();
        }
      };

      // Register to hear chat messages
      PresenceService.on('ChatMessage', service.onChatMessage);

      /**
       * @description Inform the present users service about a chat window opening or closing.
       * @params {Object} room the chat room
       * @params {boolean} open whether the chat window opened or closed
       */
      service.updateChatWindowStatus = (roomId, isOpen) => {
        let conversation = service.getConversationByRoom(roomId);
        conversation.chatOpen = isOpen;
        if (isOpen) {
          conversation.unreadCount = 0;
          conversation.seenCount = 0;
          service.updateUnreadConversationCount();
        }
      };

      /**
       * @description Toggle the users pane.
       * @params {boolean} show whether to show or hide the pane. if null then toggle state.
       */
      service.togglePanel = isOpen => {
        service.status.isPanelOpen = defaultTo(isOpen, !service.status.isPanelOpen);
        // clear the unseen conversation count
        service.status.unreadConversationCount = 0;
        // reset the seen conversation count for each conversation
        each(service.conversationByRoom, c => (c.seenCount = c.unreadCount));
        // Delay caring about presence in the course until the pane is opened...
        PresenceService.followScene({ context: PresentScene.id });
      };

      service.openOrCreateUserChat = userHandle => {
        if (service.conversationByContext[userHandle]) {
          return $q.when(service.conversationByContext[userHandle]);
        }

        return ChatAPI.openUserChat(userHandle).then(room => {
          return service.getConversationByRoom(room.id);
        });
      };

      service.openOrCreateGroupChat = sceneId => {
        if (service.conversationByContext[sceneId]) {
          return $q.when(service.conversationByContext[sceneId]);
        }

        return ChatAPI.openGroupChat(sceneId).then(room => {
          return service.getConversationByRoom(room.id);
        });
      };

      service.ensureHistoryForChatRoom = roomId => {
        const conversation = service.getConversationByRoom(roomId);

        if (conversation.historyLoaded) {
          return;
        }

        ChatAPI.getChatMessages(roomId).then(messages => {
          let lastUpdated;
          each(reverse(messages), msg => {
            const timestamp = new Date(msg.timestamp).valueOf();
            conversation.addLine(msg.id, msg.sender, timestamp, msg.message);
            if (!isNumber(lastUpdated)) {
              lastUpdated = timestamp;
            } else if (lastUpdated > timestamp) {
              lastUpdated = timestamp;
            }
          });

          const profilePromises = map(conversation.getUniqueUserHandles(), handle =>
            PresentUsers.ensureProfile(handle)
          );

          $q.all(profilePromises).then(() => {
            conversation.historyLoaded = true;
            $ngRedux.dispatch(setChatRoomLastUpdatedAction({ roomId, lastUpdated }));
          });
        });
      };

      return service;
    },
  ]);
