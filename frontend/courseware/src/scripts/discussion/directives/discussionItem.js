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

import discussionItem from './discussionItem.html';
import discussionItemContent from './discussionItemContent.html';
import discussionItemHeader from './discussionItemHeader.html';
import discussionItemFooter from './discussionItemFooter.html';

import discussionReply from './discussionReply.jsx';
import autoUnread from './discussionItemAutoUnread.js';

import discussionActions from '../actions/index.js';

import modal from 'angular-ui-bootstrap/src/modal';
import confirmModal from '../../directives/confirmModal/index.js';

import User from '../../utilities/User.js';

export default angular
  .module('lo.discussion.post', [
    User.name,
    discussionReply.name,
    autoUnread.name,
    discussionActions.name,
    modal,
    confirmModal.name,
  ])
  .directive('discussionItemContent', function () {
    return {
      restrict: 'E',
      template: discussionItemContent,
      scope: false,
    };
  })
  .directive('discussionItemHeader', function () {
    return {
      restrict: 'E',
      template: discussionItemHeader,
      scope: false,
    };
  })
  .directive('discussionItemFooter', function () {
    return {
      restrict: 'E',
      template: discussionItemFooter,
      scope: false,
    };
  })
  .component('discussionItem', {
    template: discussionItem,
    bindings: {
      discussionId: '<',
      item: '<',
      thread: '<',
      isThread: '<',
      setAllExpansion: '<?',
      setAllViewed: '<?',
      settings: '<',
    },
    controller: [
      '$scope',
      '$ngRedux',
      '$uibModal',
      'User',
      'DiscussionLoadingActions',
      'DiscussionWritingActions',
      'DiscussionPostActions',
      'Settings',
      function (
        $scope,
        $ngRedux,
        $uibModal,
        User,
        DiscussionLoadingActions,
        DiscussionWritingActions,
        DiscussionPostActions,
        Settings
      ) {
        this.$onInit = () => {
          this.showTitle = Settings.isFeatureEnabled('DiscussionTitle');
          this.isInstructor = User.isStrictlyInstructor();
          const isOwnPost = User.handle === this.item.author.handle;
          this.automarkable = this.settings.autoMarkUnread && !isOwnPost;
          this.markableUnread = this.settings.canMarkUnread && !isOwnPost;
          this.markableNew = !this.settings.canMarkUnread && !isOwnPost;
          this.canPinThread = this.isThread && this.isInstructor;
          this.statusAsDropdown = false;

          let actions = {
            setViewed: DiscussionPostActions.makeSetViewedActionCreator(
              this.discussionId,
              this.thread.id,
              this.item.id
            ),
            setBookmarked: DiscussionPostActions.makeSetBookmarkedActionCreator(
              this.discussionId,
              this.item.id
            ),
            setInappropriate: DiscussionPostActions.makeSetInappropriateActionCreator(
              this.discussionId,
              this.item.id
            ),
            setRemoved: DiscussionPostActions.makeSetRemovedActionCreator(
              this.discussionId,
              this.item.id
            ),

            editStart: DiscussionWritingActions.makeEditStartActionCreator(
              this.discussionId,
              this.item.id
            ),
            editSave: DiscussionWritingActions.makeEditSaveActionCreator(
              this.discussionId,
              this.item.id
            ),
            editDiscard: DiscussionWritingActions.makeEditDiscardActionCreator(
              this.discussionId,
              this.item.id
            ),

            replyStart: DiscussionWritingActions.makeReplyStartActionCreator(
              this.discussionId,
              this.thread.id,
              this.item.id
            ),
            replySave: DiscussionWritingActions.makeReplySaveActionCreator(
              this.discussionId,
              this.thread.id,
              this.item.id
            ),
            replyDiscard: DiscussionWritingActions.makeReplyDiscardActionCreator(
              this.discussionId,
              this.thread.id,
              this.item.id
            ),

            toggleExpandPost: DiscussionPostActions.makeToggleExpandPostActionCreator(
              this.discussionId,
              this.item.id
            ),

            reportInappropriate: DiscussionPostActions.makeReportInappropriateActionCreator(
              this.discussionId,
              this.item.id
            ),
          };

          this.reportPost = () => {
            const msg = 'DISCUSSION_CONFIRM_REPORT_POST';
            const confirmPost = $uibModal.open({
              component: 'confirmModal',
              resolve: {
                message: ['$translate', $translate => $translate(msg)],
              },
            }).result;

            confirmPost.then(() => this.reportInappropriate());
          };

          if (this.thread) {
            actions = {
              ...actions,
              toggleExpandReplies: DiscussionPostActions.makeToggleExpandRepliesActionCreator(
                this.discussionId,
                this.item.id
              ),
              setPinned: DiscussionPostActions.makeSetPinnedActionCreator(
                this.discussionId,
                this.item.id
              ),
            };
          }

          $ngRedux.connectToCtrl(null, actions)(this);
        };
      },
    ],
  });
