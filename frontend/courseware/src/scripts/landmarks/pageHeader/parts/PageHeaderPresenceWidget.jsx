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

import { angular2react } from 'angular2react';

import template from './presenceWidget.html';

import groupChatModal from '../../../chat/groupChatModal';
// Aliasing Course to PresentScene
import PresentScene from '../../../bootstrap/course';

const component = {
  template,

  bindings: {
    showGroupChat: '<',
    showPresenceChat: '<',
  },

  controller: [
    '$uibModal',
    '$ngRedux',
    'PresenceService',
    'PresentConversations',
    function ($uibModal, $ngRedux, PresenceService, PresentConversations) {
      this.presenceState = PresenceService.state;

      this.conversationState = PresentConversations.status;

      /*
       * Reconnect presence.
       */
      this.reconnectPresence = $event => {
        $event.preventDefault();
        PresenceService.reconnectPresence();
      };

      /*
       * Open the course chat window.
       */
      this.openCourseChat = $event => {
        $event.preventDefault();

        $uibModal.open({
          component: 'groupChatModal',
          resolve: {
            scene: () => PresentScene,
          },
          size: 'lg',
          backdrop: 'static',
        });
      };

      /*
       * Toggle the present users panel.
       */
      this.toggleUserList = () => {
        $ngRedux.dispatch({
          type: 'STATUS_FLAG_TOGGLE',
          sliceName: 'presentUsersPanelOpen',
        });
        PresentConversations.togglePanel();
      };
    },
  ],
};

export let PageHeaderPresenceWidget = prop => (
  <div {...prop}>'PageHeaderPresenceWidget: ng module not found'</div>
);

const ngPageHeaderPresenceWidget = angular
  .module('lof.landmarks.pageHeader.pageHeaderPresenceWidget', [groupChatModal.name])
  .component('pageHeaderPresenceWidget', component)
  .run([
    '$injector',
    function ($injector) {
      PageHeaderPresenceWidget = angular2react('pageHeaderPresenceWidget', component, $injector);
    },
  ]);

export { ngPageHeaderPresenceWidget };
