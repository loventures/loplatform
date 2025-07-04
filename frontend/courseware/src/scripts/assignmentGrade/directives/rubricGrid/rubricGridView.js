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

import { reduce, sum, map } from 'lodash';

import template from './rubricGridView.html';

export default angular
  .module('lo.assignmentGrade.directives.rubricGridView', [])
  .component('rubricGridView', {
    template,

    bindings: {
      rubric: '<',
    },

    controller: function () {
      this.$onInit = () => {
        this.maxColumns = reduce(
          this.rubric.sections,
          (max, section) => {
            const columns = 1 + section.levels.length + (section.isSelectionManual ? 1 : 0);
            return max < columns ? columns : max;
          },
          0
        );

        this.totalPointsAwarded = sum(map(this.rubric.sections, 'selectedPoints'));
        this.totalPointsPossible = sum(map(this.rubric.sections, 'points'));
        this.numCriteria = { num: this.maxColumns - 1 };
      };
    },
  });
