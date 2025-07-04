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

import { ContentQuizPlayerLoader } from '../../../../contentPlayerDirectives/quizLoaders/contentQuizPlayerLoader.js';
import { QuizInfoBar } from './QuizInfoBar.js';
import {
  quizActivityAfterSaveActionCreator,
  quizActivityAfterSubmitActionCreator,
} from '../../../../courseActivityModule/actions/quizActivityActions.js';
import { connect } from 'react-redux';

import QuizActivityOpenAttemptLoader from '../loaders/QuizActivityOpenAttemptLoader.js';

const QuizActivityPlayAttempt = ({
  content,
  viewingAs,
  printView,
  quiz,
  competencies,
  onAttempt = quiz.orderedAttempts.length + 1,
  onSubmit,
  onSave,
  instructions = quiz.assessment.instructions,
}) => (
  <QuizActivityOpenAttemptLoader
    content={content}
    quiz={quiz}
    competencies={competencies}
    viewingAs={viewingAs}
  >
    <QuizInfoBar
      attempt={quiz.openAttempt}
      instructions={instructions}
    />

    <div className="pb-4" />

    <ContentQuizPlayerLoader
      assessment={quiz.assessment}
      attempt={quiz.openAttempt}
      onAttempt={onAttempt}
      onSave={() => onSave(content, quiz, viewingAs, viewingAs.id)}
      onSubmit={() => onSubmit(content, quiz, viewingAs, viewingAs.id)}
      printView={printView}
    />
  </QuizActivityOpenAttemptLoader>
);

export default connect(null, {
  onSubmit: quizActivityAfterSubmitActionCreator,
  onSave: quizActivityAfterSaveActionCreator,
})(QuizActivityPlayAttempt);
