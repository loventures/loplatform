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

import { composableCombineReducers, createNamedReducer } from '../../utilities/reduxify.js';

import listReducer from '../../list/listReducer.js';

import {
  discussionsSlice,
  threadsDataSlice,
  postsDataSlice,
  boardsStateSlice,
  postsStateSlice,
  threadsStateSlice,
  jumpersSlice,
  searchSlice,
  viewsSlice,
  studentPickerSlice,
} from '../sliceNames.js';

import boardsReducer from './boardsReducer.js';
import postsReducer from './postsReducer.js';
import threadsReducer from './threadsReducer.js';
import viewsReducer from './viewsReducer.js';
import jumpersReducer from './jumpersReducer.js';
import searchReducer from './searchReducer.js';

import threadsDataReducer from './threadsDataReducer.js';

export { discussionsSlice, threadsDataSlice, postsDataSlice, threadsDataReducer };

export default composableCombineReducers({
  [boardsStateSlice]: boardsReducer,

  [postsStateSlice]: postsReducer,

  [threadsStateSlice]: threadsReducer,

  [viewsSlice]: viewsReducer,

  [jumpersSlice]: jumpersReducer,

  [searchSlice]: searchReducer,

  [studentPickerSlice]: createNamedReducer(studentPickerSlice, listReducer),
});
