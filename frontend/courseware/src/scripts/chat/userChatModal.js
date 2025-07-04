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

import template from './userChatModal.html';

import PresentConversations from '../presence/PresentConversations.js';

import ChatEmail from '../landmarks/chat/ChatEmail.js';
import { react2angular } from 'react2angular';

import ChatMessages from '../landmarks/chat/ChatMessages.js';
import { react2angularWithNgProvider } from '../utilities/ngReduxProvider.jsx';
import ErrSrc from '../utilities/errSrc.js';

export default angular
  .module('lo.chat.userChatModal', [PresentConversations.name, ErrSrc.name])
  /**
   * @ngdoc controller
   * @alias chatCtrl
   * @memberof lo.presence.directives
   * @description The chat controller backs an active chat window.
   */
  .component('userChatModal', {
    template,

    bindings: {
      resolve: '<',
      close: '&',
      dismiss: '&',
    },

    controller: [
      'PresentConversations',
      '$translate',
      function (PresentConversations, $translate) {
        this.emailView = false;

        this.$onInit = () => {
          //Upon opening the chat, inform the presence state.
          PresentConversations.openOrCreateUserChat(this.resolve.user.handle).then(conversation => {
            this.roomId = conversation.roomId;
            this.context = this.resolve.user;
            this.offlineMessage = $translate.instant('USER_CHAT_USER_OFFLINE', this.context);
            PresentConversations.updateChatWindowStatus(this.roomId, true);
          });
        };

        this.$onDestroy = () => {
          //Upon closing the chat,  update the presence state.
          if (this.roomId) {
            PresentConversations.updateChatWindowStatus(this.roomId, false);
          }
        };
      },
    ],
  })
  .component('chatEmailReact', react2angular(ChatEmail, ['recipientId']))
  .component(
    'chatMessagesReact',
    react2angularWithNgProvider(ChatMessages, [
      'roomId',
      'context',
      'chatToUser',
      'isContextOffline',
      'offlineMessage',
    ])
  );
