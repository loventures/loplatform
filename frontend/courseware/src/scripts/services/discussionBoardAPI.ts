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

import settings from '../utilities/settingsService';
import { request } from '../utilities/request.ts';
import { makeDiscussionBoardAPI } from './pure/discussionBoardAPI.ts';
import { makeDiscussionBoardAPILight } from './pure/discussionBoardAPILight.ts';
import { makeDiscussionPostAPI } from './pure/discussionPostAPI.ts';
import { makeDiscussionPostReplyAPI } from './pure/discussionPostReplyAPI.ts';
import { makeDiscussionPostStateAPI } from './pure/discussionPostStateAPI.ts';
import { courseReduxStore } from '../loRedux';

/**
 * Native (axios) composite DiscussionBoardAPI for React/redux callers — replaces
 * `lojector.get('DiscussionBoardAPI')`. Resolves via native Promises (no digest),
 * which is correct for its redux loaders/actions (discussion list + close policy);
 * none binds the result to an Angular `$scope`.
 *
 * The four sub-APIs are wired natively here. Still-Angular deps are resolved
 * lazily from `lojector` (one contained reach-in each): `Settings` for the board
 * list summaries, and `$ngRedux.getState` for the reply API's gating-error lookup.
 * The in-service `$q` runtimes become native (`Promise.resolve` / `Promise.all`).
 */
const discussionPostAPI = makeDiscussionPostAPI(request);
const discussionBoardAPILight = makeDiscussionBoardAPILight(request, settings);
const discussionPostReplyAPI = makeDiscussionPostReplyAPI(
  request,
  discussionPostAPI,
  () => courseReduxStore.getState(),
  value => Promise.resolve(value)
);
const discussionPostStateAPI = makeDiscussionPostStateAPI(request, promises => Promise.all(promises));

export const discussionBoardAPI = makeDiscussionBoardAPI(
  discussionBoardAPILight,
  discussionPostAPI,
  discussionPostReplyAPI,
  discussionPostStateAPI
);
