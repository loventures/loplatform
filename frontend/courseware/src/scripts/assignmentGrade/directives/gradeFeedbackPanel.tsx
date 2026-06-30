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

import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';

import { FeedbackFileList } from '../../assignmentFeedback/directives/feedbackFileList.tsx';
import { FeedbackTools } from '../../assignmentFeedback/directives/feedbackTools.tsx';
import { useTranslation } from '../../i18n/translationContext';

interface GradeFeedbackPanelProps {
  /** A `CompositeGrade` (stays Angular) — `grade.outgoing.{feedback,feedbackManager}` mutated in place. */
  grade: any;
  disableEdit?: boolean;
}

/**
 * React port of the `gradeFeedbackPanel` directive (assignmentGrade): the overall-assignment feedback
 * region in the grading panel — an auto-growing comment textarea plus the attachment tools (or, when
 * read-only, the comment text + attachment list, or a "no comments" notice). Was a small Angular
 * directive; now native React composing the already-React `FeedbackTools` (#1507) + `FeedbackFileList`
 * directly, bridged back via react2angular so the still-Angular `gradingPanel` keeps rendering
 * `<grade-feedback-panel>`.
 *
 * The `CompositeGrade` stays Angular and is mutated in place. The comment textarea is controlled by
 * local state; an effect on the `grade.outgoing` reference resyncs the text on undo (`resetGrade` swaps
 * `grade.outgoing`) / navigation — the old directive's `ng-model` did this automatically.
 * `grader.saveChanges` reads `outgoing.feedback` synchronously at save, so no digest nudge is needed.
 * `msd-elastic` (auto-grow) is reproduced with a layout effect. DOM preserved for Selenide
 * (`GradingPanel.feedbackInput`/`feedbackFileUpload`): `form.grade-feedback`, `textarea.feedback-input`,
 * `.alert-info`, and the `.attachment-tool` from `FeedbackTools`.
 */
export const GradeFeedbackPanel: React.FC<GradeFeedbackPanelProps> = ({ grade, disableEdit = false }) => {
  const translate = useTranslation();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const outgoing = grade && grade.outgoing;
  const feedbackManager = outgoing && outgoing.feedbackManager;

  // Controlled locally (like submissionScore) so typing re-renders without reading through Angular.
  // The old directive used `ng-model="grade.outgoing.feedback"`, which auto-reflected when
  // `CompositeGrade.resetGrade` replaced `grade.outgoing` (undo) or the grader swapped `activeGrade`
  // (navigation). The component re-renders via the shared GraderProvider (B3 Phase 3), so resync the text
  // whenever the `outgoing` reference changes (undo/nav) — not on each keystroke (the ref is stable then,
  // so the user's in-progress text stands).
  const [feedback, setFeedback] = useState<string>(() => (outgoing && outgoing.feedback) || '');
  useEffect(() => {
    setFeedback((outgoing && outgoing.feedback) || '');
  }, [outgoing]);

  // msd-elastic: grow the textarea to fit its content.
  useLayoutEffect(() => {
    const el = textareaRef.current;
    if (el) {
      el.style.height = 'auto';
      el.style.height = `${el.scrollHeight}px`;
    }
  });

  const onChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    setFeedback(value);
    if (outgoing) outgoing.feedback = value;
  };

  if (!grade) return null;

  return (
    <form
      className="grade-feedback grading-panel-section-input-text-align"
      role="region"
      aria-label={translate('GRADING_PANEL_OVERALL_FEEDBACK')}
    >
      {(!disableEdit || feedback) && (
        <textarea
          ref={textareaRef}
          className="feedback-input form-control"
          disabled={disableEdit}
          value={feedback}
          onChange={onChange}
          aria-label={translate('GRADING_PANEL_OVERALL_FEEDBACK_TEXT')}
          placeholder={translate('GRADING_PANEL_OVERALL_FEEDBACK_TEXT')}
        />
      )}

      {disableEdit && !feedback && <div className="alert alert-info">{translate('NO_COMMENTS')}</div>}

      {!disableEdit && feedbackManager && <FeedbackTools feedbackManager={feedbackManager} />}

      {disableEdit && feedbackManager && <FeedbackFileList files={feedbackManager.files} />}
    </form>
  );
};
