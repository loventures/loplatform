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

import React, { useMemo } from 'react';

import { useCourseSelector } from '../../loRedux';
import {
  quizResultsSelectorCreator,
  quizViewQuestionsSelectorCreator,
} from '../../quizPlayerModule/selectors/quizResultsSelectors.js';
import { QuestionView } from '../../quizQuestions/QuestionView.tsx';

interface QuestionTuple {
  index: number;
  question: any;
  response?: any;
}

interface QuizResultsProps {
  assessment: any;
  attemptId?: string;
  printView?: boolean;
}

/**
 * React port of the `quizResults` component (quizViews/players): the read-only review list of a quiz's
 * questions + responses. Each question renders through the React `QuestionView` dispatcher
 * (`canEditAnswer=false`); the still-Angular essay type view + print templates fall back to the Angular
 * question loader inside `QuestionView`. Native React — rendered directly by the React content-quiz
 * {results,questions} loaders; the redux selectors are reused verbatim.
 *
 * DOM preserved for QuizResultsPage Selenide: `ul#quiz-results-questions-anchor > li`,
 * `hr.question-separator`. The original `pageSize = 16384` paginator + Scroller were inert (a quiz never
 * has that many questions, and the sole `gotoPage` call passed `ignoreScrollTo`) and are dropped.
 */
export const QuizResults: React.FC<QuizResultsProps> = ({ assessment, attemptId }) => {
  const selector = useMemo(
    () =>
      attemptId
        ? quizResultsSelectorCreator(assessment, attemptId)
        : quizViewQuestionsSelectorCreator(assessment),
    [assessment, attemptId]
  );
  const { questionTuples = [] } = useCourseSelector(selector) as { questionTuples?: QuestionTuple[] };

  return (
    <ul
      className="list-group list-unstyled"
      id="quiz-results-questions-anchor"
    >
      {questionTuples.map(tuple => (
        <li key={tuple.index}>
          <QuestionView
            index={tuple.index}
            question={tuple.question}
            response={tuple.response}
            assessment={assessment}
            questionCount={questionTuples.length}
            canEditAnswer={false}
          />
          <hr className="question-separator" />
        </li>
      ))}
    </ul>
  );
};

export default QuizResults;
