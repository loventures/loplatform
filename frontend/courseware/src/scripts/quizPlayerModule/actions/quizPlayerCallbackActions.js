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

import { mapValues } from 'lodash';

import { createDataListUpdateMergeAction } from '../../utilities/apiDataActions.js';

export const quizLoadedActionCreator = quiz =>
  createDataListUpdateMergeAction('quizzes', {
    [quiz.contentId]: quiz,
  });

//updates changes in quiz to questions relationship to quizQuestions slice
export const quizQuestionsLoadedActionCreator = quiz =>
  createDataListUpdateMergeAction('quizQuestions', {
    [quiz.contentId]: quiz,
  });

//changes in quiz to attempts relationship to quizzes slice
export const quizAttemptsLoadedActionCreatorMaker = quizId => attempts =>
  createDataListUpdateMergeAction('quizzes', {
    [quizId]: { attempts: mapValues(attempts, 'id') },
  });

//changes on user attempts listing (primary attempts listing) to quizAttemptsByUser slice
export const quizAttemptsByUserLoadedActionCreatorMaker = userId => attempts =>
  createDataListUpdateMergeAction('quizAttemptsByUser', {
    [userId]: attempts,
  });
