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

import classnames from 'classnames';
import React, { useEffect } from 'react';

import { useTranslation } from '../../i18n/translationContext';
import { SubmissionScore } from './submissionScore.tsx';

interface GradingPanelControlsProps {
  grader: any;
  grade: any;
  onExit: () => void;
}

/**
 * React port of the `gradingPanelControls` component (gradingPanel cluster): the grader's bottom bar —
 * the running submission score + unposted-remaining status, and the prev/next/reset/save-draft/post
 * buttons. Was an Angular component; now native React rendering the React `SubmissionScore` (#1498)
 * directly, bridged back via react2angular (with the i18n provider) so the still-Angular `gradingPanel`
 * keeps rendering `<grading-panel-controls>`. The grader/grade models stay Angular.
 *
 * Every button's enabled state derives from the model (`grade.isDirty/isComplete/isReleased/
 * isSubmitting`, `grader.unpostedCount/can-edit/prev-next`), which mutates in place — react2angular only
 * re-renders on a binding reference change, so we re-render on a `$rootScope.$watch` over a signature of
 * those values (the old component re-evaluated them every digest). The vestigial `transclude: true`
 * (the consumer passes no content, and the template had no `ng-transclude`) is dropped.
 *
 * DOM preserved for Selenide: `.grading-panel-bottom`, `.grading-panel-status` (+ `all-grades-posted`),
 * the read-only `<submission-score>` (`.points-awarded`), `.exit-grader`, and the
 * `button.btn-danger`(reset) / `button.btn-success`(post/update) / `button.btn-primary`(save-draft)
 * controls.
 */
export const GradingPanelControls: React.FC<GradingPanelControlsProps> = ({ grader, grade, onExit }) => {
  const translate = useTranslation();

  useEffect(() => {
    grader.calculateUnpostedCount();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const isSubmitting = () => grade.isSubmitting;
  const canPostGrade = () =>
    (!isSubmitting() && grade.isReleased() && grade.isDirty() && grade.isComplete()) ||
    (!grade.isReleased() && grade.isComplete());
  const canSaveGrade = () => !isSubmitting() && grade.isDirty();

  // model mutations / async saves happen in React-land; the GraderProvider 150ms poll re-renders
  // this component (and the signature watch above) so the new state is picked up.
  const resetGrade = () => {
    grade.resetGrade();
  };
  const saveDraft = () => {
    grader.saveChanges(false);
  };
  const postGrade = () => {
    grader.saveChanges(true);
  };
  const changeToInfo = (info: any) => {
    if (info) grader.confirmDiscardChanges().then(() => grader.changeByInfo(info));
  };

  return (
    <div className="grading-panel-bottom">
      <div className={classnames('grading-panel-status', { 'all-grades-posted': grader.unpostedCount === 0 })}>
        <div className="status-flex-container">
          <SubmissionScore
            grade={grade}
            disableEdit
          />

          {!!grader.unpostedCount && (
            <div className="submissions-count">
              {grader.unpostedQuestionCount ? (
                <div>
                  <strong>{grader.unpostedQuestionCount}</strong>{' '}
                  <span>
                    {translate('QUESTIONS_REMAINING', { unposted: grader.unpostedQuestionCount }, 'messageformat')}
                  </span>
                </div>
              ) : null}

              <strong>{grader.unpostedCount}</strong>{' '}
              <span>{translate('SUBMISSIONS_REMAINING', { unposted: grader.unpostedCount }, 'messageformat')}</span>
            </div>
          )}

          {grader.unpostedCount === 0 && (
            <button
              className="btn btn-link text-white exit-grader"
              type="button"
              onClick={() => onExit()}
            >
              <span className="icon icon-check" />
              <span>{translate('ALL_DONE')}</span> <span>{translate('JUST_RETURN_TO_DASHBOARD')}</span>
            </button>
          )}
        </div>
      </div>

      <div className="grading-panel-controls">
        <button
          className="btn btn-outline-primary move-to-previous"
          type="button"
          title={translate(grader.prevItemToGrade?.text || '')}
          disabled={!grader.prevItemToGrade}
          onClick={() => changeToInfo(grader.prevItemToGrade)}
        >
          <span className="icon icon-angle-left" />
        </button>

        {grader.canUserEditGrade && grade ? (
          grade.isReleased() ? (
            <span>
              {grade.isDirty() && (
                <button
                  className="btn btn-danger reset-grade"
                  type="button"
                  onClick={resetGrade}
                  disabled={isSubmitting()}
                >
                  <span>{translate('GRADER_STATUS_RESET')}</span>
                </button>
              )}

              <button
                className="btn btn-success update-grade"
                type="button"
                onClick={postGrade}
                disabled={!canPostGrade()}
              >
                {canPostGrade() ? (
                  <span>{translate('GRADER_CONTROL_UPDATE')}</span>
                ) : (
                  <div>
                    <span>{translate('GRADER_STATUS_GRADE_RELEASED')}</span>
                    <span className="icon icon-check" />
                  </div>
                )}
              </button>
            </span>
          ) : (
            <span>
              <button
                className="btn btn-primary save-grade"
                type="button"
                onClick={saveDraft}
                disabled={!canSaveGrade()}
              >
                {!grade.isBlankGrade && !canSaveGrade() ? (
                  <span>{translate('GRADER_STATUS_DRAFT_SAVED')}</span>
                ) : (
                  <span>{translate('GRADER_CONTROL_DRAFT')}</span>
                )}
              </button>

              <button
                className="btn btn-success post-grade"
                type="button"
                disabled={!canPostGrade()}
                onClick={postGrade}
              >
                <span>{translate('GRADER_CONTROL_SUBMIT')}</span>
              </button>
            </span>
          )
        ) : null}

        <button
          className="btn btn-outline-primary move-to-next"
          type="button"
          title={translate(grader.nextItemToGrade?.text || '')}
          disabled={!grader.nextItemToGrade}
          onClick={() => changeToInfo(grader.nextItemToGrade)}
        >
          <span className="icon icon-angle-right" />
        </button>
      </div>
    </div>
  );
};
