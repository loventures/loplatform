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

import { isFunction } from 'lodash';

import discussionReply from './discussionReply.html';

import uibModal from 'angular-ui-bootstrap/src/modal';

import errorModal from '../modals/DiscussionReplyForbiddenModal.js';

import { richTextEditor } from '../../contentEditor/index.js';

import FeedbackManager from '../../assignmentFeedback/FeedbackManager.js';

export default angular
  .module('lo.discussion.discussionReply', [
    uibModal,
    richTextEditor.name,
    FeedbackManager.name,
    errorModal.name,
  ])
  .controller('DiscussionReplyCtrl', [
    'FeedbackManager',
    'Settings',
    'DiscussionReplyForbiddenModal',
    '$scope',
    function (FeedbackManager, Settings, DiscussionReplyForbiddenModal, $scope) {
      $scope.submitting = false;

      this.$onInit = () => {
        if (this.post && this.post.content) {
          this.contentInEdit = this.post.content;
        } else if (this.replyToName) {
          this.contentInEdit = '<p><strong>+' + this.replyToName + '</strong>&nbsp;</p>';
        } else {
          this.contentInEdit = '';
        }

        if (this.post && this.post.title) {
          this.contentTitle = this.post.title;
        }

        if (!this.post || this.post.depth === 0) {
          this.canEditTitle = true;
        }

        this.feedbackManager = new FeedbackManager(this.attachments);
      };

      this.$onChanges = ({ state }) => {
        if (state) {
          const current = state.currentValue;
          const prev = state.previousValue;

          $scope.submitting = current.saving;

          if (current.error && current.error.type === 'UNAUTHORIZED_ERROR') {
            DiscussionReplyForbiddenModal.open(current.error).then(keepWork => {
              if (!keepWork) {
                this.discard();
              } else {
                if (isFunction(this.keepWorkingAction)) {
                  this.keepWorkingAction();
                }
              }
            });
          } else if (current.error) {
            this.error = current.error;
          } else if (!current.saving && prev.saving) {
            this.contentTitle = null;
            this.contentInEdit = '';
            this.feedbackManager.clearStageFiles();
          }
        }
      };

      this.contentEdited = contentEdited => {
        this.contentInEdit = contentEdited;
      };

      this.discard = () => {
        if (isFunction(this.discardAction)) {
          this.discardAction();
        }
        this.contentTitle = null;
        this.contentInEdit = '';
      };

      this.canSubmitReply = () => {
        if (this.cannotEdit) {
          return false;
        }

        if (this.state.saving) {
          return false;
        }

        //CBLPROD-997 Advisors cannot submit a reply
        if (
          !Settings.isFeatureEnabled('LearnCourseRight') &&
          !Settings.isFeatureEnabled('TeachCourseRight')
        ) {
          return false;
        }

        //CBLPROD-17003 Allow a file without input text
        if (this.feedbackManager.hasStagedOrOngoing()) {
          return this.feedbackManager.isReady();
        }

        return !!this.contentInEdit && !!this.contentInEdit.length;
      };

      this.submit = () =>
        this.saveAction(
          this.contentTitle,
          this.contentInEdit,
          this.feedbackManager.getFilesInStaging(),
          this.feedbackManager.getRemovalsInStaging(),
          this.feedbackManager.getAttachedFiles()
        );
    },
  ])
  .component('discussionWriteThread', {
    template: discussionReply,
    controller: 'DiscussionReplyCtrl',
    bindings: {
      saveAction: '<',
      state: '<',
      discardAction: '<',
      keepWorkingAction: '<',
      showTitle: '<',
    },
  })
  .component('discussionWriteReply', {
    template: discussionReply,
    controller: 'DiscussionReplyCtrl',
    bindings: {
      replyToName: '<',
      state: '<',
      saveAction: '<',
      discardAction: '<',
      showTitle: '<',
    },
  })
  .component('discussionEditPost', {
    template: discussionReply,
    controller: 'DiscussionReplyCtrl',
    bindings: {
      cannotEdit: '<?',

      state: '<',

      post: '<?',
      attachments: '<',

      saveAction: '<',
      discardAction: '<',
      showTitle: '<',
    },
  });
