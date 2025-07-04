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

import { connectRouter, routerMiddleware } from 'connected-react-router';
import { createBrowserHistory } from 'history';
import { applyMiddleware, combineReducers, createStore } from 'redux';
import logger from 'redux-logger';
import { thunk } from 'redux-thunk';

import main from '../redux/reducers/MainReducers';

export const history = createBrowserHistory({
  basename: window.lo_base_url + '/',
});

const rootReducer = combineReducers({
  main,
  router: connectRouter(history),
});

const middleWare =
  (process.env as any).NODE_ENV === 'development'
    ? applyMiddleware(routerMiddleware(history), thunk, logger)
    : applyMiddleware(routerMiddleware(history), thunk);

export const store = createStore(rootReducer, middleWare);
