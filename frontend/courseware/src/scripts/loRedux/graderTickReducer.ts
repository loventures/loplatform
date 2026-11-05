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

/**
 * The grader's reactivity tick (issue #1512).
 *
 * The instructor grader and its grade model (`graders/pure/*`, `assignmentGrade/models/pure/*`) are
 * deliberately MUTABLE, lifted-and-shifted from Angular and read in place by the React grading-panel
 * leaves. React can't observe an in-place mutation of an object it didn't own, so previously a single
 * `GraderProvider` `setInterval(150ms)` polled a comprehensive signature and bumped a context version.
 *
 * This replaces that timer with a Redux subscription: every grader/grade mutation calls
 * `notifyGraderChanged()` (see `assignmentGrader/notifyGraderChanged`), which dispatches `GRADER_CHANGED`
 * to bump this counter. `GraderProvider` reads it via `useSelector` and feeds it down as the same
 * `version`, so the leaves keep reading the mutable model but re-render on mutation instead of on a timer.
 */
export const GRADER_CHANGED = '@@grader/CHANGED';

export const graderChanged = () => ({ type: GRADER_CHANGED } as const);

export const graderTickReducer = (state = 0, action: { type: string }): number =>
  action.type === GRADER_CHANGED ? state + 1 : state;

export const selectGraderTick = (state: { graderTick: number }): number => state.graderTick;
