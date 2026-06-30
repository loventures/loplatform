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

import { useTranslation } from '../../i18n/translationContext';
import type { ModalControls } from '../../directives/modalHost/reactModalHost';
import React from 'react';

/**
 * React port of quizSubmitModal.html (the quiz/checkpoint submit confirmation).
 * DOM preserved: `.quiz-submit-modal` wrapper, `.modal-body.no-last-p-margin`
 * with the conditional warning paragraphs, and a `.modal-footer` with cancel
 * (`.btn.btn-outline-info`) then submit (`.btn.btn-success`). Submit resolves
 * (the old `submitQuiz` → modal close); cancel rejects (→ `cancel`).
 */

export interface QuizSubmitModalProps {
  warnMaxAttempts: boolean;
  maxAttemptsMsg: string;
  onAttempt: number;
  maxAttempts: number;
  warnUnanswered: boolean;
  unansweredQuestionNumbers: string;
  unansweredQuestionCount: number;
  isCheckpoint: boolean;
}

export const QuizSubmitModalBody: React.FC<ModalControls & QuizSubmitModalProps> = ({
  close,
  dismiss,
  warnMaxAttempts,
  maxAttemptsMsg,
  onAttempt,
  maxAttempts,
  warnUnanswered,
  unansweredQuestionNumbers,
  unansweredQuestionCount,
  isCheckpoint,
}) => {
  const translate = useTranslation();
  const anyWarning = warnMaxAttempts || warnUnanswered;
  return (
    <div className="quiz-submit-modal">
      <div className="modal-body no-last-p-margin">
        {warnMaxAttempts && (
          <p>
            {translate(maxAttemptsMsg, { attemptNumber: onAttempt, maxAttemptsAllowed: maxAttempts })}
          </p>
        )}
        {warnUnanswered && (
          <p>
            {translate('WARN_UNANSWERED_MSG', {
              questionOrdinals: unansweredQuestionNumbers,
              plural: unansweredQuestionCount !== 1,
            })}
          </p>
        )}
        {anyWarning && (
          <p>{translate(isCheckpoint ? 'CONFIRM_SUBMIT_CHECKPOINT_MSG' : 'CONFIRM_SUBMIT_QUIZ_MSG')}</p>
        )}
        {!anyWarning && (
          <p className="no-warnings">
            {translate(
              isCheckpoint ? 'CONFIRM_SUBMIT_CHECKPOINT_MSG_NO_WARNINGS' : 'CONFIRM_SUBMIT_QUIZ_MSG_NO_WARNINGS'
            )}
          </p>
        )}
      </div>
      <div className="modal-footer">
        <button
          className="btn btn-outline-info ms-2"
          onClick={() => dismiss('cancel')}
        >
          {translate('QUIZ_PLAYER_CANCEL_SUBMIT')}
        </button>
        <button
          className="btn btn-success"
          onClick={() => close()}
        >
          {translate('QUIZ_PLAYER_CONFIRM_SUBMIT')}
        </button>
      </div>
    </div>
  );
};
