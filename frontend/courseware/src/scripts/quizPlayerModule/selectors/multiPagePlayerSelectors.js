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

import {
  getCanGoStatus,
  getIndexToGoAfter,
  getInitialQuestionIndex,
  getShouldDisplaySkip,
} from '../utils/quizNavigationChecker.js';

import {
  quizAttemptSaveOrSubmitStateSelectorCreator,
  quizQuestionSubmissionStateSelectorCreator,
} from '../../selectors/quizSelectors.js';

import {
  quizSettingsSelectorCreator,
  quizAttemptUnsavedChangesSelectorCreator,
  quizAttemptQuestionTuplesSelectorCreator,
  quizPlayerStateSelectorCreator,
  quizTimestampsSelectorCreator,
  quizUnansweredQuestionsSelector,
} from './quizPlayerSelectors.js';

import { ResultReleaseTimes } from '../../utilities/assessmentSettings.js';

export const multiPagePlayerStateSelector = (quizId, attemptId) => {
  const settingsSelector = quizSettingsSelectorCreator(quizId);
  const questionTuplesSelector = quizAttemptQuestionTuplesSelectorCreator(attemptId);
  const playerStateSelector = quizPlayerStateSelectorCreator(attemptId);

  return createSelector(
    [settingsSelector, questionTuplesSelector, playerStateSelector],
    (settings, { questionTuples }, playerState = {}) => {
      const lastQuestionIndex = questionTuples.length - 1;

      const currentQuestionIndex =
        playerState.currentQuestionIndex > -1
          ? playerState.currentQuestionIndex
          : getInitialQuestionIndex(questionTuples);

      const isLastQuestion = currentQuestionIndex === lastQuestionIndex;

      const canGoStatus = getCanGoStatus(currentQuestionIndex, questionTuples, settings);

      const indexToGoAfter = getIndexToGoAfter(currentQuestionIndex, lastQuestionIndex, settings);

      const indexToGoAfterSkip = getIndexToGoAfter(
        currentQuestionIndex,
        lastQuestionIndex,
        settings,
        true
      );

      const shouldDisplaySkip = getShouldDisplaySkip(settings);

      const enableAutosave =
        settings.resultsPolicy.resultReleaseTime !== ResultReleaseTimes.OnResponseScore;

      return {
        enableAutosave,
        currentQuestionIndex,
        lastQuestionIndex,
        isLastQuestion,
        indexToGoAfter,
        indexToGoAfterSkip,
        canGoStatus,
        shouldDisplaySkip,
      };
    }
  );
};

export const multiPagePlayerSelectorCreator = (quiz, attemptId) => {
  const settingsSelector = quizSettingsSelectorCreator(quiz.contentId);
  const stateSelector = multiPagePlayerStateSelector(quiz.contentId, attemptId);

  const questionTuplesSelector = quizAttemptQuestionTuplesSelectorCreator(attemptId);
  const questionSubmissionStateSelector = quizQuestionSubmissionStateSelectorCreator(attemptId);
  const attemptSubmissionStateSelector = quizAttemptSaveOrSubmitStateSelectorCreator(attemptId);
  const unsavedChangesSelector = quizAttemptUnsavedChangesSelectorCreator(attemptId);
  const unansweredQuestionsSelector = quizUnansweredQuestionsSelector(attemptId);
  const timeStampsSelector = quizTimestampsSelectorCreator(attemptId);

  return createSelector(
    [
      questionTuplesSelector,
      stateSelector,
      unsavedChangesSelector,
      unansweredQuestionsSelector,
      timeStampsSelector,
      questionSubmissionStateSelector,
      attemptSubmissionStateSelector,
      settingsSelector,
    ],
    (
      { questionTuples },
      playerState,
      unsavedChangesState,
      { unansweredQuestions },
      timeStamps,
      questionSubmissionState = {},
      quizSubmissionState = {},
      settings
    ) => {
      const currentQuestionTuple = questionTuples[playerState.currentQuestionIndex] || {};
      const currentQuestion = currentQuestionTuple.question;
      const currentQuestionResponse = currentQuestionTuple.response;
      const currentQuestionSavedResponse = currentQuestionTuple.savedResponse;
      const currentQuestionScore =
        currentQuestionTuple.savedResponse.state === 'ResponseScoreReleased';
      const currentQuestionHasUnsavedChanges =
        currentQuestionResponse !== currentQuestionSavedResponse;
      const currentQuestionAnswered = currentQuestionTuple.state.answered;

      const canSaveOrSubmitQuiz = !quizSubmissionState.loading;

      const canSubmitCurrentQuestion =
        !quizSubmissionState.loading &&
        !questionSubmissionState.loading &&
        currentQuestionHasUnsavedChanges;

      return {
        ...playerState,
        ...unsavedChangesState,
        ...timeStamps,
        questionTuples,
        settings,
        questionSubmissionState,
        quizSubmissionState,
        unansweredQuestions,
        canSaveOrSubmitQuiz,
        canSubmitCurrentQuestion,
        currentQuestion,
        currentQuestionResponse,
        currentQuestionSavedResponse,
        currentQuestionScore,
        currentQuestionHasUnsavedChanges,
        currentQuestionAnswered,
      };
    }
  );
};
