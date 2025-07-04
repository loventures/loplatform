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

import apiDataReducer from '../../utilities/apiDataReducer.ts';
import { createNamedReducer } from '../../utilities/reduxify.ts';

const contentActivityNamedReducer = createNamedReducer('activityByContentByUser', apiDataReducer);

type ContentActivityState = {
  attemptOverview?: {
    openAttempts: number;
    allAttempts: number;
  };
};

const contentActivityReducer = (state: ContentActivityState = {}, action) => {
  const attemptOverview = state.attemptOverview || {
    openAttempts: 0,
    allAttempts: 0,
  };
  switch (action.type) {
    case 'QUIZ_ACTIVITY_ATTEMPT_SUBMITTED':
      return {
        ...state,
        attemptOverview: {
          ...attemptOverview,
          openAttempts: attemptOverview.openAttempts - 1,
        },
      };
    case 'QUIZ_ACTIVITY_ATTEMPT_CREATED':
      return {
        ...state,
        attemptOverview: {
          ...attemptOverview,
          openAttempts: attemptOverview.openAttempts + 1,
          allAttempts: attemptOverview.allAttempts + 1,
        },
      };
    default:
      return state;
  }
};

type ContentActivityByUserState = Record<string, ContentActivityState>;

const contentActivityByContentByUserReducer = (state: ContentActivityByUserState = {}, action) => {
  switch (action.type) {
    case 'QUIZ_ACTIVITY_ATTEMPT_SUBMITTED':
    case 'QUIZ_ACTIVITY_ATTEMPT_CREATED':
      if (action.userId && action.id) {
        state[action.userId] = state[action.userId] ?? {};
        return {
          ...state,
          [action.userId]: {
            ...state[action.userId],
            [action.id]: contentActivityReducer(state[action.userId][action.id], action),
          },
        };
      } else {
        return state;
      }
    default:
      return contentActivityNamedReducer(state, action);
  }
};

export default contentActivityByContentByUserReducer;
