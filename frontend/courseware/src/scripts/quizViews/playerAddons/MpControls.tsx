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

import classNames from 'classnames';
import React, { useEffect, useMemo, useRef, useState } from 'react';

import { trackQuizKeyboard } from '../../analytics/trackEvents.ts';
import LoadingSpinner from '../../directives/loadingSpinner';
import { useTranslation } from '../../i18n/translationContext.tsx';
import settings from '../../utilities/settingsService.ts';
import { QuizSubmitButton } from './QuizSubmitButton.tsx';

interface ConfidenceButton {
  value: number;
  class: string;
  label: string;
}

const DEFAULT_CONFIDENCE_BUTTONS: ConfidenceButton[] = [
  { value: 1, class: 'confidence-high', label: 'CONFIDENCE_HIGH' },
  { value: 0.5, class: 'confidence-med', label: 'CONFIDENCE_MEDIUM' },
  { value: 0, class: 'confidence-low', label: 'CONFIDENCE_LOW' },
];

interface MpControlsProps {
  displayConfidenceIndicators?: boolean;
  displaySkip?: boolean;
  selectedConfidence?: number;
  hasUnsavedChanges?: boolean;
  isLastQuestion?: boolean;
  unansweredQuestions?: { ordinal: number }[];
  onAttempt?: number;
  maxAttempts?: number;
  canEditAnswer?: boolean;
  nextQuestion: () => void;
  canSubmitQuestion?: boolean;
  submitQuestion: (confidence?: number) => void;
  canSubmitQuiz?: boolean;
  submitQuiz: () => void;
  skipQuestion: () => void;
  isSubmitting?: boolean;
  isCheckpoint?: boolean;
}

/**
 * React port of the `mpControls` component: the multi-page player's per-question controls — confidence
 * buttons (or a plain submit-question button), skip, next, and (on the last question) the React
 * `QuizSubmitButton`. Reproduces the Angular controller's keyboard shortcuts via a `document` keydown
 * listener (Enter submits the question / raises the submit-quiz warning; number keys pick a confidence /
 * submit / skip), skipping events from the rich-text editor, fill-blank inputs, and open modals. Now
 * native React with no bridge. DOM preserved for MultiPageQuizNavTest / QuizKeyboardNavTest:
 * `.mp-player-controls`, `.mp-player-control-btns`, `.submit-question`, `.skip-question`,
 * `.next-question`, `.unsaved-warning`, the confidence `btn`s.
 */
export const MpControls: React.FC<MpControlsProps> = ({
  displayConfidenceIndicators,
  displaySkip,
  selectedConfidence,
  hasUnsavedChanges,
  isLastQuestion,
  unansweredQuestions,
  onAttempt,
  maxAttempts,
  canEditAnswer,
  nextQuestion,
  canSubmitQuestion,
  submitQuestion,
  canSubmitQuiz,
  submitQuiz,
  skipQuestion,
  isSubmitting,
  isCheckpoint,
}) => {
  const translate = useTranslation();
  const [showSubmitWarning, setShowSubmitWarning] = useState(false);

  // Settings are read once (they don't change for the attempt) — mirrors the Angular constructor.
  const { confidenceButtons, warnUnsaved, skippingOk } = useMemo(() => {
    return {
      confidenceButtons: (settings.getSettings('QuizConfidenceButtons') ||
        DEFAULT_CONFIDENCE_BUTTONS) as ConfidenceButton[],
      warnUnsaved: settings.isFeatureEnabled('AssessmentSaveWarning'),
      skippingOk: settings.isFeatureEnabled('SkippingIsOK'),
    };
  }, []);
  const canSkip = !!displaySkip && skippingOk;

  // Keyboard shortcuts. Keep the listener stable but always call the latest handler (current props).
  const handlerRef = useRef<(e: KeyboardEvent) => void>(() => {});
  handlerRef.current = (e: KeyboardEvent) => {
    const target = e.target as Element;
    if (
      target.matches?.('.lo-rich-text-editor') ||
      target.matches?.('.fill-blank-blank input') ||
      target.closest?.('.modal')
    ) {
      e.stopPropagation();
      return;
    }

    trackQuizKeyboard(e.which);

    if (e.which === 13) {
      if (canSubmitQuestion) {
        submitQuestion();
      } else if (canSubmitQuiz) {
        setShowSubmitWarning(true);
      }
      return;
    }

    const input = +String.fromCharCode(e.which);

    if (!canEditAnswer && input === 1) {
      nextQuestion();
    } else if (!displayConfidenceIndicators && input === 1) {
      if (canSubmitQuestion) submitQuestion();
    } else if (displayConfidenceIndicators && confidenceButtons[input - 1]) {
      if (canSubmitQuestion) submitQuestion(confidenceButtons[input - 1].value);
    } else if (input === 4 && canSkip) {
      skipQuestion();
    }
  };
  useEffect(() => {
    const listener = (e: KeyboardEvent) => handlerRef.current(e);
    document.addEventListener('keydown', listener);
    return () => document.removeEventListener('keydown', listener);
  }, []);

  return (
    <div className="mp-player-controls">
      {displayConfidenceIndicators && canEditAnswer && (
        <div className="mp-player-control-btns">
          <span className="sr-only">{translate('Question Answer Confidence Buttons')}</span>
          {confidenceButtons.map((buttonConfig, i) => (
            <button
              key={i}
              className={classNames('btn', {
                'btn-primary': selectedConfidence === buttonConfig.value,
                'btn-outline-primary': selectedConfidence !== buttonConfig.value,
              })}
              disabled={!canSubmitQuestion}
              onClick={() => submitQuestion(buttonConfig.value)}
            >
              {translate(buttonConfig.label)}
            </button>
          ))}
        </div>
      )}

      {!displayConfidenceIndicators && canEditAnswer && (
        <div className="mp-player-control-btns">
          <button
            className="btn btn-primary submit-question"
            disabled={!canSubmitQuestion}
            onClick={() => submitQuestion()}
          >
            {!isSubmitting ? <span>{translate('QUIZ_MP_SUBMIT_QUESTION')}</span> : <LoadingSpinner />}
          </button>
        </div>
      )}

      {canSkip && canEditAnswer && (
        <div className="mp-player-control-btns">
          <button
            className="btn btn-light skip-question"
            onClick={() => skipQuestion()}
          >
            {translate('QUIZ_MP_SKIP_QUESTION')}
          </button>
        </div>
      )}

      {!canEditAnswer && !isLastQuestion && (
        <div className="mp-player-control-btns">
          <button
            className="btn btn-primary next-question"
            onClick={() => nextQuestion()}
          >
            {translate('QUIZ_MP_NEXT_QUESTION')}
          </button>
        </div>
      )}

      {isLastQuestion && (
        <div className="mp-player-control-btns">
          {canSubmitQuiz && (
            <QuizSubmitButton
              canSubmit={canSubmitQuiz}
              unansweredQuestions={unansweredQuestions}
              onAttempt={onAttempt}
              maxAttempts={maxAttempts}
              submitQuiz={submitQuiz}
              showWarning={showSubmitWarning}
              enableAutofocus={true}
              isCheckpoint={isCheckpoint}
            />
          )}
        </div>
      )}

      {warnUnsaved && hasUnsavedChanges && (
        <div className="unsaved-warning alert alert-warning">
          <i className="icon-warning" />
          <span>{translate('AssessmentSaveWarningMsg')}</span>
        </div>
      )}
    </div>
  );
};

export default MpControls;
