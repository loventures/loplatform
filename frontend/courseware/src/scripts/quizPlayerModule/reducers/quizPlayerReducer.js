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

const quizPlayerReducer = (
  state = {
    currentQuestionIndex: -1,
    questionAnsweringStates: {},
    lastChangeTimestamp: dayjs().valueOf(),
    lastSaveTimestamp: dayjs().valueOf(),
    lastSaveFailed: false,
  },
  action
) => {
  switch (action.type) {
    case 'QUIZ_EXIT_PLAYER':
      return {
        ...state,
        shouldExitPlayer: true,
      };
    case 'QUIZ_EXITED_PLAYER':
      return {
        ...state,
        shouldExitPlayer: false,
      };
    case 'QUIZ_PLAYER_GOTO_QUESTION':
      if (action.resetAnswerIndex > -1) {
        state = {
          ...state,
          questionAnsweringStates: {
            ...state.questionAnsweringStates,
            [action.resetAnswerIndex]: null,
          },
        };
      }
      return {
        ...state,
        currentQuestionIndex: action.data.index,
      };

    case 'QUESTION_CHANGE_ANSWER':
      if (action.questionIndex > -1) {
        return {
          ...state,
          questionAnsweringStates: {
            ...state.questionAnsweringStates,
            [action.questionIndex]: action.data.response,
          },
          lastChangeTimestamp: dayjs().valueOf(),
        };
      } else {
        return state;
      }
    case 'QUIZ_QUESTION_SAVED':
      return {
        ...state,
        questionAnsweringStates: {
          ...state.questionAnsweringStates,
          [action.questionIndex]: null,
        },
        lastSaveTimestamp: dayjs().valueOf(),
        lastSaveFailed: false,
      };
    case 'QUIZ_ALL_QUESTIONS_SAVED':
      return {
        ...state,
        questionAnsweringStates: {},
        lastSaveTimestamp: dayjs().valueOf(),
        lastSaveFailed: false,
      };
    case 'QUIZ_SAVE_FAILED':
      return {
        ...state,
        lastSaveFailed: true,
      };
    default:
      return state;
  }
};

export default quizPlayerReducer;
