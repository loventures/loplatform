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

import { MatrixQuery } from '../bootstrap/loConfig';
import { QnaQuestionDto, QnaSummary } from '../qna/qnaApi';
import { createAction } from 'typesafe-actions';

export const toggleQnaSideBar = createAction('TOGGLE_QNA_SIDEBAR')<
  | {
      open?: boolean;
    }
  | undefined
>();

export const updateQnaQuery = createAction('UPDATE_QNA_QUERY')<Partial<MatrixQuery>>();

export const resetQnaQuery = createAction('RESET_QNA_QUERY')<void>();

export const setQnaSummaries = createAction('SET_QNA_SUMMARIES')<QnaSummary[]>();

export const setQnaQuestions = createAction('SET_QNA_QUESTIONS')<Array<QnaQuestionDto>>();

export const updateQnaQuestion = createAction('UPDATE_QNA_QUESTION')<QnaQuestionDto>();

export const addQnaQuestion = createAction('ADD_QNA_QUESTION')<QnaQuestionDto>();

export const replaceQnaQuestion = createAction('REPLACE_QNA_QUESTION')<QnaQuestionDto>();
