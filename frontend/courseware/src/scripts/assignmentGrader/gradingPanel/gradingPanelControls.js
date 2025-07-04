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

import template from './gradingPanelControls.html';

import tooltip from 'angular-ui-bootstrap/src/tooltip';

export default angular
  .module('lo.assignmentGrader.gradingPanelControls', [tooltip])
  .component('gradingPanelControls', {
    template: template,
    bindings: {
      grader: '<',
      grade: '<',
      onExit: '&',
    },
    transclude: true,
    controller: function () {
      this.$onInit = () => {
        this.grader.calculateUnpostedCount();
      };

      this.canPostGrade = () =>
        (!this.isSubmitting() &&
          this.grade.isReleased() &&
          this.grade.isDirty() &&
          this.grade.isComplete()) ||
        (!this.grade.isReleased() && this.grade.isComplete());

      this.canSaveGrade = () => !this.isSubmitting() && this.grade.isDirty();

      this.isSubmitting = () => this.grade.isSubmitting;

      this.resetGrade = () => this.grade.resetGrade();

      this.saveDraft = () => this.grader.saveChanges(false);

      this.postGrade = () => this.grader.saveChanges(true);

      this.changeToInfo = info =>
        info && this.grader.confirmDiscardChanges().then(() => this.grader.changeByInfo(info));

      this.exit = () => this.onExit();
    },
  });
