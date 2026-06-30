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

import { map } from 'lodash';
import React, { useEffect, useMemo, useRef } from 'react';

import SubmissionAttemptView from '../../contentPlayerComponents/activityViews/submission/views/SubmissionAttemptView.tsx';
import SubmissionInstructionsView from '../../contentPlayerComponents/activityViews/submission/views/SubmissionInstructionsView.tsx';
import { submissionActivityAPI } from '../../services/submissionActivityAPI.ts';
import { getSearchParams } from '../../utils/linkUtils.js';
import { GraderProvider, useGrader } from '../graderContext.tsx';
import { SubmissionGrader as SubmissionGraderCtor } from '../graders/pure/submissionGrader.ts';
import { GradingPanel } from '../gradingPanel/GradingPanel.tsx';

interface SubmissionGraderProps {
  assignment: any;
  onExit: () => void;
}

/**
 * React port of the `submissionGrader` Angular component (the instructor submission/observation grader
 * view): the split-pane layout (`.assignment-grader` > `.content-column` + `.panel-column`). The grader
 * (`SubmissionGrader`) is now the pure-TS constructor (graders/pure/submissionGrader.ts), `new`ed directly
 * and mutated in place; on mount it loads the user/attempt (`?forLearnerId`/`?attemptId`) and installs the
 * nav blocker. Re-renders are driven by the
 * shared `GraderProvider`/`useGrader` (B3 Phase 3).
 *
 * Content: the instructions view (instructor / observation = not student-driven) or the student's attempt
 * view (student-driven), both already-React (`SubmissionInstructionsView` / `SubmissionAttemptView`). The
 * attempt's attachments are enriched with view/download URLs (the old `$watch(activeAttempt)` →
 * `activityState`). The panel is the React `GradingPanel` (`variant` = submission / authentic). DOM
 * preserved: `.assignment-grader`/`.content-column`/`.panel-column`. `onChange`/`assignmentName`/`dueDate`
 * were vestigial (no-op / never displayed) and are dropped, as in `AssessmentGrader`.
 */
const SubmissionGraderContent: React.FC<{ submissionActivity: any; onExit: () => void }> = ({
  submissionActivity,
  onExit,
}) => {
  const { grader } = useGrader();
  const activeAttempt = grader.activeAttempt;

  // Enrich the active attempt's attachments with view/download URLs (the Angular controller's
  // `$watch(activeAttempt)` → `activityState`).
  const enrichedAttempt = useMemo(() => {
    if (!activeAttempt) return null;
    const api = submissionActivityAPI;
    return {
      ...activeAttempt,
      attachments: map(activeAttempt.attachments, (attachmentId: any) => {
        const info = activeAttempt.attachmentInfos[attachmentId];
        return {
          ...info,
          viewUrl: api.createAttachmentUrl(activeAttempt.id, attachmentId, false),
          downloadUrl: api.createAttachmentUrl(activeAttempt.id, attachmentId, true),
        };
      }),
    };
  }, [activeAttempt]);

  return (
    <div className="assignment-grader">
      <div className="content-column">
        <section>
          {!grader.isStudentDriven && (
            <div>
              <SubmissionInstructionsView submissionActivity={submissionActivity} />
            </div>
          )}
          {grader.isStudentDriven && enrichedAttempt && (
            <div>
              <SubmissionAttemptView
                attempt={enrichedAttempt}
                isGrading={true}
              />
            </div>
          )}
        </section>
      </div>

      <section className="panel-column">
        <GradingPanel
          onExit={onExit}
          variant={grader.isStudentDriven ? 'submission' : 'authentic'}
        />
      </section>
    </div>
  );
};

export const SubmissionGrader: React.FC<SubmissionGraderProps> = ({ assignment, onExit }) => {
  const graderRef = useRef<any>(null);
  if (!graderRef.current) {
    graderRef.current = new (SubmissionGraderCtor as any)(assignment);
  }
  const grader = graderRef.current;

  useEffect(() => {
    const { forLearnerId, attemptId } = getSearchParams();
    grader.changeUser(forLearnerId, attemptId).catch((error: any) => {
      // eslint-disable-next-line no-console
      console.log('Failed to change user on grader', error);
    });
    grader.blockNavForUnsavedChanges();
    return () => grader.removeNavBlocker();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submissionActivity = useMemo(() => ({ assessment: assignment }), [assignment]);

  return (
    <GraderProvider grader={grader}>
      <SubmissionGraderContent
        submissionActivity={submissionActivity}
        onExit={onExit}
      />
    </GraderProvider>
  );
};

