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

import template from './groupChatModal.html';

import PresentConversations from '../presence/PresentConversations.js';

export default angular
  .module('lo.chat.groupChatModal', [PresentConversations.name])
  /**
   * @ngdoc controller
   * @alias chatCtrl
   * @memberof lo.presence.directives
   * @description The chat controller backs an active chat window.
   */
  .component('groupChatModal', {
    template,

    bindings: {
      resolve: '<',
      close: '&',
      dismiss: '&',
    },

    controller: [
      'PresentConversations',
      function (PresentConversations) {
        this.$onInit = () => {
          //Upon opening the chat, inform the presence state.
          PresentConversations.openOrCreateGroupChat(this.resolve.scene.id).then(conversation => {
            this.roomId = conversation.roomId;
            this.context = this.resolve.scene;
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
  });
