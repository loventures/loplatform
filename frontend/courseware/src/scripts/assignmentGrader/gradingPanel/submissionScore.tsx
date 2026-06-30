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

import React, { useEffect, useMemo, useRef, useState } from 'react';

import { grade as gradeFormat, makeGradeDisplayMethods } from '../../filters/pure/grade.ts';
import { useTranslation } from '../../i18n/translationContext';

interface SubmissionScoreProps {
  /** The grade model (stays Angular); the direct grade is `grade.outgoing.pointsAwarded`. */
  grade: any;
  invalidated?: boolean;
  late?: boolean;
  disableEdit?: boolean;
}

/**
 * React port of the `submissionScore` directive (assignmentGrader/gradingPanel): the "Score: N / M"
 * row — an editable direct-grade number input (or a read-only `--`/points display) plus the
 * percent/points suffix and the deleted/late badges. Was an Angular directive; now native React, bridged
 * back via react2angular (with the i18n provider) so the still-Angular graders / controls keep rendering
 * `<submission-score>`. The grade model stays Angular.
 *
 * The direct input reproduces Angular's `input[type=number]` validity: the displayed text is local
 * state, and the model (`grade.outgoing.pointsAwarded`) is set to the parsed value only when it's in
 * range [0, pointsPossible] — otherwise `undefined`, so `grade.isComplete()` is false and the post
 * button disables (the old behaviour). A `$rootScope.$watch` keeps the input in sync when the model
 * changes externally (rubric scoring, "Undo Changes"), since react2angular won't re-render on a nested
 * grade mutation. The dead `scaledAwarded` $watch (it watched a `grader` absent from the isolate scope
 * and was never displayed) is dropped. DOM preserved for Selenide: `.submission-score`,
 * `input.direct-grade-input`, `.points-awarded`, `.points-possible`.
 */
export const SubmissionScore: React.FC<SubmissionScoreProps> = ({ grade, invalidated, late, disableEdit }) => {
  const translate = useTranslation();
  const gradeMethods = useMemo(() => makeGradeDisplayMethods(translate), [translate]);

  const pointsPossible = grade?.pointsPossible;
  const pointsAwarded = grade?.outgoing?.pointsAwarded;

  // The direct-grade input is UNCONTROLLED. The grading panel's 150ms re-render poll (the consolidated
  // GraderProvider, #1552) fires constantly while grading and a controlled `value` would be re-asserted
  // mid-entry — racing Selenide's clear+type so the value gets clobbered/appended (the GradingPanelTest
  // "post a score with a decimal value" flake: value="7.5-75" instead of "-75"). With no `value` prop the
  // poll can't touch the DOM value; we only push EXTERNAL model changes (rubric scoring / undo / nav) into
  // the DOM via the ref, and `lastUserWriteRef` distinguishes those from the user's own keystroke.
  const inputRef = useRef<HTMLInputElement>(null);
  const lastUserWriteRef = useRef<any>(pointsAwarded);

  useEffect(() => {
    if (pointsAwarded === lastUserWriteRef.current) return;
    lastUserWriteRef.current = pointsAwarded;
    const domText = inputRef.current?.value ?? '';
    const modelRejected = domText !== '' && pointsAwarded == null;
    if (!modelRejected && inputRef.current) {
      inputRef.current.value = pointsAwarded == null ? '' : String(pointsAwarded);
    }
  }, [pointsAwarded]);

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    const n = raw === '' ? null : Number(raw);
    const valid = n != null && !isNaN(n) && n >= 0 && (pointsPossible == null || n <= pointsPossible);
    const modelVal = valid ? n : undefined;
    if (grade?.outgoing) grade.outgoing.pointsAwarded = modelVal;
    lastUserWriteRef.current = modelVal;
  };

  // The grader can briefly hand us a null grade while navigating (e.g. QuizGrader nulls activeGrade
  // between questions); render nothing rather than dereferencing it (Angular tolerated this implicitly).
  if (!grade) return null;
  const displayStyle = grade.displayStyle;

  const hasScore = pointsAwarded || pointsAwarded === 0;

  return (
    <div className="submission-score">
      <div className="submission-scoring">
        <span>{translate('Score')}</span>
        <span role="presentation">&nbsp;:&nbsp;</span>

        {!disableEdit ? (
          <input
            ref={inputRef}
            className="direct-grade-input form-control"
            type="text"
            inputMode="decimal"
            defaultValue={pointsAwarded == null ? '' : String(pointsAwarded)}
            onChange={onChange}
            aria-label={translate('GRADING_PANEL_SCORE_EDIT_AWARDED', { pointsAwarded })}
          />
        ) : (
          <span className="points-awarded">
            {!hasScore ? (
              <span aria-label={translate('GRADING_PANEL_SCORE_NO_GRADE')}>--</span>
            ) : (
              <span aria-label={translate('GRADING_PANEL_SCORE_CURRENT_AWARDED')}>
                {gradeFormat(gradeMethods, pointsAwarded, 'points') as React.ReactNode}
              </span>
            )}
          </span>
        )}

        {displayStyle === 'percent' && (
          <span
            className="points-possible"
            aria-label={translate('GRADING_PANEL_SCORE_PERCENT', { pointsPossible })}
          >
            &nbsp;%&nbsp;
          </span>
        )}
        {displayStyle === 'points' && (
          <span
            className="points-possible"
            aria-label={translate('GRADING_PANEL_SCORE_POINTS', { pointsPossible })}
          >
            &nbsp;/&nbsp;{pointsPossible}
          </span>
        )}
      </div>

      {invalidated && <div className="block-badge badge-danger">{translate('GRADER_ATTEMPT_DELETED')}</div>}
      {late && <div className="block-badge badge-warning">{translate('GRADER_ATTEMPT_LATE')}</div>}
    </div>
  );
};
