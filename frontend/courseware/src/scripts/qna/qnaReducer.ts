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

import { MatrixFilter, MatrixQuery } from '../bootstrap/loConfig';
import { DEFAULT_PAGE_SIZE } from '../components/PaginateWithMax';
import {
  addQnaQuestion,
  replaceQnaQuestion,
  resetQnaQuery,
  setQnaQuestions,
  setQnaSummaries,
  toggleQnaSideBar,
  updateQnaQuery,
  updateQnaQuestion,
} from '../qna/qnaActions';
import { QnaState } from '../qna/qnaState';
import { keyBy } from 'lodash';
import { AnyAction, Reducer } from 'redux';
import { isActionOf } from 'typesafe-actions';

export const QnaActiveFilter: MatrixFilter[] = [
  { property: 'open', operator: 'eq', value: true },
  { property: 'sent', value: false },
];
export const QnaClosedFilter: MatrixFilter[] = [
  { property: 'closed', operator: 'eq', value: true },
  { property: 'sent', value: false },
];
export const QnaAnsweredFilter: MatrixFilter[] = [
  { property: 'open', operator: 'eq', value: false },
  { property: 'closed', operator: 'eq', value: false },
  { property: 'sent', value: false },
];
export const QnaSentFilter: MatrixFilter[] = [{ property: 'sent', value: true }];

const initialQnaQuery: MatrixQuery = {
  offset: 0,
  limit: DEFAULT_PAGE_SIZE,
  order: { property: 'created', direction: 'asc' },
  prefilter: QnaActiveFilter,
};

const initialState: QnaState = {
  open: false,
  questions: [],
  summaries: {},
  query: initialQnaQuery,
  instructorMessage: undefined,
};

const qnaReducer: Reducer<QnaState, AnyAction> = (state = initialState, action) => {
  if (isActionOf(toggleQnaSideBar, action)) {
    return {
      ...state,
      open: action.payload?.open ?? !state.open,
    };
  } else if (isActionOf(setQnaQuestions, action)) {
    const questions = action.payload;
    return {
      ...state,
      questions,
    };
  } else if (isActionOf(addQnaQuestion, action)) {
    return {
      ...state,
      questions: [...state.questions, action.payload],
    };
  } else if (isActionOf(replaceQnaQuestion, action)) {
    const index = state.questions.findIndex(q => q.id === action.payload.id);
    return {
      ...state,
      questions: [
        ...state.questions.slice(0, index),
        action.payload,
        ...state.questions.slice(1 + index),
      ],
    };
  } else if (isActionOf(updateQnaQuestion, action)) {
    const index = state.questions.findIndex(q => q.id === action.payload.id);
    const existing = state.questions[index];
    const updatedQuestion = {
      ...action.payload,
      messages: existing.messages.concat(action.payload.messages),
    };
    return {
      ...state,
      questions: [
        ...state.questions.slice(0, index),
        updatedQuestion,
        ...state.questions.slice(1 + index),
      ],
    };
  } else if (isActionOf(updateQnaQuery, action)) {
    return {
      ...state,
      query: {
        ...state.query,
        ...action.payload,
      },
    };
  } else if (isActionOf(resetQnaQuery, action)) {
    return {
      ...state,
      query: initialQnaQuery,
    };
  } else if (isActionOf(setQnaSummaries, action)) {
    const summaries = keyBy(action.payload, 'edgePath');
    return {
      ...state,
      summaries,
    };
  } else {
    return state;
  }
};

export default qnaReducer;
