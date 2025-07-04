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

import { getSearchParams } from '../../utils/linkUtils.js';
import { map } from 'lodash';
import { submissionAttemptViewComponent } from '../../contentPlayerComponents/activityViews/submission/views/SubmissionAttemptView.js';
import { submissionInstructionsViewComponent } from '../../contentPlayerComponents/activityViews/submission/views/SubmissionInstructionsView.js';
import SubmissionActivityAPI from '../../services/SubmissionActivityAPI.js';

import assessmentSubmissionSection from '../assessmentGrader/assessmentSubmissionSection.jsx';
import authenticAssessmentSubmissionSection from '../authenticAssessmentGrader/authenticAssessmentSubmissionSection.jsx';
import SubmissionGrader from '../graders/SubmissionGrader.js';
import gradingPanel from '../gradingPanel/index.js';
import template from './submissionGrader.html';

export default angular
  .module('lo.assignmentGrader.submissionGrader', [
    SubmissionGrader.name,
    gradingPanel.name,
    assessmentSubmissionSection.name,
    authenticAssessmentSubmissionSection.name,
    submissionInstructionsViewComponent.name,
    submissionAttemptViewComponent.name,
    SubmissionActivityAPI.name,
  ])
  .component('submissionGrader', {
    template: template,
    bindings: {
      assignment: '=?',
      assignmentName: '=?',
      dueDate: '=?',
      onChange: '&',
      onExit: '&',
    },
    controller: [
      '$scope',
      'SubmissionGrader',
      'NavBlockerService',
      'SubmissionActivityAPI',
      function ($scope, SubmissionGrader, NavBlockerService, SubmissionActivityAPI) {
        this.$onInit = () => {
          this.submissionActivity = {
            assessment: this.assignment,
          };

          this.activityState = null;

          this.grader = new SubmissionGrader(this.assignment);

          this.grader
            .changeUser(getSearchParams().forLearnerId, getSearchParams().attemptId)
            .catch(error => {
              console.log('Failed to change user on grader', error);
              // this.onExit();
            });
          this.grader.blockNavForUnsavedChanges();
        };

        $scope.$watch(
          () => this.grader.activeUser,
          user => user && this.onChange({ user })
        );

        $scope.$watch(
          () => this.grader.activeAttempt,
          activeAttempt => {
            if (activeAttempt) {
              this.activityState = {
                activeAttempt: {
                  ...activeAttempt,
                  attachments: map(activeAttempt.attachments, attachmentId => {
                    const info = activeAttempt.attachmentInfos[attachmentId];
                    return {
                      ...info,
                      viewUrl: SubmissionActivityAPI.createAttachmentUrl(
                        activeAttempt.id,
                        attachmentId,
                        false
                      ),
                      downloadUrl: SubmissionActivityAPI.createAttachmentUrl(
                        activeAttempt.id,
                        attachmentId,
                        true
                      ),
                    };
                  }),
                },
              };
            }
          }
        );

        this.$onDestroy = () => {
          this.grader.removeNavBlocker();
        };
      },
    ],
  });
