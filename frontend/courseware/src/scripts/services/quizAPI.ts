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

import { currentUser } from '../utilities/currentUserData.ts';
import { request } from '../utilities/request.ts';
import { makeQuizAPI } from './pure/quizAPI.ts';

/**
 * Native (axios) QuizAPI for React/redux callers — replaces
 * `lojector.get('QuizAPI')`. Resolves via native Promises (no digest).
 *
 * The quiz player's AngularJS views (question rendering + save/submit status) read
 * the shared store via `$ngRedux.connectToCtrl`, so a no-digest dispatch would
 * normally leave them stale (the A2 regression). That is handled globally by the
 * `$ngRedux.subscribe(() => $rootScope.$applyAsync())` digest bridge in
 * `bootstrap/ngRedux.js`, so these consumers can use the native request safely.
 *
 * The current-user id comes from the pure `currentUser` singleton
 * (utilities/currentUserData.ts).
 */
export const quizAPI = makeQuizAPI(request, currentUser);
