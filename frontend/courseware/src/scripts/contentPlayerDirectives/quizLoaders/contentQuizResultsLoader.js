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

import template from './contentQuizResultsLoader.html';

import quizResults from '../../quizViews/players/quizResults.js';

import { quizLoadedActionCreatorMaker } from '../../quizPlayerModule/actions/quizPlayerLoadActions.js';

import { contentQuizLoaderSelectorCreator } from './contentQuizLoaderSelector.js';

import { enterQuizPlayerActionCreatorMaker } from '../../quizPlayerModule/actions/quizPlayerActions.js';
import { angular2react } from 'angular2react';

const component = {
  template,
  bindings: {
    assessment: '<',
    attempt: '<',
    useProjectResults: '<',
    printView: '<?',
  },
  controller: [
    '$ngRedux',
    function ($ngRedux) {
      this.$onInit = () => {
        $ngRedux.connectToCtrl(contentQuizLoaderSelectorCreator(this.attempt.id, this.assessment), {
          enterQuiz: enterQuizPlayerActionCreatorMaker(this.attempt.id),
          setAssessmentDetails: quizLoadedActionCreatorMaker(this.assessment),
        })(this);

        this.enterQuiz();
      };
    },
  ],
};

let ContentQuizResultsLoader = 'ContentQuizResultsLoader: ng module not included';

export default angular
  .module('cbl.content.directives.quizLoaders.resultsLoader', [quizResults.name])
  .component('contentQuizResultsLoader', component)
  .run([
    '$injector',
    function ($injector) {
      ContentQuizResultsLoader = angular2react('contentQuizResultsLoader', component, $injector);
    },
  ]);

export { ContentQuizResultsLoader };
