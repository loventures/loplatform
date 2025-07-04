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

import template from './rubricSectionEditable.html';

import { findIndex, max } from 'lodash';

export default angular
  .module('lo.assignmentGrade.rubric.section.editable', [])
  .component('rubricSectionEditable', {
    bindings: {
      section: '<',
      titleIcon: '<?',
      titleAction: '<?',
    },
    template,
    controller: [
      '$element',
      '$timeout',
      'Settings',
      function ($element, $timeout, Settings) {
        this.allowManualGrading = Settings.isFeatureEnabled('InstructorRubricManualGrading');

        this.rounded = function (number) {
          const temp = number * 100;
          const anotherTemp = Math.round(temp);
          const someTemp = anotherTemp / 100;
          return someTemp;
        };

        this.manualPoints = null;

        if (Settings.isFeatureEnabled('RubricGraderUseMaxCriterionValue')) {
          const maxLevelIndex = findIndex(this.section.levels, level => {
            return level.points === max(this.section.levels.map(l => l.points));
          });
          this.section.setSelection(maxLevelIndex);
        }

        this.$onChanges = ({ section }) => {
          if (section && section.currentValue) {
            if (section.currentValue.isSelectionManual) {
              this.manualPoints = section.currentValue.selectedPoints;
            } else {
              this.manualPoints = null;
            }
          }
        };

        this.setFeedbackStatus = function (status) {
          this.section.feedbackStatus = status;
          if (!status) {
            this.section.setFeedback('');
          }
        };
      },
    ],
  });
