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

import {
  quizResultsSelectorCreator,
  quizViewQuestionsSelectorCreator,
} from '../../quizPlayerModule/selectors/quizResultsSelectors.js';
import questionLoader from '../../quizQuestions/questionLoader.jsx';

import template from './quizResults.html';

export default angular
  .module('lo.quiz.quizResults', [questionLoader.name])
  .controller('QuizResultsCtrl', [
    '$ngRedux',
    '$element',
    'Scroller',
    function ($ngRedux, $element, Scroller) {
      this.scrollTo = $element.find('#quiz-results-questions-anchor');

      this.$onInit = () => {
        this.pageSize = 16384;

        const selector = this.attemptId
          ? quizResultsSelectorCreator(this.assessment, this.attemptId)
          : quizViewQuestionsSelectorCreator(this.assessment);

        $ngRedux.connectToCtrl(selector, {})(this);

        this.gotoPage(1, true);
      };

      this.gotoPage = (pageNumber, ignoreScrollTo) => {
        this.activePage = pageNumber;
        const start = (pageNumber - 1) * this.pageSize;
        this.activeQuestionTuples = this.questionTuples.slice(start, start + this.pageSize);
        if (!ignoreScrollTo) {
          Scroller.scrollTop(this.scrollTo);
        }
      };
    },
  ])
  .component('quizResults', {
    template,
    bindings: {
      assessment: '<',
      attemptId: '<',
      printView: '<?',
    },
    controller: 'QuizResultsCtrl',
  });
