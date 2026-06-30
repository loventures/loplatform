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

import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';

import { TranslationProvider } from '../../i18n/translationContext.tsx';
import { enterQuizPlayerActionCreatorMaker } from '../../quizPlayerModule/actions/quizPlayerActions.js';
import { quizLoadedActionCreatorMaker } from '../../quizPlayerModule/actions/quizPlayerLoadActions.js';
import { QuizResults } from '../../quizViews/players/QuizResults.tsx';
import { QueryClientProvider, queryClient } from '../../resources/queryClient';
import { withNgReduxProvider } from '../../utilities/ngReduxProvider.jsx';

interface ContentQuizResultsLoaderProps {
  assessment: any;
  attempt: any;
  useProjectResults?: boolean;
  printView?: boolean;
}

/**
 * React port of the `contentQuizResultsLoader` directive (was an angular2react bridge). Initializes the
 * quiz-player redux state for the attempt and renders the React `QuizResults`. Now a plain React
 * component — its only consumer (`QuizActivityResultsAttempt`) is React — wrapped in the redux/query/i18n
 * providers the React quiz views need (the players' former react2angular boundary supplied these).
 */
const ContentQuizResultsLoaderInner: React.FC<ContentQuizResultsLoaderProps> = ({
  assessment,
  attempt,
  useProjectResults,
  printView,
}) => {
  const dispatch = useDispatch();

  useEffect(() => {
    // Hand the assessment to redux (so results questionTuples get their question detail, needed to
    // map responses to choices in print), then init the player state for the attempt.
    dispatch(quizLoadedActionCreatorMaker()(assessment));
    dispatch(enterQuizPlayerActionCreatorMaker(attempt.id)());
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attempt.id]);

  return <div>{!useProjectResults && <QuizResults assessment={assessment} attemptId={attempt.id} printView={printView} />}</div>;
};

export const ContentQuizResultsLoader = withNgReduxProvider((props: ContentQuizResultsLoaderProps) => (
  <QueryClientProvider client={queryClient}>
    <TranslationProvider>
      <ContentQuizResultsLoaderInner {...props} />
    </TranslationProvider>
  </QueryClientProvider>
));

export default ContentQuizResultsLoader;
