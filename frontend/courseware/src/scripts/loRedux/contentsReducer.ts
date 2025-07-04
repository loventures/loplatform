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

import { Content, fetchContents } from '../api/contentsApi';
import { Loadable, errored, loaded, loading } from '../types/loadable';
import { Dispatch } from 'react';

import { createKeyedReducer } from './createKeyedReducer';

/**
 * A keyed reducer where the keys are the user ids, and the value is the list of
 * Contents for the current course, for how they appear to that user
 */
const [reducer, update, replace] = createKeyedReducer<Loadable<Content[]>>('ContentsForUser');

export const contentsReducer = reducer;
export const updateContentsAction = update;
export const fetchContentsAction =
  (courseId: number, userId: number) =>
  (dispatch: Dispatch<any>): void => {
    dispatch(replace({ [userId.toString()]: loading }));

    fetchContents(courseId, userId)
      .then(contents => {
        dispatch(replace({ [userId.toString()]: loaded(contents.objects) }));
      })
      .catch(error => {
        dispatch(replace({ [userId.toString()]: errored(error) }));
      });
  };
