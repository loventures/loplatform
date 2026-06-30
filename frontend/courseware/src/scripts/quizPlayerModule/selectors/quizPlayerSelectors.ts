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

import { map, filter, isEmpty, pickBy, mapValues } from 'lodash';

import { createSelector } from 'reselect';

import {
  quizSettingsSelectorCreator,
  attemptDetailsSelectorCreator,
  quizPlayerStateSelectorCreator,
} from '../../selectors/quizSelectors.js';

import { getQuestionStates } from '../utils/quizQuestionStates.js';

export {
  quizSettingsSelectorCreator,
  quizPlayerStateSelectorCreator,
  attemptDetailsSelectorCreator,
};

export const quizAttemptUnsavedChangesSelectorCreator = (attemptId: any) => {
  const stateSelector = quizPlayerStateSelectorCreator(attemptId);

  return createSelector([stateSelector], (playerState: any = {}) => {
    const allUnsavedChanges = pickBy(
      mapValues(playerState.questionAnsweringStates, (state: any) => {
        return isEmpty(state) ? null : state;
      }),
      (val: any) => !!val
    );

    const anyQuestionHasUnsavedChanges = !isEmpty(allUnsavedChanges);

    return {
      allUnsavedChanges,
      anyQuestionHasUnsavedChanges,
    };
  });
};

export const quizAttemptQuestionTuplesSelectorCreator = (attemptId: any) => {
  const attemptSelector = attemptDetailsSelectorCreator(attemptId);
  const stateSelector = quizPlayerStateSelectorCreator(attemptId);

  return createSelector([attemptSelector, stateSelector], (attempt: any, playerState: any = {}) => {
    // `questionAnsweringStates` is absent until `enterQuizPlayer` initializes the attempt's player state.
    // The React players run this selector on their first render (before that dispatch lands), so optional-
    // chain it — Angular's digest masked this; a synchronous React render does not.
    const questionTuples = map(attempt.questionTuples, (tuple: any, index: number) => {
      const answeringState = playerState.questionAnsweringStates?.[index];
      const hasUnsavedChanges = !isEmpty(answeringState);
      const savedResponse = tuple.response;
      const response = hasUnsavedChanges ? answeringState : savedResponse;
      return {
        ...tuple,
        ordinal: index + 1,
        savedResponse,
        response,
        state: getQuestionStates(tuple.question, savedResponse, response.score),
      };
    });

    return {
      questionTuples,
    };
  });
};

export const quizUnansweredQuestionsSelector = (attemptId: any) => {
  const questionTuplesSelector = quizAttemptQuestionTuplesSelectorCreator(attemptId);
  return createSelector([questionTuplesSelector], ({ questionTuples }: any) => {
    const unansweredQuestions = filter(questionTuples, (tuple: any) => !tuple.state.answered);
    return {
      unansweredQuestions,
    };
  });
};

export const quizTimestampsSelectorCreator = (attemptId: any) => {
  const stateSelector = quizPlayerStateSelectorCreator(attemptId);
  return createSelector(
    [stateSelector],
    ({ lastChangeTimestamp, lastSaveTimestamp, lastSaveFailed }: any = {}) => {
      return {
        lastChangeTimestamp,
        lastSaveTimestamp,
        lastSaveFailed,
      };
    }
  );
};
