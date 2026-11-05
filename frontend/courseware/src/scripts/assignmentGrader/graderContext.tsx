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

import React, { createContext, useContext } from 'react';

import { useCourseSelector } from '../loRedux';
import { selectGraderTick } from '../loRedux/graderTickReducer';

/**
 * The grader's single React re-render source (issue #1512).
 *
 * The grader (`QuizGrader`/`SubmissionGrader`) and its grade model stay MUTABLE objects mutated in place.
 * React can't observe an in-place mutation of an object it didn't own, so the leaves (panel, controls,
 * score, student-picker, rubric/feedback panels, submission sections, the question list) read the model
 * directly and rely on this provider re-rendering them. Every grade/grader mutation calls
 * `notifyGraderChanged()`, which bumps the `graderTick` Redux slice; the provider subscribes to it and
 * exposes it as `version`. When it changes the context value identity changes, so every `useGrader`
 * consumer re-renders, cascading to every child leaf — so the leaves stay plain components.
 *
 * This replaces the former single `setInterval(150ms)` signature poll (B3 Phase 3): mutations now push a
 * re-render through the store instead of a timer pulling one. See `notifyGraderChanged` for the (now
 * exhaustive, by construction) set of mutation sites that signal.
 */
interface GraderContextValue {
  grader: any;
  version: number;
}

const GraderContext = createContext<GraderContextValue>({ grader: null, version: 0 });

export const useGrader = (): GraderContextValue => useContext(GraderContext);

export const GraderProvider: React.FC<{ grader: any; children: React.ReactNode }> = ({
  grader,
  children,
}) => {
  const version = useCourseSelector(selectGraderTick);

  return <GraderContext.Provider value={{ grader, version }}>{children}</GraderContext.Provider>;
};
