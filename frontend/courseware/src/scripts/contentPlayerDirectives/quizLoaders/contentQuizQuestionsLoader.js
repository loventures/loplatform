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

import template from './contentQuizQuestionsLoader.html';

import quizResults from '../../quizViews/players/quizResults.js';

import {
  loadQuizQuestionsActionCreatorMaker,
  quizLoadedActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerLoadActions.js';

import { contentQuizQuestionLoaderSelectorCreator } from './contentQuizLoaderSelector.js';

const component = {
  template,
  bindings: {
    assessment: '<',
    useProjectResults: '<',
    printView: '<?',
  },
  controller: [
    '$ngRedux',
    function ($ngRedux) {
      this.$onInit = () => {
        $ngRedux.connectToCtrl(contentQuizQuestionLoaderSelectorCreator(this.assessment), {
          setAssessmentDetails: quizLoadedActionCreatorMaker(this.assessment),
          loadAssessmentQuestions: loadQuizQuestionsActionCreatorMaker(this.assessment),
        })(this);

        //TODO: remove after new content player is in prod
        this.setAssessmentDetails(this.assessment);

        if (
          !this.quizAssessmentQuestionsState.loaded &&
          !this.quizAssessmentQuestionsState.loading
        ) {
          this.loadAssessmentQuestions(this.assessment);
        }
      };
    },
  ],
};

import { angular2react } from 'angular2react';
let ContentQuizQuestionsLoader = 'ContentQuizQuestionsLoader: ng module not included';

export default angular
  .module('cbl.content.directives.quizLoaders.questionsLoader', [quizResults.name])
  .component('contentQuizQuestionsLoader', component)
  .run([
    '$injector',
    function ($injector) {
      ContentQuizQuestionsLoader = angular2react(
        'contentQuizQuestionsLoader',
        component,
        $injector
      );
    },
  ]);

export { ContentQuizQuestionsLoader };
