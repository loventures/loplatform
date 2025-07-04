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

import { angular2react } from 'angular2react';
import { groupBy } from 'lodash';

import template from './competencyMasterySummary.html';

// This is the Diagnostic proficiency summary
const component = {
  template,
  bindings: {
    competencies: '<',
    quiz: '<',
  },
  linkedAttemptIsComplete: false,
  controller: function () {
    this.$onInit = () => {
      this.linkedAttemptIsComplete = this.quiz.isLatestSubmittedAttemptFinalized;
      const competencyPerformance = this.quiz.latestAttemptCompetencyBreakdown;
      this.competencySplit = groupBy(this.competencies, prof =>
        competencyPerformance?.has(prof.id) ? 'mastered' : 'remaining'
      );
    };
  },
};

export let CompetencyMasterySummary = 'CompetencyMasterySummary: ng module not included';
export default angular
  .module('cbl.content.directives.competencyMasterySummary', [])
  .component('competencyMasterySummary', component)
  .run([
    '$injector',
    function ($injector) {
      CompetencyMasterySummary = angular2react('competencyMasterySummary', component, $injector);
    },
  ]);
