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

import { loadQuizActivityActionCreator } from '../../courseActivityModule/actions/quizActivityActions.js';
import { errorMessage } from '../../filters/pure/errorMessage.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';
import navBlockerService from '../../services/navBlockerService.ts';
import { useCourseSelector } from '../../loRedux';
import {
  autosaveQuizActionCreatorMaker,
  changeQuestionAnswerActionCreatorMaker,
  saveQuizActionCreatorMaker,
  submitQuizActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerActions.js';
import { singlePagePlayerSelectorCreator } from '../../quizPlayerModule/selectors/singlePagePlayerSelectors.js';
import { QuestionView } from '../../quizQuestions/QuestionView.tsx';
import { selectCurrentUser } from '../../utilities/rootSelectors.js';
import { QuizSubmitButton } from '../playerAddons/QuizSubmitButton.tsx';
import { useQuizAutosave } from '../playerAddons/useQuizAutosave.ts';

interface SinglePagePlayerProps {
  assessment: any;
  attemptId: string;
  onAttempt?: number;
  printView?: boolean;
}

/**
 * React port of the `singlePagePlayer` component (quizViews/players): the all-questions-on-one-page quiz
 * player. Connects the `singlePagePlayerSelectorCreator` redux state and dispatches the player actions
 * (change/save/autosave/submit/discard); each question renders through the React `QuestionView`
 * dispatcher (`canEditAnswer=true`). Autosave runs through the native `useQuizAutosave` hook and the
 * submit button is the native React `QuizSubmitButton`. Rendered directly by the React
 * `ContentQuizPlayerLoader` (which supplies the redux/query/i18n providers).
 *
 * DOM preserved for SinglePageQuizSubmissionTest: `.sp-quiz-player`, `ul.sp-quiz-player-questions > li`,
 * `#page-question-{index}`, `.quiz-question`, `.sp-quiz-player-controls`, `.quiz-submit-button`. The dead
 * injected `$timeout` and the always-true `allowSaving` flag are dropped.
 */
export const SinglePagePlayer: React.FC<SinglePagePlayerProps> = ({ assessment, attemptId, onAttempt }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const selector = useMemo(
    () => singlePagePlayerSelectorCreator(assessment, attemptId),
    [assessment, attemptId]
  );
  const {
    questionTuples = [],
    allUnsavedChanges,
    anyQuestionHasUnsavedChanges,
    lastChangeTimestamp,
    lastSaveTimestamp,
    settings = {},
    quizSubmissionState = {},
    unansweredQuestions = [],
    canSaveOrSubmitQuiz,
  } = useCourseSelector(selector) as any;

  const viewingAs = useCourseSelector(selectCurrentUser) as any;

  const changeAnswer = useMemo(() => changeQuestionAnswerActionCreatorMaker(attemptId), [attemptId]);
  const saveQuizAction = useMemo(
    () => saveQuizActionCreatorMaker(attemptId, assessment),
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

  const onChangeAnswer = (index: number, response: any) => dispatch(changeAnswer(index, response));
  const saveQuiz = () => dispatch(saveQuizAction(allUnsavedChanges));
  const submitQuiz = () => dispatch(submitQuizAction(allUnsavedChanges));
  const autosaveQuiz = () => dispatch(autosaveQuizAction(allUnsavedChanges));
  const discardQuiz = () => dispatch(loadQuizActivityActionCreator(assessment, viewingAs, viewingAs.id));

  // Nav blocker: warn before leaving with unsaved changes. The condition is read live (it changes as the
  // learner edits), so close over a ref rather than the render-time value.
  const unsavedRef = useRef(anyQuestionHasUnsavedChanges);
  unsavedRef.current = anyQuestionHasUnsavedChanges;
  useEffect(() => {
    return navBlockerService.register(() => unsavedRef.current, 'QUIZ_CONFIRM_MOVE_UNSAVED_CHANGES');
  }, []);

  useQuizAutosave({
    hasChanges: anyQuestionHasUnsavedChanges,
    save: autosaveQuiz,
    lastChange: lastChangeTimestamp,
    lastSave: lastSaveTimestamp,
  });

  return (
    <div className="sp-quiz-player">
      <ul className="sp-quiz-player-questions list-unstyled">
        {questionTuples.map((tuple: any, i: number) => (
          <li key={tuple.index}>
            <div
              className="sp-question-control-container"
              id={`page-question-${tuple.index}`}
            >
              <div className="quiz-question">
                <QuestionView
                  index={i}
                  question={tuple.question}
                  response={tuple.response}
                  assessment={assessment}
                  questionCount={questionTuples.length}
                  changeAnswer={onChangeAnswer}
                  canEditAnswer={true}
                />
              </div>
            </div>
            <hr className="question-separator" />
          </li>
        ))}
      </ul>

      <div className="sp-quiz-player-controls d-print-none">
        <div className="flex-row-content justify-content-md-end flex-wrap justify-content-center">
          <button
            className="btn btn-outline-secondary"
            disabled={!canSaveOrSubmitQuiz}
            onClick={saveQuiz}
          >
            <span>{translate('QUIZ_PLAYER_SAVE_EXIT')}</span>
          </button>

          <QuizSubmitButton
            canSubmit={canSaveOrSubmitQuiz}
            unansweredQuestions={unansweredQuestions}
            onAttempt={onAttempt}
            maxAttempts={settings.maxAttempts}
            submitQuiz={submitQuiz}
            isCheckpoint={settings.isCheckpoint}
          />
        </div>

        {quizSubmissionState.loading && (
          <div className="alert alert-info mt-3 mb-0">
            {translate('QUIZ_PLAYER_SAVE_EXIT_IN_PROGRESS')}
          </div>
        )}

        {!quizSubmissionState.loading && quizSubmissionState.error && (
          <div className="alert alert-danger mt-3 mb-0 py-2 d-flex align-items-center justify-content-between">
            <div>{errorMessage(quizSubmissionState.error, translate)}</div>
            <button
              className="btn btn-sm btn-outline-danger"
              onClick={discardQuiz}
            >
              <span>{translate('QUIZ_PLAYER_DISCARD_ANSWERS')}</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default SinglePagePlayer;
