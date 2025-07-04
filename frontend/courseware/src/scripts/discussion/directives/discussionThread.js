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

import { filter } from 'lodash';

import discussionThread from './discussionThread.html';

import discussionActions from '../actions/index.js';

import discussionItem from './discussionItem.js';

import User from '../../utilities/User.js';

export default angular
  .module('lo.discussion.thread', [User.name, discussionItem.name, discussionActions.name])
  .component('discussionThread', {
    template: discussionThread,
    bindings: {
      discussionId: '<',
      thread: '<',
      showOrphan: '<?',
      settings: '<',
    },
    controller: [
      '$scope',
      '$ngRedux',
      'User',
      'DiscussionPostActions',
      'DiscussionLoadingActions',
      function ($scope, $ngRedux, User, DiscussionPostActions, DiscussionLoadingActions) {
        this.$onInit = () => {
          this.isInstructor = User.isStrictlyInstructor();

          $ngRedux.connectToCtrl(null, {
            loadReplies: DiscussionLoadingActions.makeLoadRepliesActionCreator(
              this.discussionId,
              this.thread.id,
              this.isInstructor
            ),

            _setAllExpansion: DiscussionPostActions.makeSetAllExpansionActionCreator(
              this.discussionId,
              this.thread.id
            ),
            _setAllViewed: DiscussionPostActions.makeBatchSetViewedActionCreator(
              this.discussionId,
              this.thread.id
            ),
          })(this);
        };

        this.initReplies = () => {
          if (this.thread.replies.length === 0 && this.thread.repliesRemaining > 0) {
            this.loadReplies();
          }
        };

        this.loadMoreReplies = () => this.loadReplies(this.thread.replies.length);

        this.setAllExpansion = expansion => this._setAllExpansion(expansion, this.thread.replies);
        this.setAllViewed = viewed =>
          this._setAllViewed(
            viewed,
            filter(
              this.thread.replies,
              post =>
                //=== because unread and viewed are opposites
                post.isUnread === viewed && !post.isCurrentUserPost
            )
          );
      },
    ],
  });
