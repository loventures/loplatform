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

import template from './assessmentGrader.html';

import QuizGrader from '../graders/QuizGrader.js';
import assignmentGrader from '../gradingPanel/assignmentGrader.js';

import gradingPanel from '../gradingPanel/index.js';

import assessmentSubmissionSection from './assessmentSubmissionSection.jsx';

import { getSearchParams } from '../../utils/linkUtils.js';

export default angular
  .module('lo.assignmentGrader.assessmentGrader', [
    assignmentGrader.name,
    QuizGrader.name,
    gradingPanel.name,
    assessmentSubmissionSection.name,
  ])
  .component('assessmentGrader', {
    template: template,
    bindings: {
      assignmentId: '=?',
      assignmentType: '=?',
      assignmentName: '=?',
      dueDate: '=?',
      onChange: '&',
      onExit: '&',
    },
    controller: [
      '$scope',
      'QuizGrader',
      function ($scope, QuizGrader) {
        this.$onInit = () => {
          //TODO not recreating it
          this.grader = new QuizGrader(this.assignmentId);

          this.grader
            .changeUser(getSearchParams().forLearnerId, getSearchParams().attemptId)
            .catch(() => {
              return this.onExit();
            });

          this.grader.blockNavForUnsavedChanges();
        };

        $scope.$watch(
          () => this.grader.activeUser,
          user => user && this.onChange({ user })
        );

        this.$onDestroy = () => {
          this.grader.removeNavBlocker();
        };
      },
    ],
  });
