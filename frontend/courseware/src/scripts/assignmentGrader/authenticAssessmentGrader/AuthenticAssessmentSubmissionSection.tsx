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

import { isEmpty } from 'lodash';
import React, { useEffect, useState } from 'react';

import LoadingSpinner from '../../directives/loadingSpinner/index.tsx';
import { openConfirmModal } from '../../directives/modalHost/ConfirmModal.tsx';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { openErrorModal } from '../../modals/errorModal/errorModal.tsx';
import { AttemptDropdownItem } from '../assessmentGrader/AttemptDropdownItem.tsx';
import { SubmissionScore } from '../gradingPanel/submissionScore.tsx';
import { GraderSelect } from '../graderSelect/GraderSelect.tsx';

interface AuthenticAssessmentSubmissionSectionProps {
  grader: any;
  user: any;
  attempt: any;
}

/**
 * React port of the `authenticAssessmentSubmissionSection` Angular component (the authentic/observation
 * submission grader's selector panel): the no-attempts message + "create attempt" button, the attempt
 * dropdown (+ delete + start/goto-in-progress buttons), and the submission score. The instructor-driven
 * variant of `AssessmentSubmissionSection` (no question dropdown; adds start-attempt). Same patterns:
 * React `GraderSelect` replaces `lo-select`; `SubmissionScore` rendered directly; the delete flow uses the
 * React modal host (`openConfirmModal`/`openErrorModal`); a poll re-renders on in-place grader mutation (B3 Phase 3 removes it). DOM
 * preserved for Selenide: `.authentic-submission-section`, `.submission-selection-control`,
 * `button.start-attempt`, `button.goto-current-attempt`, `button.btn-outline-danger`, `.alert-info`.
 */
export const AuthenticAssessmentSubmissionSection: React.FC<
  AuthenticAssessmentSubmissionSectionProps
> = ({ grader, user, attempt }) => {
  const translate = useTranslation();
  const [attempts, setAttempts] = useState<any[]>([]);
  const [hasAttempts, setHasAttempts] = useState(false);
  const [startingAttempt, setStartingAttempt] = useState(false);

  const updateAttempts = () =>
    grader.loadUserOrderedAttempts().then((a: any[]) => {
      setAttempts(a || []);
      setHasAttempts(!isEmpty(a));
    });

  useEffect(() => {
    updateAttempts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, attempt]);

  // $onInit registered updateAttempts as the grader's on-change callback.
  useEffect(() => {
    grader.registerOnChangeCallback(() => updateAttempts());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const changeAttempt = (a: any) => {
    return grader.changeAttempt(a.id);
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

  const startAttempt = () => {
    setStartingAttempt(true);
    grader.startAttempt().then((newAttempt: any) => {
      changeAttempt(newAttempt).then(() => setStartingAttempt(false));
    });
  };

  const StartAttemptButton = (
    <button
      className="btn btn-primary start-attempt"
      disabled={startingAttempt}
      onClick={startAttempt}
    >
      <span>{translate('AUTHENTIC_ASSESSMENT_GRADER_CREATE_ATTEMPT')}</span>
      {startingAttempt && <LoadingSpinner />}
    </button>
  );

  // Read the live grader.activeAttempt for render values (not the `attempt` prop): the poll re-renders
  // at arbitrary times, and `canInvalidateAttempt()` dereferences `this.activeAttempt.valid`, so a
  // stale-truthy prop against a transiently-null live attempt would crash (Angular swallowed it).
  const activeAttempt = grader.activeAttempt;

  return (
    <section className="authentic-submission-section grader-panel">
      {!hasAttempts && (
        <div className="submission-selection-control">
          <div className="alert alert-info">{translate('AUTHENTIC_ASSESSMENT_GRADER_NO_ATTEMPTS')}</div>
        </div>
      )}

      {!hasAttempts && <div className="submission-selection-control">{StartAttemptButton}</div>}

      {hasAttempts && (
        <div className="submission-selection-control">
          <GraderSelect
            options={attempts}
            selected={activeAttempt}
            getKey={a => a.id}
            renderSelected={a => (
              <AttemptDropdownItem
                attempt={a}
                icon="icon-chevron-down"
              />
            )}
            renderOption={a => <AttemptDropdownItem attempt={a} />}
            onSelect={changeAttempt}
          />

          {activeAttempt && grader.canInvalidateAttempt() && (
            <button
              className="btn btn-outline-danger delete-attempt"
              onClick={confirmDelete}
              title={translate('GRADER_DELETE_ATTEMPT_TOOLTIP')}
            >
              <i className="icon icon-trash" />
            </button>
          )}
        </div>
      )}

      {grader.activeGrade && (
        <div className="submission-selection-control">
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

          {!grader.inProgressAttempt && StartAttemptButton}

          {grader.inProgressAttempt && (
            <button
              className="btn btn-primary goto-current-attempt"
              disabled={activeAttempt?.id === grader.inProgressAttempt.id}
              onClick={() => changeAttempt(grader.inProgressAttempt)}
            >
              <span>{translate('AUTHENTIC_ASSESSMENT_GRADER_GOTO_IN_PROGRESS')}</span>
            </button>
          )}
        </div>
      )}
    </section>
  );
};
