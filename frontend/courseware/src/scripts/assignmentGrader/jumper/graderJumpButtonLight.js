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

import template from './graderJumpButtonLight.html';

import { isGradableAssignment, isAssessment } from '../../utilities/contentTypes.js';

import { getAssignmentType } from '../getAssignmentType.js';
import { angular2react } from 'angular2react';
import { getAttemptOverviews } from '../../api/attemptOverviewApi.js';

import { gotoLink } from '../../utilities/routingUtils.js';
import {
  InstructorGraderPageLink,
  InstructorAssignmentOverviewPageLink,
} from '../../utils/pageLinks.js';
import errorService from '../../utilities/errorService.jsx';

export default angular.module('lo.assignmentGrader.graderJumpButtonLight', [errorService.name]);

const component = {
  bindings: {
    contentId: '<?',
    userId: '<?',
    fromGradebook: '<?',
  },
  template,
  controller: [
    'CourseContentOverviewAPI',
    'errorService',
    'Roles',
    '$ngRedux',
    '$q',
    function (CourseContentOverviewAPI, errorService, Roles, $ngRedux, $q) {
      this.loading = true;

      this.setup = () => {
        this.questionId = $ngRedux.getState().api.contentItems[this.contentId];
        this.showJumperButton = isGradableAssignment(this.content);
        this.assignmentType = getAssignmentType(this.content);
        this.loading = false;
      };

      this.checkIsUserGradable = () => {
        if (this.userId && isAssessment(this.content)) {
          return getAttemptOverviews([this.content.id], this.userId).then(overview => {
            return overview[0] && overview[0].allAttempts > 0;
          });
        } else {
          return $q.when(true);
        }
      };

      this.gotoGrader = () => {
        this.checkIsUserGradable().then(isUserGradable => {
          if (isUserGradable) {
            this.navToGrader();
          } else {
            errorService.generic('StudentHasNoSubmission', 'CannotGradeTillSubmit', [], {
              hideSecondaryButton: true,
            });
          }
        });
      };

      this.navToGrader = () => {
        if (this.userId) {
          gotoLink(
            InstructorGraderPageLink.toLink({
              contentId: this.contentId,
              forLearnerId: this.userId,
            })
          );
        } else {
          gotoLink(
            InstructorAssignmentOverviewPageLink.toLink({
              contentId: this.contentId,
            })
          );
        }
      };
    },
  ],
};

export let GraderJumpButton = 'GraderJumpButton: ng module not included';
angular
  .module('lo.assignmentGrader.graderJumpButtonLight')
  .component('graderJumpButtonLight', component)
  .run([
    '$injector',
    function ($injector) {
      GraderJumpButton = angular2react('graderJumpButtonLightForReact', component, $injector);
    },
  ]);
