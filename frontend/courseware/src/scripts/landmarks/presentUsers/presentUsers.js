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

import template from './presentUsers.html';

import PresenceService from '../../presence/PresenceService';
import PresenceSession from '../../presence/PresenceSession';
import PresentUsers from '../../presence/PresentUsers';
import PresentConversations from '../../presence/PresentConversations';

import userChatModal from '../../chat/userChatModal';
import autofocus from '../../utilities/autofocus';
import ErrSrc from '../../utilities/errSrc';

const component = {
  template,
  controller: [
    'PresenceService',
    'PresenceSession',
    'PresentConversations',
    'PresentUsers',
    '$uibModal',
    '$ngRedux',
    function (
      PresenceService,
      PresenceSession,
      PresentConversations,
      PresentUsers,
      $uibModal,
      $ngRedux
    ) {
      // The number of columns in which to render users
      const COLUMNS = 2;

      this.presentUsers = PresentUsers.orderedPresentUsers;

      this.conversationStatus = PresentConversations.status;

      this.conversationsByContext = PresentConversations.conversationByContext;

      this.presenceSession = PresenceSession;

      $ngRedux.connectToCtrl(state => ({
        isOpen: state.ui.presentUsersPanelOpen.status,
      }))(this);

      this.closeUsers = () => {
        $ngRedux.dispatch({
          type: 'STATUS_FLAG_TOGGLE',
          sliceName: 'presentUsersPanelOpen',
          data: { status: false },
        });
        PresentConversations.togglePanel(false);
      };

      /**
       * Return the CSS style describing the position and size of a user icon. This is necessary
       * in order to smoothly animate users shuffling; float layout is otherwise instantaneous.
       */
      this.userStyle = i => {
        const x = i % COLUMNS;
        const y = Math.floor(i / COLUMNS);
        return {
          transform: `translate3d(${x * 100}%, ${y * 100}%, 0)`,
          width: `${100 / COLUMNS}%`,
          height: `${100 / COLUMNS}%`,
        };
      };

      /**
       * Open a chat window with a particular user.
       */
      this.openChat = ($event, user) => {
        $event.preventDefault();
        $uibModal.open({
          component: 'userChatModal',
          resolve: {
            user: () => user,
          },
          size: 'lg',
          backdrop: 'static',
        });
      };

      /**
       * Set my visibility.
       */
      this.setVisible = visible => {
        this.isVisible = PresenceService.setVisibleToOthers(visible);
      };
    },
  ],
};

import { angular2react } from 'angular2react';

export let OnlineUsersPanel = 'OnlineUsersPanel: ng module not found';

export default angular
  .module('lo.landmarks.presentUsers', [
    userChatModal.name,
    autofocus.name,

    PresenceService.name,
    PresenceSession.name,
    PresentUsers.name,
    PresentConversations.name,
    ErrSrc.name,
  ])
  /**
   * @ngdoc directive
   * @alias presentUsers
   * @memberof lo.presence.directives
   * @description The present users directive renders a sidebar panel with the users present in the current course.
   */
  .component('presentUsers', component)
  .run([
    '$injector',
    function ($injector) {
      OnlineUsersPanel = angular2react('presentUsers', component, $injector);
    },
  ]);
