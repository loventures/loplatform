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

import React, { useEffect, useRef } from 'react';

import { QuestionView } from '../../quizQuestions/QuestionView.tsx';
import { getSearchParams } from '../../utils/linkUtils.js';
import { GraderProvider, useGrader } from '../graderContext.tsx';
import { QuizGrader } from '../graders/pure/quizGrader.ts';
import { GradingPanel } from '../gradingPanel/GradingPanel.tsx';

interface AssessmentGraderProps {
  assignmentId: string;
  onExit: () => void;
}

/**
 * React port of the `assessmentGrader` Angular component (the instructor quiz grader view): the
 * split-pane layout (`.assignment-grader` > `.content-column` questions + `.panel-column` panel). The
 * grader (`QuizGrader`) is now the pure-TS constructor (graders/pure/quizGrader.ts), `new`ed directly and
 * mutated in place; on mount it loads the user/attempt (`?forLearnerId`/`?attemptId`) and installs the
 * unsaved-changes nav blocker.
 *
 * The questions render through the React `QuestionView` dispatcher in grading mode (essay → the React
 * grading view, every other type → its base view read-only) — B3 Phase 4 retired the Angular
 * `questionLoader`. The panel is the React `GradingPanel` (B3 Phase 2a). The grader (`QuizGrader`) is the
 * pure-TS constructor, `new`ed directly and mutated in place; on mount it loads the user/attempt
 * (`?forLearnerId`/`?attemptId`) and installs the unsaved-changes nav blocker. Re-renders are driven by
 * the shared `GraderProvider`/`useGrader` (B3 Phase 3) — `GraderContent` consumes it, so the question
 * list + panel re-render on any grader mutation.
 *
 * `onChange` is dropped (the Angular `<assessment-grader on-change>` resolved to an undefined scope
 * member — a no-op); `assignmentName`/`dueDate` were never displayed by the wrapper. DOM preserved for
 * Selenide: `.assignment-grader`/`.content-column`/`.panel-column`, the `ul.list-group > li` question
 * list + `hr`.
 */
const GraderContent: React.FC<{ onExit: () => void }> = ({ onExit }) => {
  const { grader } = useGrader();
  return (
    <div className="assignment-grader">
      <div className="content-column">
        <section>
          <ul className="list-group list-unstyled">
            {(grader.displayedQuestionList ?? []).map((tuple: any) => (
              <li key={tuple.question.id}>
                <QuestionView
                  index={tuple.index}
                  question={tuple.question}
                  response={tuple.response}
                  canEditAnswer={false}
                  grading={true}
                />
                <hr />
              </li>
            ))}
          </ul>
        </section>
      </div>

      <section className="panel-column">
        <GradingPanel
          onExit={onExit}
          variant="quiz"
        />
      </section>
    </div>
  );
};

export const AssessmentGrader: React.FC<AssessmentGraderProps> = ({ assignmentId, onExit }) => {
  const graderRef = useRef<any>(null);
  if (!graderRef.current) {
    graderRef.current = new (QuizGrader as any)(assignmentId);
  }
  const grader = graderRef.current;

  useEffect(() => {
    const { forLearnerId, attemptId } = getSearchParams();
    grader.changeUser(forLearnerId, attemptId).catch(() => onExit());
    grader.blockNavForUnsavedChanges();
    return () => grader.removeNavBlocker();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <GraderProvider grader={grader}>
      <GraderContent onExit={onExit} />
    </GraderProvider>
  );
};

