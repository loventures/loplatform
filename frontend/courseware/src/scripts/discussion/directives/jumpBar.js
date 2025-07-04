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

import { includes } from 'lodash';
import jumpBar from './jumpBar.html';
import jumper from './jumper.js';
import User from '../../utilities/User.js';
import { createJumperSelector } from '../selectors.js';

export default angular
  .module('lo.discussion.discussionBoardJumpBar', [jumper.name, User.name])
  .component('discussionBoardJumpBar', {
    template: jumpBar,
    bindings: {
      discussionId: '<',
      setInView: '<',
      displayingView: '<',
      backAction: '<',
      lastVisitedTime: '<',
    },
    controller: [
      '$scope',
      '$ngRedux',
      'User',
      'DiscussionJumperActions',
      'DiscussionJumperCategories',
      function ($scope, $ngRedux, User, DiscussionJumperActions, DiscussionJumperCategories) {
        this.$onInit = () => {
          this.isInstructor = User.isStrictlyInstructor();

          this.userJumperTypes = this.isInstructor
            ? DiscussionJumperCategories.instructor
            : DiscussionJumperCategories.student;

          const actions = {
            loadInitSummary: DiscussionJumperActions.makeSummaryLoadActionCreator(
              this.discussionId,
              this.userJumperTypes,
              this.lastVisitedTime
            ),
          };
          // Grabbing the user-posts jumpbar because I want to get the initial selection.
          // This is why individual jumpers should not manage their own state.
          // Not worried about this right now because of proposed redesign.
          const mapStateToThis = createJumperSelector(this.discussionId, 'user-posts');

          $ngRedux.connectToCtrl(mapStateToThis, actions)(this);

          this.loadInitSummary(this.userHandle || User.getHandle());
        };

        this.showJumper = jumperType => {
          return includes(this.userJumperTypes, jumperType);
        };
      },
    ],
  });
