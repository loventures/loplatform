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

import { connectRouter, routerMiddleware as createRouterMiddleware } from 'connected-react-router';
import { createBrowserHistory } from 'history';
import { applyMiddleware, combineReducers, createStore } from 'redux';
import logger from 'redux-logger';
import { thunk } from 'redux-thunk';

import announcement from './redux/reducers/AnnouncementReducer';
import main from './redux/reducers/MainReducers';
import presence from './redux/reducers/PresenceReducer';
export const history = createBrowserHistory({
  basename: window.lo_base_url + '/',
});
const routerMiddleware = createRouterMiddleware(history);
const rootReducer = combineReducers({
  main,
  presence,
  announcement,
  router: connectRouter(history),
});

const middleWare =
  process.env.NODE_ENV === 'development'
    ? applyMiddleware(routerMiddleware, thunk, logger)
    : applyMiddleware(routerMiddleware, thunk);

export const store = createStore(rootReducer, middleWare);
