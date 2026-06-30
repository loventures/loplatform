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

import React, { useEffect, useMemo, useRef } from 'react';
import { useDispatch } from 'react-redux';

import LoadingSpinner from '../../directives/loadingSpinner';
import { errorMessage } from '../../filters/pure/errorMessage.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';
import navBlockerService from '../../services/navBlockerService.ts';
import { useCourseSelector } from '../../loRedux';
import {
  autosaveQuizActionCreatorMaker,
  changeQuestionAnswerActionCreatorMaker,
  gotoQuestionActionCreatorMaker,
  skipQuestionActionCreatorMaker,
  submitQuestionActionCreatorMaker,
  submitQuizActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerActions.js';
import { multiPagePlayerSelectorCreator } from '../../quizPlayerModule/selectors/multiPagePlayerSelectors.js';
import { QuestionView } from '../../quizQuestions/QuestionView.tsx';
import { MpControls } from '../playerAddons/MpControls.tsx';
import { MpQuestionNav } from '../playerAddons/MpQuestionNav.tsx';
import { useQuizAutosave } from '../playerAddons/useQuizAutosave.ts';

interface MultiPagePlayerProps {
  assessment: any;
  attemptId: string;
  onAttempt?: number;
  printView?: boolean;
}

/**
 * React port of the `multiPagePlayer` component (quizViews/players): the one-question-per-page quiz
 * player — a paginated navigation state machine. Connects `multiPagePlayerSelectorCreator` and dispatches
 * the player actions (change / goto / skip / submitQuestion / submitQuiz / autosave). The current
 * question renders through the React `QuestionView` dispatcher, keyed by `currentQuestionIndex` so it
 * fully remounts on navigation (the old controller cleared `currentQuestion` + `$timeout`'d the goto to
 * force a complete question-loader re-render — the key does that natively). The question-nav strip
 * (`MpQuestionNav`), page controls (`MpControls`, keyboard bindings + confidence/skip/submit), and the
 * autosave (`useQuizAutosave`) are all native React now. Rendered directly by the React
 * `ContentQuizPlayerLoader` (which supplies the redux/query/i18n providers).
 *
 * DOM preserved for MultiPageQuizNavTest / QuizKeyboardNavTest: `.mp-quiz-player`, `.quiz-page`,
 * `.quiz-page-question`, `.quiz-page-controls` (+ the sub-components' internal DOM, unchanged).
 */
export const MultiPagePlayer: React.FC<MultiPagePlayerProps> = ({
  assessment,
  attemptId,
  onAttempt,
  printView,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const selector = useMemo(
    () => multiPagePlayerSelectorCreator(assessment, attemptId),
    [assessment, attemptId]
  );
  const {
    questionTuples = [],
    currentQuestion,
    currentQuestionIndex,
    currentQuestionResponse,
    currentQuestionSavedResponse,
    currentQuestionScore,
    currentQuestionHasUnsavedChanges,
    currentQuestionAnswered,
    canGoStatus,
    indexToGoAfter,
    indexToGoAfterSkip,
    canSubmitCurrentQuestion,
    canSaveOrSubmitQuiz,
    unansweredQuestions = [],
    allUnsavedChanges,
    isLastQuestion,
    shouldDisplaySkip,
    settings = {},
    questionSubmissionState = {},
    quizSubmissionState = {},
    lastChangeTimestamp,
    lastSaveTimestamp,
    enableAutosave,
  } = useCourseSelector(selector) as any;

  const changeAnswer = useMemo(() => changeQuestionAnswerActionCreatorMaker(attemptId), [attemptId]);
  const gotoQuestionAction = useMemo(() => gotoQuestionActionCreatorMaker(attemptId), [attemptId]);
  const skipQuestionAction = useMemo(
    () => skipQuestionActionCreatorMaker(attemptId, assessment),
    [attemptId, assessment]
  );
  const submitQuestionAction = useMemo(
    () => submitQuestionActionCreatorMaker(attemptId, assessment),
    [attemptId, assessment]
  );
  const autosaveQuizAction = useMemo(
    () => autosaveQuizActionCreatorMaker(attemptId, assessment),
    [attemptId, assessment]
  );
  const submitQuizAction = useMemo(
    () => submitQuizActionCreatorMaker(attemptId, assessment),
    [attemptId, assessment]
  );

  // Nav blocker (warn before leaving with unsaved changes) + the confirm-discard gate used on navigation.
  const unsavedRef = useRef(currentQuestionHasUnsavedChanges);
  unsavedRef.current = currentQuestionHasUnsavedChanges;
  useEffect(() => {
    return navBlockerService.register(() => unsavedRef.current, 'QUIZ_CONFIRM_MOVE_UNSAVED_CHANGES');
  }, []);
  const confirmDiscard = () =>
    navBlockerService.confirmNavByModal(['QUIZ_CONFIRM_MOVE_UNSAVED_CHANGES']);

  const onChangeAnswer = (index: number, response: any) => dispatch(changeAnswer(index, response));

  const gotoQuestion = (toIndex: number) => {
    if (canGoStatus && !canGoStatus[toIndex]) return;
    confirmDiscard().then(() => dispatch(gotoQuestionAction(toIndex)));
  };

  const nextQuestion = () => gotoQuestion(indexToGoAfterSkip);

  const skipQuestion = () => {
    const skipResponse = { ...currentQuestionSavedResponse };
    skipResponse.selection.skip = true;
    dispatch(skipQuestionAction(currentQuestionIndex, skipResponse, indexToGoAfterSkip));
  };

  const submitQuestion = (confidenceValue?: number) => {
    const response = { ...currentQuestionResponse };
    if (response.selection) {
      response.selection.confidence = confidenceValue;
      response.selection.skip = false;
    }
    dispatch(submitQuestionAction(currentQuestionIndex, response, indexToGoAfter));
  };

  const submitQuiz = () => dispatch(submitQuizAction(allUnsavedChanges));
  const autosaveQuiz = () => dispatch(autosaveQuizAction(allUnsavedChanges, currentQuestionIndex));

  useQuizAutosave({
    enabled: enableAutosave,
    hasChanges: currentQuestionHasUnsavedChanges,
    save: autosaveQuiz,
    lastChange: lastChangeTimestamp,
    lastSave: lastSaveTimestamp,
  });

  const hasError =
    (!questionSubmissionState.loading && questionSubmissionState.error) ||
    (!quizSubmissionState.loading && quizSubmissionState.error);
  const errorText = () =>
    errorMessage(quizSubmissionState.error || questionSubmissionState.error, translate);

  return (
    <div className="mp-quiz-player">
      {!printView && (
        <MpQuestionNav
          questionTuples={questionTuples}
          currentIndex={currentQuestionIndex}
          goto={gotoQuestion}
          canGoStatus={canGoStatus}
        />
      )}

      {!currentQuestion && (
        <div className="alert alert-info my-2">
          <LoadingSpinner />
        </div>
      )}

      {hasError && (
        <div className="alert alert-danger my-2">
          <span>{errorText()}</span>
        </div>
      )}

      <div className="quiz-page">
        {currentQuestion && (
          <div className="quiz-page-question">
            <QuestionView
              key={currentQuestionIndex}
              index={currentQuestionIndex}
              focusOnRender={true}
              question={currentQuestion}
              response={currentQuestionResponse}
              assessment={assessment}
              questionCount={questionTuples.length}
              changeAnswer={onChangeAnswer}
              canEditAnswer={!currentQuestionScore}
            />
          </div>
        )}

        {!printView && (
          <div className="quiz-page-controls">
            <MpControls
              displayConfidenceIndicators={settings.displayConfidenceIndicators}
              displaySkip={shouldDisplaySkip}
              selectedConfidence={currentQuestionResponse?.selection?.confidence}
              hasUnsavedChanges={currentQuestionHasUnsavedChanges}
              isLastQuestion={isLastQuestion}
              unansweredQuestions={unansweredQuestions}
              onAttempt={onAttempt}
              maxAttempts={settings.maxAttempts}
              canEditAnswer={!currentQuestionScore}
              nextQuestion={nextQuestion}
              canSubmitQuestion={canSubmitCurrentQuestion}
              submitQuestion={submitQuestion}
              canSubmitQuiz={
                canSaveOrSubmitQuiz && !currentQuestionHasUnsavedChanges && currentQuestionAnswered
              }
              submitQuiz={submitQuiz}
              skipQuestion={skipQuestion}
              isSubmitting={questionSubmissionState.loading}
              isCheckpoint={settings.isCheckpoint}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default MultiPagePlayer;
