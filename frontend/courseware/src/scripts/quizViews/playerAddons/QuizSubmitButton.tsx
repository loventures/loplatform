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

import { openReactModal } from '../../directives/modalHost/reactModalHost.tsx';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { QuizSubmitModalBody } from './QuizSubmitModalBody.tsx';

interface QuizSubmitButtonProps {
  canSubmit: boolean;
  unansweredQuestions?: { ordinal: number }[];
  onAttempt?: number;
  maxAttempts?: number;
  submitQuiz: () => void;
  enableAutofocus?: boolean;
  /** When the parent flips this true (keyboard Enter on the last question) the modal auto-opens. */
  showWarning?: boolean;
  isCheckpoint?: boolean;
}

/**
 * React port of the `quizSubmitButton` component: the "Submit Quiz" button that opens the (already-React)
 * `QuizSubmitModalBody` confirmation, resolving → `submitQuiz`. Now native React with no react2angular
 * bridge — its only consumers are the React players + `MpControls`. DOM preserved:
 * `button.quiz-submit-button.btn.btn-success`; the modal's `.quiz-submit-modal` is unchanged.
 */
export const QuizSubmitButton: React.FC<QuizSubmitButtonProps> = ({
  canSubmit,
  unansweredQuestions = [],
  onAttempt,
  maxAttempts,
  submitQuiz,
  enableAutofocus,
  showWarning,
  isCheckpoint,
}) => {
  const translate = useTranslation();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const isOpen = useRef(false);

  const warnMaxAttempts = (maxAttempts ?? 0) > 0 && !isCheckpoint;
  const warnUnanswered = unansweredQuestions.length > 0;
  const unansweredQuestionCount = unansweredQuestions.length;
  const unansweredQuestionNumbers = unansweredQuestions.map(q => q.ordinal).join(', ');
  const maxAttemptsMsg =
    maxAttempts === 1
      ? 'WARN_ONLY_ATTEMPT_MSG'
      : onAttempt === maxAttempts
        ? 'WARN_FINAL_ATTEMPT_MSG'
        : 'WARN_MAX_ATTEMPT_MSG';
  const submitMsg = isCheckpoint ? 'CHECKPOINT_PLAYER_SUBMIT' : 'QUIZ_PLAYER_SUBMIT';

  const click = () => {
    if (isOpen.current) return;
    isOpen.current = true;
    openReactModal(controls => (
      <QuizSubmitModalBody
        {...controls}
        warnMaxAttempts={warnMaxAttempts}
        maxAttemptsMsg={maxAttemptsMsg}
        onAttempt={onAttempt as number}
        maxAttempts={maxAttempts as number}
        warnUnanswered={warnUnanswered}
        unansweredQuestionNumbers={unansweredQuestionNumbers}
        unansweredQuestionCount={unansweredQuestionCount}
        isCheckpoint={isCheckpoint as boolean}
      />
    ))
      .then(() => {
        isOpen.current = false;
        submitQuiz();
      })
      .catch(() => {
        isOpen.current = false;
        // return focus to the button (the old controller's `$timeout(...250)`)
        setTimeout(() => buttonRef.current?.focus(), 250);
      });
  };

  // The old `<quiz-submit-button show-warning>`: open the modal when the parent raises the flag.
  useEffect(() => {
    if (showWarning) click();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showWarning]);

  // `lo-autofocus="300"` — focus the button shortly after it appears.
  useEffect(() => {
    if (!enableAutofocus) return;
    const t = setTimeout(() => buttonRef.current?.focus(), 300);
    return () => clearTimeout(t);
  }, [enableAutofocus]);

  if (!canSubmit) return null;

  return (
    <button
      ref={buttonRef}
      className="quiz-submit-button btn btn-success"
      onClick={click}
    >
      <span>{translate(submitMsg)}</span>
    </button>
  );
};

export default QuizSubmitButton;
