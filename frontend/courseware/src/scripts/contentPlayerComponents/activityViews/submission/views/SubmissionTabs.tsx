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

import classNames from 'classnames';
import InstructorNotifications from '../../../../components/InstructorNotifications.tsx';
import RubricGrid from '../../../../components/rubric/RubricGrid.tsx';
import { Tab, TabList, TabPanel, Tabs } from '../../../../components/tabs';
import { map } from 'lodash';
import InstructionsTab from '../../../activityViews/quiz/parts/attemptHistoryTabs/InstructionsTab';
import NewAttemptTab from '../../../activityViews/quiz/parts/attemptHistoryTabs/NewAttemptTab';
import ViewAttemptTab from '../../../activityViews/quiz/parts/attemptHistoryTabs/ViewAttemptTab';
import SubmissionActivityInfo from '../parts/SubmissionActivityInfo.tsx';
import SubmissionCompetencies from '../parts/SubmissionCompetencies.tsx';
import { SubmissionActivity } from '../submissionActivity';
import SubmissionAttemptView from './SubmissionAttemptView.tsx';
import ContentInstructions from '../../../parts/ContentInstructions';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../../../courseContentModule/selectors/contentEntry';
import {
  QUIZ_ACTIVITY_RESULTS_ATTEMPT,
  QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS,
} from '../../../../utilities/activityStates';
import { ATTEMPT_OPEN } from '../../../../utilities/attemptStates';
import React from 'react';

interface SubmissionTabsProps {
  content: ContentWithNebulousDetails;
  submissionActivity: SubmissionActivity;
  viewingAs: ViewingAs;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  newAttempt: () => void;
  printView?: boolean;
}

export const SubmissionTabs: React.FC<SubmissionTabsProps> = ({
  content,
  submissionActivity,
  viewingAs,
  activeTab,
  setActiveTab,
  newAttempt,
  printView,
}) => {
  const { assessment, orderedAttempts } = submissionActivity;

  const attemptsRemaining =
    (assessment.settings.maxAttempts ?? 0) -
    orderedAttempts.filter(a => a.state !== ATTEMPT_OPEN).length;
  const canPlayAttempt =
    !assessment.pastDeadline &&
    !content.availability.isReadOnly &&
    !viewingAs.isPreviewing &&
    (submissionActivity?.latestAttempt?.isOpen ||
      !assessment.settings.maxAttempts ||
      attemptsRemaining > 0);

  return (
    <Tabs
      activeTab={activeTab}
      setActiveTab={setActiveTab}
    >
      <InstructorNotifications contentId={content.id} />

      {!printView && (
        <TabList
          className="attempt-history-browser list-group list-unstyled mb-3 d-print-none"
          ariaLabel="View Attempt"
        >
          <Tab
            tabId={QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS}
            className={classNames(
              'attempt-history-item list-group-item list-group-item-action instructions-tab',
              { 'vertical-line': submissionActivity.orderedAttempts.length > 0 }
            )}
            activeClassName="active-item"
          >
            <InstructionsTab />
          </Tab>
          {map(submissionActivity.orderedAttempts, (attempt, i) => (
            <Tab
              tabId={QUIZ_ACTIVITY_RESULTS_ATTEMPT + attempt.id}
              key={attempt.id}
              className="attempt-history-item list-group-item list-group-item-action view-attempt-tab vertical-line"
              activeClassName="active-item"
            >
              <ViewAttemptTab
                key={attempt.id}
                attempt={attempt}
                attemptNumber={i}
                showGrade
                content={undefined}
                quiz={undefined}
                viewingAs={undefined}
              />
            </Tab>
          ))}
        </TabList>
      )}

      {!printView && canPlayAttempt && (
        <div className="mb-4">
          <button
            className={'new-attempt btn btn-outline-success w-100 quiz-retake'}
            onClick={newAttempt}
          >
            <NewAttemptTab
              i18nKey="ASSIGNMENT_ANOTHER_SUBMISSION"
              viewingAs={viewingAs}
              attemptsRemaining={attemptsRemaining}
              unlimitedAttempts={!assessment.settings.maxAttempts}
              isReadOnly={content.availability.isReadOnly}
              isCheckpoint={false}
            />
          </button>
        </div>
      )}

      <TabPanel
        tabId={QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS}
        className=""
      >
        <ContentInstructions instructions={submissionActivity.assessment.instructions} />

        {submissionActivity.assessment.rubric && (
          <div className="my-4 mx-3">
            <RubricGrid rubric={submissionActivity.assessment.rubric} />
          </div>
        )}

        <SubmissionActivityInfo
          content={content}
          submissionActivity={submissionActivity}
          noAttemptNumber
        />

        {content.hasCompetencies && <SubmissionCompetencies competencies={content.competencies} />}
      </TabPanel>
      {map(submissionActivity.orderedAttempts, attempt => (
        <TabPanel
          tabId={QUIZ_ACTIVITY_RESULTS_ATTEMPT + +attempt.id}
          key={attempt.id}
          className="mt-4"
        >
          <SubmissionAttemptView
            content={content}
            viewingAs={viewingAs}
            assessment={submissionActivity.assessment}
            attempt={attempt}
          />
        </TabPanel>
      ))}
    </Tabs>
  );
};
