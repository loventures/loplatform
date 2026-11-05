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

import { graderChanged } from '../loRedux/graderTickReducer';

/**
 * Signal that the mutable grader / grade model changed in place, so the React grading panel re-renders
 * (issue #1512). Call this from every grader/grade mutation the panel displays — it replaces the
 * `GraderProvider` 150ms signature poll.
 *
 * The store registers its dispatch via `registerGraderDispatch` (from `loRedux`) rather than this module
 * importing the store: the model + leaf components import only this helper, so pulling them into a unit
 * test does not eagerly construct the store (whose middleware reads `window.lo_platform` at import time).
 * Before registration (or outside a course app) this is a no-op.
 */
type GraderDispatch = (action: ReturnType<typeof graderChanged>) => void;

let storeDispatch: GraderDispatch | null = null;

export const registerGraderDispatch = (dispatch: GraderDispatch): void => {
  storeDispatch = dispatch;
};

export const notifyGraderChanged = (): void => {
  storeDispatch?.(graderChanged());
};
