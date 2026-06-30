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

import React, { useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';

import { TranslationProvider, useTranslation } from '../../i18n/translationContext.tsx';
import { useCourseSelector } from '../../loRedux';
import {
  enterQuizPlayerActionCreatorMaker,
  resetSaveStatusActionCreatorMaker,
  resetSubmitStatusActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerActions.js';
import { quizLoadedActionCreatorMaker } from '../../quizPlayerModule/actions/quizPlayerLoadActions.js';
import { MultiPagePlayer } from '../../quizViews/players/MultiPagePlayer.tsx';
import { SinglePagePlayer } from '../../quizViews/players/SinglePagePlayer.tsx';
import { QueryClientProvider, queryClient } from '../../resources/queryClient';
import { withNgReduxProvider } from '../../utilities/ngReduxProvider.jsx';
import { contentQuizLoaderSelectorCreator } from './contentQuizLoaderSelector.js';

interface ContentQuizPlayerLoaderProps {
  assessment: any;
  attempt: any;
  onAttempt?: number;
  onSave?: () => void;
  onSubmit?: () => void;
  printView?: boolean;
}

/**
 * React port of the `contentQuizPlayerLoader` directive (was an angular2react bridge). Initializes the
 * quiz-player redux state for the open attempt and renders the React single- or multi-page player, or —
 * once the save/submit completes — a "saved"/"submitted" notice (resetting that status and calling the
 * `onSave`/`onSubmit` callback, as the old `ng-init` saved()/submitted() did). Now a plain React
 * component (only consumer `QuizActivityPlayAttempt` is React), wrapped in the redux/query/i18n providers
 * the React players need.
 */
const ContentQuizPlayerLoaderInner: React.FC<ContentQuizPlayerLoaderProps> = ({
  assessment,
  attempt,
  onAttempt,
  onSave,
  onSubmit,
  printView,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const selector = useMemo(() => contentQuizLoaderSelectorCreator(attempt.id), [attempt.id]);
  const { attemptSubmissionState = {}, attemptSaveState = {} } = useCourseSelector(selector) as any;

  useEffect(() => {
    // We know the assessment exists from the node; hand it to redux, then init the player state.
    dispatch(quizLoadedActionCreatorMaker()(assessment));
    dispatch(enterQuizPlayerActionCreatorMaker(attempt.id)());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attempt.id]);

  // On submit/save completing: reset that status and notify the parent (deferred, as the old $timeout).
  useEffect(() => {
    if (attemptSubmissionState.loaded) {
      dispatch(resetSubmitStatusActionCreatorMaker(attempt.id)());
      setTimeout(() => onSubmit?.(), 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attemptSubmissionState.loaded]);

  useEffect(() => {
    if (attemptSaveState.loaded) {
      dispatch(resetSaveStatusActionCreatorMaker(attempt.id)());
      setTimeout(() => onSave?.(), 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attemptSaveState.loaded]);

  const settings = assessment.settings;
  const singlePage =
    settings.pagingPolicy === 'ASSESSMENT_AT_A_TIME' ||
    (!!printView &&
      settings.navigationPolicy?.backtrackingAllowed &&
      settings.navigationPolicy?.skippingAllowed);

  const hasQuestions = attempt.questions && attempt.questions.length > 0;
  const showPlayer = !attemptSubmissionState.loaded && !attemptSaveState.loaded && hasQuestions;

  return (
    <div>
      {!hasQuestions && (
        <div className="alert alert-warning">
          <span>{translate('QUIZ_CONTAINS_NO_QUESTIONS')}</span>
        </div>
      )}

      {showPlayer &&
        (singlePage ? (
          <SinglePagePlayer
            assessment={assessment}
            attemptId={attempt.id}
            onAttempt={onAttempt}
            printView={printView}
          />
        ) : (
          <MultiPagePlayer
            assessment={assessment}
            attemptId={attempt.id}
            onAttempt={onAttempt}
            printView={printView}
          />
        ))}

      {attemptSubmissionState.loaded && (
        <div className="alert alert-success mb-0">
          <span>{translate('ASSESSMENT_PLAYING_SUBMITTED')}</span>
        </div>
      )}
      {attemptSaveState.loaded && (
        <div className="alert alert-success mb-0">
          <span>{translate('ASSESSMENT_PLAYING_SAVED')}</span>
        </div>
      )}
    </div>
  );
};

export const ContentQuizPlayerLoader = withNgReduxProvider((props: ContentQuizPlayerLoaderProps) => (
  <QueryClientProvider client={queryClient}>
    <TranslationProvider>
      <ContentQuizPlayerLoaderInner {...props} />
    </TranslationProvider>
  </QueryClientProvider>
));

export default ContentQuizPlayerLoader;
