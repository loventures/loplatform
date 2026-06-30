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

import React, { createContext, useContext, useEffect, useReducer } from 'react';

/**
 * B3 Phase 3 — the grader's single React re-render source.
 *
 * The grader (`QuizGrader`/`SubmissionGrader`) stays an Angular service mutated in place. Previously each
 * React grader component (panel, controls, score, student-picker, rubric/feedback panels, submission
 * sections, the question list) carried its own `$rootScope.$watch` or `setInterval` poll to notice those
 * mutations — digest-dependent and scattered. This provider replaces all of them with ONE digest-
 * independent poll over a comprehensive signature of the displayed grader state; on any change it bumps a
 * `version` in context. `GradingPanel` and the quiz content consume it (via `useGrader`) and re-render,
 * which cascades to every child leaf — so the leaves are plain components again (no per-component sync).
 *
 * (A future full refactor would move the grader state into Redux and subscribe instead of polling; this
 * consolidates the workaround into one place without rewriting the Angular grader service.)
 */
interface GraderContextValue {
  grader: any;
  version: number;
}

const GraderContext = createContext<GraderContextValue>({ grader: null, version: 0 });

export const useGrader = (): GraderContextValue => useContext(GraderContext);

// Comprehensive signature: the union of every field the React grader components display. Missing a field
// here means that component won't update when only that field changes — keep it exhaustive.
const graderSignature = (grader: any): string => {
  if (!grader) return '';
  const g = grader.activeGrade;
  const a = grader.activeAttempt;
  const sas = a?.scorableAttemptState ?? {};
  return [
    grader.activeUser?.id,
    grader.activeUser?.fullName,
    a?.id,
    a?.valid,
    a?.state,
    sas.scorePosted,
    sas.hasInstructorInput,
    sas.awaitsInstructorInput,
    sas.requiresInstructorInput,
    g ? 1 : 0,
    g && g.isReleased(),
    g && g.isDirty(),
    g && g.isComplete(),
    g && g.isSubmitting,
    g && g.isBlankGrade,
    g?.outgoing?.pointsAwarded,
    g?.outgoing?.rubric ? 1 : 0,
    grader.detailedGradeExists,
    grader.canUserEditGrade,
    grader.unpostedCount,
    grader.unpostedQuestionCount,
    grader.prevItemToGrade?.text,
    grader.nextItemToGrade?.text,
    grader.inProgressAttempt?.id,
    a ? grader.canInvalidateAttempt() : false,
    (grader.displayedQuestionList ?? []).map((t: any) => t.question?.id).join(','),
    (grader.gradableQuestionList ?? []).map((q: any) => q.question?.id).join(','),
  ].join('|');
};

export const GraderProvider: React.FC<{ grader: any; children: React.ReactNode }> = ({
  grader,
  children,
}) => {
  const [version, bump] = useReducer((c: number) => c + 1, 0);

  useEffect(() => {
    let last = graderSignature(grader);
    const id = window.setInterval(() => {
      const next = graderSignature(grader);
      if (next !== last) {
        last = next;
        bump();
      }
    }, 150);
    return () => window.clearInterval(id);
  }, [grader]);

  return <GraderContext.Provider value={{ grader, version }}>{children}</GraderContext.Provider>;
};
