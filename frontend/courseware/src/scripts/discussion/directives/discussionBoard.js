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

import { isFunction, mapValues } from 'lodash';

import modal from 'angular-ui-bootstrap/src/modal';

import discussionBoard from './discussionBoard.html';

import discussionOrders from '../services/discussionOrders.js';

import discussionBoardSettings from './discussionBoardSettings.js';
import jumpBar from './jumpBar.js';
import discussionBoardSearch from './discussionBoardSearch.js';
import discussionBoardThreadsView from './discussionBoardThreadsView.js';
import discussionBoardSingleThreadView from './discussionBoardSingleThreadView.js';
import discussionReply from './discussionReply.jsx';

import actions from '../actions/index.js';

import { createBoardSelector } from '../selectors.js';

import { angular2react } from 'angular2react';

const component = {
  template: discussionBoard,
  bindings: {
    discussionId: '<',
    contentItemId: '<',
    afterNewThread: '<?',
    grading: '<?',

    isOpen: '<',
    isClosed: '<',
    gatingPolicies: '<',
    printView: '<?',
  },
  controller: [
    '$scope',
    '$ngRedux',
    '$uibModal',
    'Roles',
    'Settings',
    'DiscussionBoardActions',
    'DiscussionWritingActions',
    'DiscussionViewActions',
    'DiscussionBoardMessages',
    'DiscussionViewHeaderTranslationKeys',
    'DiscussionSortActions',
    function (
      $scope,
      $ngRedux,
      $uibModal,
      Roles,
      Settings,
      DiscussionBoardActions,
      DiscussionWritingActions,
      DiscussionViewActions,
      DiscussionBoardMessages,
      DiscussionViewHeaderTranslationKeys,
      DiscussionSortActions
    ) {
      this.keyMap = DiscussionViewHeaderTranslationKeys;

      const isTrialLearner = Roles.isUnderTrialAccess();
      const isInstructor = Settings.isFeatureEnabled('TeachCourseRight');

      this.canWritePosts = () =>
        !this.courseEnded &&
        !this.grading &&
        !isTrialLearner &&
        (this.isOpen || isInstructor) &&
        !this.printView;

      this.canCreateThreads = () =>
        !this.courseEnded &&
        !this.grading &&
        !isTrialLearner &&
        (this.isOpen || isInstructor) &&
        !this.printView;

      // evidently the instructor can reply while grading.
      this.canWriteReplies = () =>
        !this.courseEnded && !isTrialLearner && (this.isOpen || isInstructor) && !this.printView;

      this.$onInit = () => {
        const actionCreators = {
          setVisited: DiscussionBoardActions.makeVisitBoardActionCreator(this.discussionId),

          updateSettings: DiscussionBoardActions.makeUpdateSettingsActionCreator(this.discussionId),

          _closeDiscussion: DiscussionBoardActions.makeCloseDiscussionActionCreator(
            this.contentItemId,
            this.discussionId,
            this.gatingPolicies
          ),

          setInViewPost: DiscussionViewActions.makeViewPostActionCreator(this.discussionId),

          setInViewRepliedPost: DiscussionViewActions.makeViewRepliedToPostActionCreator(
            this.discussionId
          ),

          setInViewInappropriatePost: DiscussionViewActions.makeViewInappropriatePostActionCreator(
            this.discussionId
          ),

          _threadSave: DiscussionWritingActions.makeThreadSaveActionCreator(this.discussionId),

          threadDiscard: DiscussionWritingActions.makeWritingDiscardActionCreator({
            discussionId: this.discussionId,
          }),

          threadKeepWorking: DiscussionWritingActions.makeWritingKeepWorkingActionCreator({
            discussionId: this.discussionId,
          }),

          toThreadsView: DiscussionViewActions.makeRestoreDefaultActionCreator(this.discussionId),
        };

        const sortActionCreators = mapValues(discussionOrders, order =>
          DiscussionSortActions.makeSortActionCreator(this.discussionId, order)
        );

        $scope.$on('$destroy', $ngRedux.connect(null, sortActionCreators)((this.sortActions = {})));

        $ngRedux.connectToCtrl(createBoardSelector(this.discussionId), actionCreators)(this);

        const initialSettings = {
          autoMarkUnread: true,
          canMarkUnread: isInstructor,
          closeDiscussion: this.isClosed,
          canWriteReplies: () => this.canWriteReplies(),
          ...this.getCachedSettings(),
        };

        this.confirm = msg =>
          $uibModal.open({
            component: 'confirmModal',
            resolve: {
              message: ['$translate', $translate => $translate(msg)],
            },
          }).result;

        this.closeDiscussion = settings => {
          const msg = settings.closeDiscussion
            ? DiscussionBoardMessages.closeDiscussion
            : DiscussionBoardMessages.openDiscussion;
          this.confirm(msg).then(() => {
            this._closeDiscussion(settings);
          });
        };

        this.updateSettingsFns = {
          autoMarkUnread: this.updateCachedSettings,
          closeDiscussion: this.closeDiscussion,
        };

        this.updateSettings(initialSettings);
        this.updateViewForNotifications();
        if (!this.printView) this.setVisited(this.grading);
        this.showTitle = Settings.isFeatureEnabled('DiscussionTitle');
      };

      //TODO the following could be in the init/update action flow
      this.getCachedSettings = () => Settings.getUserContext('discussionBoardUserSettings');

      this.setCachedSettings = settings =>
        Settings.setUserContext('discussionBoardUserSettings', settings);

      this.updateCachedSettings = update => {
        this.setCachedSettings({
          ...this.getCachedSettings(),
          ...update,
        });
        this.updateSettings(update);
      };

      this.updateViewForNotifications = () => {
        if (!this.notification) {
          return;
        }
        if (this.notification.inappropriate) {
          this.setInViewInappropriatePost(this.notification.id, this.inViewThreadId);
        } else {
          this.setInViewRepliedPost(this.notification.id, this.inViewThreadId);
        }
      };

      this.threadSave = (...args) => {
        const revisit =
          this.lastVisitedError && this.lastVisitedError.type === 'UNAUTHORIZED_ERROR';

        this._threadSave(...args, revisit);

        if (isFunction(this.afterNewThread)) {
          this.afterNewThread();
        }
      };

      this.setInView = (post, info) => {
        this.setInViewPost(post, this.inViewThreadId, info);
      };
    },
  ],
};

let DiscussionBoard = 'DiscussionBoard: ng module not included';

export default angular
  .module('lo.discussion.discussionBoard', [
    modal,
    discussionBoardSettings.name,
    discussionBoardSearch.name,
    discussionBoardThreadsView.name,
    discussionBoardSingleThreadView.name,
    discussionReply.name,
    jumpBar.name,
    actions.name,
  ])
  .constant('DiscussionViewHeaderTranslationKeys', {
    unread: 'DISCUSSION_VIEW_UNREAD_POSTS',
    new: 'DISCUSSION_VIEW_NEW_POSTS',
    bookmarked: 'DISCUSSION_VIEW_BOOKMARKED_POSTS',
    unresponded: 'DISCUSSION_VIEW_UNRESPONDED_POSTS',
    'user-posts': 'DISCUSSION_VIEW_USER_POSTS',
    search: 'DISCUSSION_VIEW_SEARCH_POSTS',
    'reported-inappropriate-posts': 'DISCUSSION_VIEW_REPORTED_INAPPROPRIATE_POSTS',
  })
  .constant('DiscussionBoardMessages', {
    closeDiscussion: 'CONFIRM_CLOSE_DISCUSSION',
    openDiscussion: 'CONFIRM_OPEN_DISCUSSION',
  })
  .component('discussionBoard', component)
  .run([
    '$injector',
    function ($injector) {
      DiscussionBoard = angular2react('discussionBoard', component, $injector);
    },
  ]);

export { DiscussionBoard };
