/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { loadingActionCreatorMaker } from '../../utilities/loadingStateUtils.js';
import { quizAPI } from '../../services/quizAPI.ts';

import {
  quizLoadedActionCreator,
  quizQuestionsLoadedActionCreator,
} from './quizPlayerCallbackActions.js';

// Native (axios) QuizAPI: question rendering happens in Angular quiz-player
// components that read the store via $ngRedux.connectToCtrl and so need an Angular
// digest to re-render. A native load resolves outside the digest, but the
// store-level digest bridge in bootstrap/ngRedux.js ($ngRedux.subscribe ->
// $rootScope.$applyAsync) schedules one on every dispatch, so the questions still
// render. (Switching this to native without that bridge was the A2 regression; see
// docs/migration/A4-digest-audit-axios-flip.md.)
const getQuizQuestions = (contentId: any) =>
  quizAPI.loadQuestions(contentId).then((questionInfo: any) => ({ contentId, questionInfo }));

export const quizLoadedActionCreatorMaker = () => {
  return quizLoadedActionCreator;
};

export const loadQuizQuestionsActionCreator = (quiz: any) => {
  const loader = () => getQuizQuestions(quiz.contentId);

  const actionCreator = loadingActionCreatorMaker(
    { sliceName: 'quizQuestionsState', id: quiz.contentId },
    loader,
    [quizQuestionsLoadedActionCreator]
  );

  return actionCreator();
};

export const loadQuizQuestionsActionCreatorMaker = () => {
  return loadQuizQuestionsActionCreator;
};
