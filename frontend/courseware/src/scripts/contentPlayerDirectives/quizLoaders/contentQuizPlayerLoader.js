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

import template from './contentQuizPlayerLoader.html';

import multiPagePlayer from '../../quizViews/players/multiPagePlayer.js';
import singlePagePlayer from '../../quizViews/players/singlePagePlayer.js';

import { quizLoadedActionCreatorMaker } from '../../quizPlayerModule/actions/quizPlayerLoadActions.js';

import {
  enterQuizPlayerActionCreatorMaker,
  resetSaveStatusActionCreatorMaker,
  resetSubmitStatusActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerActions.js';
import { contentQuizLoaderSelectorCreator } from './contentQuizLoaderSelector.js';

import { angular2react } from 'angular2react';

const component = {
  template,
  bindings: {
    assessment: '<',
    attempt: '<',
    onAttempt: '<',
    onSave: '<',
    onSubmit: '<',
    printView: '<?',
  },
  controller: [
    '$q',
    '$ngRedux',
    '$timeout',
    function ($q, $ngRedux, $timeout) {
      this.$onInit = () => {
        $ngRedux.connectToCtrl(contentQuizLoaderSelectorCreator(this.attempt.id, this.assessment), {
          enterQuizPlayer: enterQuizPlayerActionCreatorMaker(this.attempt.id),
          setAssessmentDetails: quizLoadedActionCreatorMaker(),
          resetSaveStatus: resetSaveStatusActionCreatorMaker(this.attempt.id),
          resetSubmitStatus: resetSubmitStatusActionCreatorMaker(this.attempt.id),
        })(this);

        //we know this exists from the node. just give it to redux.
        //TODO: remove after new content player is in prod
        this.setAssessmentDetails(this.assessment);

        // enter quiz player simply initializes some redux state,
        this.enterQuizPlayer();

        //TODO: simplify in https://gojira.difference-engine.com/browse/TECH-1335
        this.submitted = () => {
          this.resetSubmitStatus();
          $timeout(() => this.onSubmit());
        };

        this.saved = () => {
          this.resetSaveStatus();
          $timeout(() => this.onSave());
        };

        this.singlePage =
          this.assessment.settings.pagingPolicy === 'ASSESSMENT_AT_A_TIME' ||
          (this.printView &&
            this.assessment.settings.navigationPolicy?.backtrackingAllowed &&
            this.assessment.settings.navigationPolicy?.skippingAllowed);
      };
    },
  ],
};

let ContentQuizPlayerLoader = 'ContentQuizPlayerLoader: ng module not included';

export default angular
  .module('cbl.content.directives.quizLoaders.playerLoader', [
    multiPagePlayer.name,
    singlePagePlayer.name,
  ])
  .component('contentQuizPlayerLoader', component)
  .run([
    '$injector',
    function ($injector) {
      ContentQuizPlayerLoader = angular2react('contentQuizPlayerLoader', component, $injector);
    },
  ]);

export { ContentQuizPlayerLoader };
