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

import { makeQuizOverviewAPI } from './pure/quizOverviewAPI.ts';
import { request } from '../utilities/request.ts';

/**
 * Native (axios) QuizOverviewAPI for React/redux callers — replaces
 * `lojector.get('QuizOverviewAPI')`. Resolves via native Promises (no digest),
 * which is correct for the redux/thunk consumers that use it.
 */
export const quizOverviewAPI = makeQuizOverviewAPI((url, method, ...rest) =>
  request.promiseRequest(url, method, ...rest)
);
