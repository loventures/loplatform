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

import { ContentQuizResultsLoader } from '../../../../contentPlayerDirectives/quizLoaders/contentQuizResultsLoader.js';
import Tutorial from '../../../../tutorial/Tutorial.js';
import { useTranslation } from '../../../../i18n/translationContext.js';
import {
  ATTEMPT_FINALIZED,
  LEGACY_ATTEMPT_COMPLETED,
} from '../../../../utilities/attemptStates.js';
import { GoClockFill } from 'react-icons/go';
import { Alert } from 'reactstrap';

import QuizAttemptScorePanel from '../parts/QuizAttemptScorePanel.jsx';

const QuizActivityResultsAttempt = ({
  course,
  content,
  viewingAs,
  printView,
  quiz,
  attempt,
  playAttempt,
  isSoftLimitActive,
  hasGrade = attempt &&
    (attempt.state === LEGACY_ATTEMPT_COMPLETED || attempt.state === ATTEMPT_FINALIZED),
  showScorePanel = hasGrade && !quiz.assessment.settings.isCheckpoint,
}) => {
  const translate = useTranslation();
  // const maxMinutes = quiz.assessment.settings.maxMinutes;
  // const exceeded = Math.ceil(-(attempt.remainingMillis ?? 0) / 60000);
  // const overdue = !!maxMinutes && exceeded > 0;
  return (
    <div className="quiz-results-with-score-panel">
      {attempt.autoSubmitted && (
        <Alert
          color="danger"
          className="mt-2"
        >
          <GoClockFill className="me-2 mb-1" />
          {translate('WARN_AUTO_SUBMITTED')}
        </Alert>
      )}

      {showScorePanel ? (
        <QuizAttemptScorePanel
          course={course}
          content={content}
          quiz={quiz}
          viewingAs={viewingAs}
          printView={printView}
          attempt={attempt}
          playAttempt={playAttempt}
          isSoftLimitActive={isSoftLimitActive}
        />
      ) : (
        <div className="pt-4"></div>
      )}

      {/*overdue && (
        <Alert color="danger">
          {translate('WARN_LATE_SUBMISSION', { exceeded, maxMinutes })}{' '}
          {hasGrade ? '' : translate('WARN_MANUAL_GRADING')}
        </Alert>
      )*/}

      <div className="results-question-list">
        <ContentQuizResultsLoader
          key={attempt.id}
          assessment={quiz.assessment}
          attempt={attempt}
          printView={printView}
        />
      </div>

      {quiz.assessment.settings.singlePage &&
        !quiz.assessment.settings.isCheckpoint &&
        !printView && <Tutorial name="assessment.1-results" />}
    </div>
  );
};

export default QuizActivityResultsAttempt;
