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

import dayjs from 'dayjs';
import { map, mapValues, pick } from 'lodash';
import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';
import { ResultReleaseTimes } from '../../utilities/assessmentSettings.js';
import { isScoreFinalized } from '../../utilities/attemptStates.js';
import {
  loadingActionCreatorMaker,
  loadingResetActionCreatorMaker,
  loadingSuccessActionCreatorMaker,
} from '../../utilities/loadingStateUtils.js';

import { quizAttemptsByUserLoadedActionCreatorMaker } from './quizPlayerCallbackActions.js';
import { $q, lojector } from '../../loject.js';

export const enterQuizPlayerActionCreatorMaker = attemptId => () => ({
  type: 'QUIZ_PLAYER_ENTER',
  sliceName: 'quizPlayerState',
  id: attemptId,
});

export const changeQuestionAnswerActionCreatorMaker = attemptId => {
  return (questionIndex, response) => ({
    type: 'QUESTION_CHANGE_ANSWER',
    sliceName: 'quizPlayerState',
    id: attemptId,
    questionIndex,
    data: {
      response,
    },
  });
};

export const gotoQuestionActionCreatorMaker = attemptId => {
  return (index, resetAnswerIndex = -1) => ({
    type: 'QUIZ_PLAYER_GOTO_QUESTION',
    sliceName: 'quizPlayerState',
    id: attemptId,
    data: {
      index,
      resetAnswerIndex,
    },
  });
};

export const submitQuestionActionCreatorMaker = (attemptId, quiz) => {
  const gotoQuestionActionCreator = gotoQuestionActionCreatorMaker(attemptId);
  return (responseIndex, { selection, attachments, uploads }, indexToGoAfter = -1) => {
    const quizAPI = lojector.get('QuizAPI');
    const User = lojector.get('User');

    // TODO: 'shouldSubmit' is being inferred here by the configuration of the assessment
    //   Really, this should be an option given to the student by the interface.
    //   Right now, there is no way for the student to mark their confidence without submitting their answer
    const shouldSubmitEveryQuestion =
      quiz.settings.resultsPolicy.resultReleaseTime === ResultReleaseTimes.OnResponseScore;

    const loader = () => {
      const start = dayjs();
      return quizAPI
        .submitQuestions(
          attemptId,
          {
            [responseIndex]: {
              selection,
              attachments: map(attachments, a => a.id),
              uploads: map(uploads, u => pick(u, ['guid', 'sizes'])),
            },
          },
          shouldSubmitEveryQuestion,
          false,
          false
        )
        .then(
          attempt => {
            if (attempt.remainingMillis)
              attempt.deadline = start.add(attempt.remainingMillis, 'ms');
            return { [attemptId]: attempt };
          },
          error => $q.reject(error || 'QUIZ_PLAYER_QUESTION_SAVE_ERROR')
        );
    };

    const successActions = [
      quizAttemptsByUserLoadedActionCreatorMaker(User.id),
      {
        type: 'QUIZ_QUESTION_SAVED',
        sliceName: 'quizPlayerState',
        id: attemptId,
        questionIndex: responseIndex,
      },
      attemptResp => {
        // TODO: is it okay that the backend "Finalizes" an assessment after submitting the last question?
        if (isScoreFinalized(attemptResp[attemptId])) {
          return [
            loadingSuccessActionCreatorMaker({
              sliceName: 'quizAttemptSubmissionState',
              id: attemptId,
            })(),
            createDataListUpdateMergeAction('quizAttemptsByUser', {
              [User.id]: {
                [attemptResp.id]: attemptResp,
              },
            }),
          ];
        } else {
          return [];
        }
      },
    ];
    if (indexToGoAfter > -1) {
      successActions.push(gotoQuestionActionCreator(indexToGoAfter));
    } else {
      successActions.push(gotoQuestionActionCreator(responseIndex));
    }

    const actionCreator = loadingActionCreatorMaker(
      { sliceName: 'quizQuestionSubmissionState', id: attemptId },
      loader,
      successActions
    );

    return actionCreator();
  };
};

export const skipQuestionActionCreatorMaker = (attemptId, quiz) => {
  const submitQuestionActionCreator = submitQuestionActionCreatorMaker(attemptId, quiz);
  return (index, response, indexToGoAfterSkip) => {
    return submitQuestionActionCreator(index, response, indexToGoAfterSkip);
  };
};

const _saveOrSubmitQuizActionCreatorMaker = (
  attemptId,
  saveOrSubmitFromQuizAPI,
  loadingActionConfig,
  errorMessage
) => {
  const User = lojector.get('User');
  const gotoQuestionActionCreator = gotoQuestionActionCreatorMaker(attemptId);
  return (responses, currentQuestionId = -1, autoSubmit = false) => {
    const loader = () => {
      const start = dayjs();
      return saveOrSubmitFromQuizAPI(
        attemptId,
        mapValues(responses, ({ selection, attachments, uploads }) => ({
          selection,
          attachments: map(attachments, a => a.id),
          uploads: map(uploads, u => pick(u, ['guid', 'sizes'])),
        })),
        autoSubmit
      ).then(
        attempt => {
          if (attempt.remainingMillis) attempt.deadline = start.add(attempt.remainingMillis, 'ms');
          return { [attemptId]: attempt };
        },
        error => $q.reject(error || errorMessage)
      );
    };

    const gotoQuestionAction =
      currentQuestionId > -1
        ? gotoQuestionActionCreator(currentQuestionId)
        : { type: 'GOTO_QUESTION_SKIPPED' };

    const actionCreator = loadingActionCreatorMaker(
      loadingActionConfig,
      loader,
      [
        quizAttemptsByUserLoadedActionCreatorMaker(User.id),
        gotoQuestionAction,
        {
          type: 'QUIZ_ALL_QUESTIONS_SAVED',
          sliceName: 'quizPlayerState',
          id: attemptId,
        },
      ],
      null,
      [
        {
          type: 'QUIZ_SAVE_FAILED',
          sliceName: 'quizPlayerState',
          id: attemptId,
        },
      ]
    );

    return actionCreator();
  };
};

export const saveQuizActionCreatorMaker = attemptId => {
  const QuizAPI = lojector.get('QuizAPI');
  return _saveOrSubmitQuizActionCreatorMaker(
    attemptId,
    QuizAPI.saveAttempt,
    {
      sliceName: 'quizAttemptSaveState',
      id: attemptId,
    },
    'QUIZ_PLAYER_ATTEMPT_SAVE_ERROR'
  );
};

export const autosaveQuizActionCreatorMaker = attemptId => {
  const QuizAPI = lojector.get('QuizAPI');
  return _saveOrSubmitQuizActionCreatorMaker(
    attemptId,
    QuizAPI.saveAttempt,
    {
      sliceName: 'quizAttemptAutoSaveState',
      id: attemptId,
    },
    'QUIZ_PLAYER_ATTEMPT_AUTOSAVE_ERROR'
  );
};

export const submitQuizActionCreatorMaker = attemptId => {
  const QuizAPI = lojector.get('QuizAPI');
  return _saveOrSubmitQuizActionCreatorMaker(
    attemptId,
    QuizAPI.submitAttempt,
    {
      sliceName: 'quizAttemptSubmissionState',
      id: attemptId,
    },
    'QUIZ_PLAYER_ATTEMPT_SUBMIT_ERROR'
  );
};

export const resetSaveStatusActionCreatorMaker = attemptId =>
  loadingResetActionCreatorMaker({
    sliceName: 'quizAttemptSaveState',
    id: attemptId,
  });

export const resetSubmitStatusActionCreatorMaker = attemptId =>
  loadingResetActionCreatorMaker({
    sliceName: 'quizAttemptSubmissionState',
    id: attemptId,
  });
