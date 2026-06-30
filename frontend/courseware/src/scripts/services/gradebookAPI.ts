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
import { makeGradebookAPI } from './pure/gradebookAPI.ts';

/**
 * Native (axios) GradebookAPI for React/redux callers — replaces
 * `lojector.get('GradebookAPI')`. Resolves via native Promises (no digest), which
 * is correct for its React/redux/react-query consumers (download-url builders,
 * the gradebook loaders, ER learner pages, grade-cell editing) — none bind the
 * result to an Angular `$scope`.
 *
 * `Settings` is still an AngularJS service, so it is resolved lazily from
 * `lojector` on each use (a single contained reach-in that replaces the 8
 * call-site reach-ins) until Settings is itself migrated.
 */
export const gradebookAPI = makeGradebookAPI(settings, request);
