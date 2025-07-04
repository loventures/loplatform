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

import { find, map, findIndex } from 'lodash';

import Rubric from '../../models/Rubric.js';
import rubricGridView from './rubricGridView.js';

import { angular2react } from 'angular2react';

const toLegacyRubricResponse = (rubric, rubricResponse, rubricFeedback) => {
  rubricResponse = rubricResponse || {};
  return map(rubric.sections, section => {
    const response = rubricResponse[section.name];
    const levelIndex = response && findIndex(section.levels, { points: response.pointsAwarded });
    const sectionFeedback = find(rubricFeedback, { sectionName: section.name });
    return {
      levelIndex,
      feedback: (sectionFeedback && sectionFeedback.comment) || '',
      levelGrade: response && response.pointsAwarded,
      manual: response && levelIndex === -1,
    };
  });
};

const component = {
  template: '<rubric-grid-view rubric="$ctrl.rubricInstance"></rubric-grid-view>',

  bindings: {
    rubric: '<',
    rubricResponse: '<',
    rubricFeedback: '<',
  },

  controller: [
    'Rubric',
    function (Rubric) {
      this.$onChanges = () => {
        const legacyRubricResponse = toLegacyRubricResponse(
          this.rubric,
          this.rubricResponse,
          this.rubricFeedback
        );
        this.rubricInstance = new Rubric(this.rubric, legacyRubricResponse);
      };
    },
  ],
};

let RubricGrid = 'RubricGrid: ng module not included';

export default angular
  .module('lo.assignmentGrade.directives.rubricGrid', [Rubric.name, rubricGridView.name])
  .component('rubricGrid', component)
  .run([
    '$injector',
    function ($injector) {
      RubricGrid = angular2react('rubricGrid', component, $injector);
    },
  ]);

export { RubricGrid };
