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

import { applyMiddleware, combineReducers, createStore, Reducer } from 'redux';
import { logger } from 'redux-logger';
import { thunk } from 'redux-thunk';

import { LocalmailState, reducer } from '../localmail/reducks';
import main from '../redux/reducers/MainReducers';

// reducks' reducer is typed against its own action union, not the generic
// redux Action, so widen it for combineReducers.
const localmail = reducer as unknown as Reducer<LocalmailState>;

const rootReducer = combineReducers({
  main,
  localmail,
});

const middleWare =
  (process.env as any).NODE_ENV === 'development'
    ? applyMiddleware(thunk, logger)
    : applyMiddleware(thunk);

export const store = createStore(rootReducer, middleWare);
