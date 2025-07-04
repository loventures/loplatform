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

import gradeFeedback from '../../assignmentGrade/directives/gradeFeedbackPanel.js';
import rubricPanel from '../../assignmentGrade/directives/rubricGradePanel.js';
import { graderStatusFlagToggleAction } from './graderActions.js';
import panelTmpl from './gradingPanel.html';
import panelControls from './gradingPanelControls.js';
import panelSection from './gradingPanelSection.js';
import studentPicker from './studentPicker.js';
import submissionScore from './submissionScore.js';

export default angular
  .module('lo.assignmentGrader.gradingPanel', [
    panelControls.name,
    panelSection.name,
    studentPicker.name,
    submissionScore.name,
    rubricPanel.name,
    gradeFeedback.name,
  ])
  .component('gradingPanel', {
    template: panelTmpl,
    bindings: {
      grader: '<',
      onExit: '&',
    },
    transclude: {
      fixedTop: 'fixedTop',
      scrollableSections: 'scrollableSections',
      fixedBottom: 'fixedBottom',
    },
    controller: [
      '$ngRedux',
      'Settings',
      function ($ngRedux, Settings) {
        this.$onInit = () => $ngRedux.dispatch(graderStatusFlagToggleAction(true));
        this.$onDestroy = () => $ngRedux.dispatch(graderStatusFlagToggleAction(false));

        this.togglePanel = () => $ngRedux.dispatch(graderStatusFlagToggleAction());
        this.panelCollapse = () => !$ngRedux.getState().ui.graderOpenState.status;

        this.isInstructor = Settings.isFeatureEnabled('TeachCourseRight');
      },
    ],
  });
