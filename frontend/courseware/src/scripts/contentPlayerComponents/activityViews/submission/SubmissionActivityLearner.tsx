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

import { ERActivityProps } from '../../../commonPages/contentPlayer/views/ERActivity.tsx';
import RubricGrid from '../../../components/rubric/RubricGrid.tsx';
import Tutorial from '../../../tutorial/Tutorial.tsx';
import { SubmissionOpenAttemptLoader } from '../../activityViews/submission/loaders';
import SubmissionActivityInfo from './parts/SubmissionActivityInfo.tsx';
import { selectSubmissionActivityComponent } from './redux/submissionActivitySelectors.ts';
import NewSubmissionButton from './sticky/NewSubmissionButton.tsx';
import SubmissionEditor from './sticky/SubmissionEditor.tsx';
import { SubmissionTabs } from './views/SubmissionTabs.tsx';
import ContentInstructions from '../../parts/ContentInstructions';
import {
  QUIZ_ACTIVITY_RESULTS_ATTEMPT,
  QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS,
} from '../../../utilities/activityStates';
import React, { useEffect, useRef, useState } from 'react';
import { ConnectedProps, connect } from 'react-redux';

const connector = connect(selectSubmissionActivityComponent);
type SubmissionActivityLearnerStickyProps = ConnectedProps<typeof connector> & {
  printView?: boolean;
  onLoaded?: () => void | (() => void);
};

const SubmissionActivityLearner: React.FC<SubmissionActivityLearnerStickyProps> = ({
  content,
  viewingAs,
  printView,
  onLoaded,
  submissionActivity,
}) => {
  const { assessment, isLatestAttemptOpen, orderedAttempts, latestAttempt } = submissionActivity;

  const initialTabState = () => {
    if (submissionActivity.latestSubmittedAttempt && !assessment.pastDeadline) {
      /* In all cases it must be before deadline and a previous submitted attempt must exist.
       *  Then, if the student doesn't have a current attempt or if we're not a student, show last attempt. */
      if (!isLatestAttemptOpen || viewingAs.isPreviewing) {
        return QUIZ_ACTIVITY_RESULTS_ATTEMPT + submissionActivity.latestSubmittedAttempt.id;
      }
    }
    return QUIZ_ACTIVITY_RESULTS_INSTRUCTIONS;
  };
  const [activeTab, setActiveTab] = useState(initialTabState);

  useEffect(() => onLoaded?.(), [onLoaded]);

  const [mode, setMode] = useState<'tabs' | 'instructions' | 'editor'>(
    orderedAttempts.length && !isLatestAttemptOpen ? 'tabs' : 'instructions'
  );

  const prevAttemptsLength = useRef<number>(orderedAttempts.length);
  useEffect(() => {
    if (
      !isLatestAttemptOpen &&
      orderedAttempts.length &&
      orderedAttempts.length > prevAttemptsLength.current &&
      latestAttempt
    ) {
      setMode('tabs');
      setActiveTab(QUIZ_ACTIVITY_RESULTS_ATTEMPT + latestAttempt.id);
    }
    prevAttemptsLength.current = orderedAttempts.length;
  }, [isLatestAttemptOpen, orderedAttempts.length, latestAttempt]);

  const showNewOrResumeButton = !viewingAs.isInstructor && submissionActivity.learnerCanDrive;

  return (
    <>
      {mode === 'tabs' ? (
        <SubmissionTabs
          content={content}
          submissionActivity={submissionActivity}
          viewingAs={viewingAs}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          printView={printView}
          newAttempt={() => setMode('instructions')}
        />
      ) : mode === 'editor' ? (
        <SubmissionOpenAttemptLoader
          content={content}
          viewingAsId={viewingAs.id}
        >
          <SubmissionEditor
            submissionActivity={submissionActivity}
            content={content}
            viewingAs={viewingAs}
            closeEditor={() => setMode('instructions')}
          />
        </SubmissionOpenAttemptLoader>
      ) : (
        <>
          <ContentInstructions instructions={submissionActivity.assessment.instructions} />

          {submissionActivity.assessment.rubric && (
            <div className="mx-3 my-4">
              <RubricGrid rubric={submissionActivity.assessment.rubric} />
            </div>
          )}

          <SubmissionActivityInfo
            content={content}
            submissionActivity={submissionActivity}
          />

          {showNewOrResumeButton && !printView && (
            <NewSubmissionButton
              submissionActivity={submissionActivity}
              content={content}
              viewingAs={viewingAs}
              playAttempt={() => setMode('editor')}
            />
          )}
        </>
      )}

      {!printView && (
        <Tutorial
          name={
            submissionActivity.learnerCanDrive
              ? 'assignment-introduction'
              : 'observation-introduction'
          }
        />
      )}
    </>
  );
};

export default connector(SubmissionActivityLearner) as React.FC<ERActivityProps>;
