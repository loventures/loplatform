/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

import { ContentQuizQuestionsLoader } from '../../../contentPlayerDirectives/quizLoaders/contentQuizQuestionsLoader.js';
import QuizActivityInfo from './parts/QuizActivityInfo.js';
import ContentInstructions from '../../parts/ContentInstructions.jsx';
import { OnLoader } from '../../utils/OnLoader.js';
import { loadQuizActivityActionCreator } from '../../../courseActivityModule/actions/quizActivityActions.js';
import {
  selectContentActivityLoaderComponent,
  selectQuizActivityComponent,
} from '../../../courseActivityModule/selectors/quizActivitySelectors.js';
import { createLoaderComponent } from '../../../utilities/withLoader.js';
import React from 'react';
import { connect } from 'react-redux';

export const QuizActivityInstructorLoader = createLoaderComponent(
  selectContentActivityLoaderComponent,
  ({ content, viewingAs }) => loadQuizActivityActionCreator(content, viewingAs, viewingAs.id),
  'QuizActivityInstructor'
);

const QuizActivityInstructor = ({ content, viewingAs, printView, quiz, onLoaded }) => (
  <QuizActivityInstructorLoader
    content={content}
    viewingAs={viewingAs}
    printView={printView}
  >
    <OnLoader onLoaded={onLoaded} />
    <ContentInstructions instructions={quiz.assessment.instructions} />

    {!quiz.assessment.settings?.isCheckpoint && (
      <QuizActivityInfo
        content={content}
        quiz={quiz}
        noAttemptNumber
      />
    )}

    <ContentQuizQuestionsLoader
      assessment={quiz.assessment}
      printView={printView}
    />
  </QuizActivityInstructorLoader>
);

export default connect(selectQuizActivityComponent)(QuizActivityInstructor);
