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

import { pick } from 'lodash';
import lscache from 'lscache';

const REDUX_LS_STATE = 'REDUX_LS_STATE';

//only save the ones we positively want to save
const pathsToPersist = [['ui', 'gradebookTableOptions']];

export const createSubscription = store => {
  store.subscribe(() => {
    const pickedState = pick(store.getState(), pathsToPersist);
    lscache.set(REDUX_LS_STATE + window.lo_platform.course.id, pickedState);
  });
};

export const getSavedState = () => {
  return lscache.get(REDUX_LS_STATE + window.lo_platform.course.id);
};
