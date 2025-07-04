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

import { createSelector } from 'reselect';

import { filter, isEmpty } from 'lodash';

import { quizAttemptSaveOrSubmitStateSelectorCreator } from '../../selectors/quizSelectors.js';

import {
  quizSettingsSelectorCreator,
  quizAttemptQuestionTuplesSelectorCreator,
  quizAttemptUnsavedChangesSelectorCreator,
  quizTimestampsSelectorCreator,
} from './quizPlayerSelectors.js';

export const singlePagePlayerUnansweredQuestionsSelector = attemptId => {
  const unsavedChangesSelector = quizAttemptUnsavedChangesSelectorCreator(attemptId);
  const questionTuplesSelector = quizAttemptQuestionTuplesSelectorCreator(attemptId);
  return createSelector(
    [unsavedChangesSelector, questionTuplesSelector],
    ({ allUnsavedChanges }, { questionTuples }) => {
      const unansweredQuestions = filter(questionTuples, tuple => {
        return !tuple.state.answered && isEmpty(allUnsavedChanges[tuple.index]);
      });
      return {
        unansweredQuestions,
      };
    }
  );
};

export const singlePagePlayerSelectorCreator = (quiz, attemptId) => {
  const settingsSelector = quizSettingsSelectorCreator(quiz.contentId);

  const questionTuplesSelector = quizAttemptQuestionTuplesSelectorCreator(attemptId);
  const attemptSubmissionStateSelector = quizAttemptSaveOrSubmitStateSelectorCreator(attemptId);
  const unsavedChangesSelector = quizAttemptUnsavedChangesSelectorCreator(attemptId);
  const unansweredQuestionsSelector = singlePagePlayerUnansweredQuestionsSelector(attemptId);
  const timeStampsSelector = quizTimestampsSelectorCreator(attemptId);

  return createSelector(
    [
      questionTuplesSelector,
      unsavedChangesSelector,
      unansweredQuestionsSelector,
      timeStampsSelector,
      attemptSubmissionStateSelector,
      settingsSelector,
    ],
    (
      { questionTuples },
      unsavedChangesState,
      { unansweredQuestions },
      timeStamps,
      quizSubmissionState = {},
      settings = {}
    ) => {
      const canSaveOrSubmitQuiz = !quizSubmissionState.loading;

      return {
        ...unsavedChangesState,
        ...timeStamps,
        questionTuples,
        settings,
        quizSubmissionState,
        unansweredQuestions,
        canSaveOrSubmitQuiz,
      };
    }
  );
};
