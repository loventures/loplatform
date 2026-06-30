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

import React, { useEffect, useState } from 'react';

import LoadingSpinner from '../../directives/loadingSpinner/index.tsx';
import { openConfirmModal } from '../../directives/modalHost/ConfirmModal.tsx';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { openErrorModal } from '../../modals/errorModal/errorModal.tsx';
import { SubmissionScore } from '../gradingPanel/submissionScore.tsx';
import { GraderSelect } from '../graderSelect/GraderSelect.tsx';
import { AttemptDropdownItem } from './AttemptDropdownItem.tsx';
import { QuestionDropdownItem } from './QuestionDropdownItem.tsx';

interface AssessmentSubmissionSectionProps {
  grader: any;
  user: any;
  attempt: any;
  activeQuestions?: any[];
}

/**
 * React port of the `assessmentSubmissionSection` Angular component (the quiz grader's submission
 * selector panel): the attempt dropdown (+ delete-attempt button), the question dropdown, and the
 * submission score. Replaces the two `lo-select` (`compile`) dropdowns with the React `GraderSelect`,
 * and renders the already-React `SubmissionScore` directly. The delete-attempt confirm/fail flow uses the
 * React modal host (`openConfirmModal` / `openErrorModal`).
 *
 * The grader (`QuizGrader`) stays Angular and is mutated in place, so — like its sibling grader leaves
 * (`submissionScore`/`gradingPanelControls`) — a small poll forces a re-render when the displayed
 * attempt/grade state changes (B3 Phase 3 replaces this with a React store). DOM preserved for Selenide
 * (`GradingPanelAttemptDropdown` / `LoSelect`): `.assessment-submission-section`,
 * `.submission-selection-control`, `.lo-select.dropdown`, `ul > li.btn`, `.block-badge`,
 * `button.icon-btn-danger`.
 */
export const AssessmentSubmissionSection: React.FC<AssessmentSubmissionSectionProps> = ({
  grader,
  user,
  attempt,
  activeQuestions,
}) => {
  const translate = useTranslation();
  const [attempts, setAttempts] = useState<any[]>([]);
  const [questions, setQuestions] = useState<any[]>([]);

  const updateAttempts = () =>
    grader.loadUserOrderedAttempts().then((a: any[]) => setAttempts(a || []));

  const updateBoth = () => {
    updateAttempts();
    if (typeof grader.loadAttemptGradableQuestions === 'function') {
      grader.loadAttemptGradableQuestions().then((q: any[]) => setQuestions(q || []));
    }
  };

  // $onInit always loads both; $onChanges reloaded on user/attempt changes. The grader loaders are
  // idempotent, so reloading both on either binding change matches the original behaviour.
  useEffect(() => {
    updateBoth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, attempt]);

  const changeAttempt = (a: any) => {
    grader.changeAttempt(a.id);
  };

  // changeQuestion runs an internal $timeout/$q chain that self-digests; the section's poll picks up the
  // new activeGrade.
  const changeQuestion = (q: any) => {
    grader.changeQuestion(q.index);
  };

  const confirmDelete = () => {
    return openConfirmModal({
      header: 'GRADER_DELETE_ATTEMPT_CONFIRM_HEADER',
      message: 'GRADER_DELETE_ATTEMPT_CONFIRM_MSG',
      confirmButton: 'GRADER_DELETE_ATTEMPT_CONFIRM_OK',
      cancelButton: 'GRADER_DELETE_ATTEMPT_CONFIRM_CANCEL',
      confirmClass: 'btn-success',
      cancelClass: 'btn-danger',
    }).then(
      () => {
        grader.invalidateAttempt().catch(() =>
          openErrorModal({
            title: 'GRADER_DELETE_ATTEMPT_FAIL_HEADER',
            message: 'GRADER_DELETE_ATTEMPT_FAIL_MSG',
            buttons: { hideSecondaryButton: true },
          })
        );
      },
      () => {} // cancel: no-op (matches the old $uibModal dismiss)
    );
  };

  const showQuestionsSelector =
    !!activeQuestions && !!activeQuestions[0] && grader.assignmentType !== 'final-project';

  // Read the live grader.activeAttempt for render values (not the `attempt` prop): the poll re-renders
  // at arbitrary times, and `canInvalidateAttempt()` dereferences `this.activeAttempt.valid`, so a
  // stale-truthy prop against a transiently-null live attempt would crash (Angular swallowed it).
  const activeAttempt = grader.activeAttempt;

  return (
    <section className="assessment-submission-section grader-panel">
      <div className="submission-selection-control grading-panel-section-input-text-align">
        <GraderSelect
          options={attempts}
          selected={activeAttempt}
          getKey={a => a.id}
          renderSelected={a => (
            <AttemptDropdownItem attempt={a} />
          )}
          renderOption={a => <AttemptDropdownItem attempt={a} />}
          onSelect={changeAttempt}
        />

        {activeAttempt && grader.canInvalidateAttempt() && (
          <button
            className="icon-btn icon-btn-danger mx-1"
            onClick={confirmDelete}
            title={translate('GRADER_DELETE_ATTEMPT_TOOLTIP')}
          >
            <i className="icon icon-trash" />
          </button>
        )}
      </div>

      {showQuestionsSelector && (
        <div className="submission-selection-control grading-panel-section-input-text-align">
          <GraderSelect
            options={questions}
            selected={activeQuestions![0]}
            getKey={q => q.id ?? q.index}
            renderSelected={q => <QuestionDropdownItem question={q} />}
            renderOption={q => <QuestionDropdownItem question={q} />}
            onSelect={changeQuestion}
          />
        </div>
      )}

      <div className="submission-selection-control">
        {!grader.activeGrade && <LoadingSpinner />}

        {grader.activeGrade && (
          <SubmissionScore
            grade={grader.activeGrade}
            invalidated={!grader.activeAttempt?.valid}
            late={grader.activeGrade.isLate?.()}
            disableEdit={
              !!grader.activeGrade.outgoing?.rubric ||
              !grader.canUserEditGrade ||
              !grader.detailedGradeExists
            }
          />
        )}
      </div>
    </section>
  );
};
