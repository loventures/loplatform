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

import { ScrollToTopContext } from '../../../landmarks/ScrollToTopProvider.tsx';
import Tutorial from '../../../tutorial/Tutorial.tsx';
import { selectQuizActivityComponent } from '../../../courseActivityModule/selectors/quizActivitySelectors.ts';
import React, { useContext, useEffect, useState } from 'react';
import { connect } from 'react-redux';

import InstructorNotifications from '../../../components/InstructorNotifications.tsx';
import QuizActivityPlayAttempt from './views/QuizActivityPlayAttempt';
import QuizActivityPlayInstructions from './views/QuizActivityPlayInstructions.tsx';
import QuizActivityPreviewUnsubmitted from './views/QuizActivityPreviewUnsubmitted';

const QuizActivityLearnerPlay: React.FC<any> = ({ content, quiz, viewingAs, printView }) => {
  const [playing, startPlaying] = useState<{ topics?: string[] } | undefined>();
  // playing is the boolean we care about for QNA button. TODO: put this in redux?
  const scrollToTop = useContext(ScrollToTopContext);
  useEffect(() => scrollToTop(), [scrollToTop, playing, viewingAs.isPreviewing]);
  /* If we display the quiz in print view then that would probably create a new attempt
   * in the database (in order, for example, to select pool questions) which would be bad.
   * But creating a print view as an instructor/admin for a student would give them all
   * the answers. */
  return (
    <div>
      {!playing ? <InstructorNotifications contentId={content.id} /> : null}

      {viewingAs.isPreviewing && <QuizActivityPreviewUnsubmitted quiz={quiz} />}

      {!playing && !viewingAs.isPreviewing && (
        <>
          <QuizActivityPlayInstructions
            content={content}
            quiz={quiz}
            viewingAs={viewingAs}
            printView={printView}
            startPlaying={startPlaying}
          />
          {quiz.assessment.settings.singlePage &&
            !quiz.assessment.settings.isCheckpoint &&
            !printView && <Tutorial name="assessment.1-instructions" />}
        </>
      )}

      {(playing || (printView && quiz.openAttempt)) && (
        <>
          {printView && <div className="mb-4" />}
          <QuizActivityPlayAttempt
            content={content}
            quiz={quiz}
            competencies={playing?.topics}
            viewingAs={viewingAs}
            printView={printView}
          />
          {quiz.assessment.settings.singlePage &&
            !quiz.assessment.settings.isCheckpoint &&
            !printView && <Tutorial name="assessment.1-questions" />}
        </>
      )}
    </div>
  );
};

export default connect(selectQuizActivityComponent)(QuizActivityLearnerPlay);
