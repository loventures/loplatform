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

import { QuizAttempt } from '../../../api/quizApi.ts';
import { Tab, TabList, TabPanel, Tabs } from '../../../components/tabs';
import { isEmpty, map } from 'lodash';
import QuizActivityInfo, { QuizActivity } from './parts/QuizActivityInfo.tsx';
import QuizTestOut from './QuizTestOut.tsx';
import ContentInstructions from '../../parts/ContentInstructions';
import { selectQuizActivityComponent } from '../../../courseActivityModule/selectors/quizActivitySelectors.ts';
import { ContentWithNebulousDetails } from '../../../courseContentModule/selectors/contentEntry';
import {
  QUIZ_ACTIVITY_RESULTS_ATTEMPT,
  QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS,
} from '../../../utilities/activityStates';
import { CourseWithDetails, UserWithRoleInfo } from '../../../utilities/rootSelectors.ts';
import React, { useState } from 'react';
import { connect } from 'react-redux';
import { compose } from 'recompose';

import InstructorNotifications from '../../../components/InstructorNotifications.tsx';
import InstructionsTab from './parts/attemptHistoryTabs/InstructionsTab';
import NewAttemptTab from './parts/attemptHistoryTabs/NewAttemptTab';
import ViewAttemptTab from './parts/attemptHistoryTabs/ViewAttemptTab';
import QuizSoftLimitMessage from './parts/QuizSoftLimitMessage';
import QuizActivityResultsAttempt from './views/QuizActivityResultsAttempt';
import QuizActivitySummary from './views/QuizActivitySummary';

const tabClassNames = 'attempt-history-item list-group-item list-group-item-action vertical-line';
const tabActiveClassNames = 'active-item ';

type QuizActivityLearnerResultsProps = {
  content: ContentWithNebulousDetails;
  printView: boolean;
  isSoftLimitActive: boolean;
  playAttempt: () => void;
};

type ConnectedQuizActivityLearnerResultsProps = QuizActivityLearnerResultsProps & {
  quiz: QuizActivity;
  course: CourseWithDetails;
  viewingAs: UserWithRoleInfo;
};

const QuizActivityLearnerResults: React.FC<ConnectedQuizActivityLearnerResultsProps> = ({
  content,
  quiz,
  course,
  viewingAs,
  printView,
  playAttempt,
  isSoftLimitActive,
}) => {
  const [activeTab, setActiveTab] = useState(
    QUIZ_ACTIVITY_RESULTS_ATTEMPT + quiz.latestSubmittedAttempt?.id
  );

  const noNewAttempt =
    quiz.assessment.pastDeadline ||
    isSoftLimitActive ||
    content.availability.isReadOnly ||
    viewingAs.isPreviewing ||
    (!quiz.unlimitedAttempts && quiz.attemptsRemaining === 0);

  return (
    <Tabs
      activeTab={activeTab}
      setActiveTab={setActiveTab}
    >
      <InstructorNotifications contentId={content.id} />

      {!printView && (
        <TabList className="attempt-history-browser list-group list-unstyled mb-3">
          <Tab
            tabId={QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS}
            className={tabClassNames + ' instructions-tab'}
            activeClassName={tabActiveClassNames}
          >
            <InstructionsTab />
          </Tab>
          {map(quiz.orderedAttempts, (attempt, i) => (
            <Tab
              tabId={QUIZ_ACTIVITY_RESULTS_ATTEMPT + attempt.id}
              key={attempt.id}
              className={tabClassNames + ' view-attempt-tab'}
              activeClassName={tabActiveClassNames}
            >
              <ViewAttemptTab
                key={attempt.id}
                attempt={attempt}
                attemptNumber={i}
                showGrade={!quiz.assessment.settings.isCheckpoint}
                content={content}
                quiz={quiz}
                viewingAs={viewingAs}
              />
            </Tab>
          ))}
        </TabList>
      )}

      {!printView && !noNewAttempt && (
        <div className="mb-3">
          <button
            className={'new-attempt btn btn-outline-success w-100 quiz-retake'}
            onClick={playAttempt}
          >
            <NewAttemptTab
              viewingAs={viewingAs}
              attemptsRemaining={quiz.attemptsRemaining}
              unlimitedAttempts={quiz.unlimitedAttempts}
              isReadOnly={content.availability.isReadOnly}
              isCheckpoint={quiz.assessment.settings.isCheckpoint}
              i18nKey={
                quiz.assessment.settings.isCheckpoint ? 'CHECKPOINT_RETAKE' : 'ASSESSMENT_RETAKE'
              }
            />
          </button>
        </div>
      )}

      {isSoftLimitActive && (
        <QuizSoftLimitMessage
          message={quiz.assessment.settings.softAttemptLimitMessage}
          limit={quiz.assessment.settings.softAttemptLimit}
        />
      )}

      <TabPanel tabId={QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS}>
        <ContentInstructions instructions={quiz.assessment.instructions} />
        {!quiz.assessment.settings.isCheckpoint && (
          <QuizActivityInfo
            content={content}
            quiz={quiz}
            noAttemptNumber
          />
        )}
      </TabPanel>

      {map(quiz.orderedAttempts, attempt => (
        <TabPanel
          tabId={QUIZ_ACTIVITY_RESULTS_ATTEMPT + attempt.id}
          key={attempt.id}
        >
          {!isEmpty(quiz.assessment.settings.testsOut) && (
            <QuizTestOut
              quiz={quiz}
              attempt={attempt}
            />
          )}

          <QuizActivityResultsAttempt
            course={course}
            content={content}
            quiz={quiz}
            attempt={attempt}
            viewingAs={viewingAs}
            printView={printView}
            playAttempt={playAttempt}
            isSoftLimitActive={isSoftLimitActive}
          />

          {content.hasCompetencies && quiz.latestSubmittedAttempt?.id === attempt.id && (
            <>
              <hr className="competencies-separator" />
              <QuizActivitySummary
                content={content}
                quiz={quiz}
                viewAttempt={(attempt: QuizAttempt) =>
                  setActiveTab(QUIZ_ACTIVITY_RESULTS_ATTEMPT + attempt.id)
                }
              />
            </>
          )}
        </TabPanel>
      ))}
    </Tabs>
  );
};

// We have to use compose because the garbage selectors otherwise don't work inside a function component
// in lesson print mode because they look to the browser URL content id, not the parameter content id
export default compose<ConnectedQuizActivityLearnerResultsProps, QuizActivityLearnerResultsProps>(
  connect(selectQuizActivityComponent)
)(QuizActivityLearnerResults);
