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

import jumperTemplate from './jumper.html';

import { createJumperSelector } from '../selectors.js';

import uibModal from 'angular-ui-bootstrap/src/modal';

import studentPickerModal from '../modals/discussionStudentPickerModal.js';

import actions from '../actions/index.js';

export default angular
  .module('lo.discussion.discussionBoardJumper', [uibModal, studentPickerModal.name, actions.name])
  .constant('DiscussionJumperNames', {
    new: 'DISCUSSION_NEW_POSTS',
    bookmarked: 'DISCUSSION_BOOKMARKED_POSTS',
    unread: 'DISCUSSION_UNREAD_POSTS',
    unresponded: 'DISCUSSION_UNRESPONDED_POSTS',
    'user-posts': 'DISCUSSION_MINE_POSTS',
  })
  .constant('DiscussionJumperCategories', {
    instructor: ['user-posts', 'unread', 'bookmarked', 'unresponded'],
    student: ['user-posts', 'new', 'bookmarked'],
  })
  .component('discussionBoardJumper', {
    template: jumperTemplate,
    bindings: {
      discussionId: '<',
      viewType: '<',
      setInView: '<',
    },
    controller: [
      '$scope',
      '$ngRedux',
      'DiscussionJumperActions',
      'DiscussionJumperNames',
      '$uibModal',
      function ($scope, $ngRedux, DiscussionJumperActions, DiscussionJumperNames, $uibModal) {
        const loadLimit = 5;
        const fetchTriggerSize = 2;

        this.$onInit = () => {
          this.jumperName = DiscussionJumperNames[this.viewType];

          const mapStateToThis = createJumperSelector(this.discussionId, this.viewType);

          const actions = {
            loadPosts: DiscussionJumperActions.makeLoadActionCreator(
              this.discussionId,
              this.viewType
            ),
            setJumperActive: DiscussionJumperActions.makeViewJumperActionCreator(
              this.discussionId,
              this.viewType,
              this.setInView
            ),
            setCurrentPost: DiscussionJumperActions.makeViewPostActionCreator(
              this.discussionId,
              this.viewType,
              this.setInView
            ),
          };

          if (this.viewType === 'user-posts') {
            actions.changeUser = DiscussionJumperActions.makeSetUserActionCreator(
              this.discussionId,
              this.viewType
            );

            this.pickUser = () => {
              $uibModal
                .open({
                  component: 'discussion-student-picker-modal',
                  resolve: {
                    discussionId: () => this.discussionId,
                  },
                })
                .result.then(user => {
                  if (user && user.handle !== this.userHandle) {
                    this.changeUser(user);
                  }
                });
            };
          }

          $ngRedux.connectToCtrl(mapStateToThis, actions)(this);

          if (this.viewType === 'user-posts') {
            //Should get fired off when (and only when) the user changes
            $scope.$watch(
              () => !this.loading && !this.loadedOnce,
              shouldLoad => {
                if (shouldLoad) {
                  this.loadMore();
                }
              }
            );

            $scope.$watch(
              () => this.userName,
              () => {
                this.jumperName = this.isSelf
                  ? DiscussionJumperNames[this.viewType]
                  : this.userName;
              }
            );
          }
        };

        this.changeCurrentPost = post => {
          this.setCurrentPost(post);

          //this is going to be the state before the actual change
          //but that is fine. as long as this happens periodically.
          if (
            !this.loading &&
            this.loadedCount - this.currentPostIndex < fetchTriggerSize &&
            this.loadedCount < this.totalCount
          ) {
            //incrementally load if the user is nearing the end
            this.loadMore();
          }
        };

        this.loadMore = () =>
          this.loadPosts(loadLimit, this.loadedCount, {
            userHandle: this.userHandle,
            lastVisitedTime: this.lastVisitedTime,
          });
      },
    ],
  });
