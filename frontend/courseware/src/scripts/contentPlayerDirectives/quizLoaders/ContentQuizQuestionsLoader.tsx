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
import { TranslationProvider } from '../../i18n/translationContext.tsx';
import { useCourseSelector } from '../../loRedux';
import {
  loadQuizQuestionsActionCreatorMaker,
  quizLoadedActionCreatorMaker,
} from '../../quizPlayerModule/actions/quizPlayerLoadActions.js';
import { QuizResults } from '../../quizViews/players/QuizResults.tsx';
import { QueryClientProvider, queryClient } from '../../resources/queryClient';
import { withNgReduxProvider } from '../../utilities/ngReduxProvider.jsx';
import { contentQuizQuestionLoaderSelectorCreator } from './contentQuizLoaderSelector.js';

interface ContentQuizQuestionsLoaderProps {
  assessment: any;
  useProjectResults?: boolean;
  printView?: boolean;
}

/**
 * React port of the `contentQuizQuestionsLoader` directive (was an angular2react bridge). Loads the
 * assessment's questions (the instructor "all questions" view, no attempt) and renders the React
 * `QuizResults` once they're loaded. Now a plain React component (only consumer `QuizActivityInstructor`
 * is React), wrapped in the redux/query/i18n providers the React quiz views need.
 */
const ContentQuizQuestionsLoaderInner: React.FC<ContentQuizQuestionsLoaderProps> = ({
  assessment,
  useProjectResults,
  printView,
}) => {
  const dispatch = useDispatch();
  const selector = useMemo(() => contentQuizQuestionLoaderSelectorCreator(assessment), [assessment]);
  const { quizAssessmentQuestionsState = {} } = useCourseSelector(selector) as any;

  const inited = useRef(false);
  useEffect(() => {
    if (inited.current) return;
    inited.current = true;
    dispatch(quizLoadedActionCreatorMaker()(assessment));
    if (!quizAssessmentQuestionsState.loaded && !quizAssessmentQuestionsState.loading) {
      dispatch(loadQuizQuestionsActionCreatorMaker()(assessment));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <>
      {quizAssessmentQuestionsState.loading && (
        <div className="alert alert-info">
          <LoadingSpinner />
        </div>
      )}
      {quizAssessmentQuestionsState.loaded && !useProjectResults && (
        <QuizResults assessment={assessment} printView={printView} />
      )}
    </>
  );
};

export const ContentQuizQuestionsLoader = withNgReduxProvider((props: ContentQuizQuestionsLoaderProps) => (
  <QueryClientProvider client={queryClient}>
    <TranslationProvider>
      <ContentQuizQuestionsLoaderInner {...props} />
    </TranslationProvider>
  </QueryClientProvider>
));

export default ContentQuizQuestionsLoader;
