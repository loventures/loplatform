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

import RubricGrid from '../../../components/rubric/RubricGrid.tsx';
import { selectSubmissionActivityComponent } from './redux/submissionActivitySelectors.ts';
import ContentInstructions from '../../parts/ContentInstructions';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../../courseContentModule/selectors/contentEntry';
import React, { useEffect } from 'react';
import { ConnectedProps, connect } from 'react-redux';

import { SubmissionActivityLoader } from './loaders';
import SubmissionActivityInfo from './parts/SubmissionActivityInfo.tsx';
import SubmissionCompetencies from './parts/SubmissionCompetencies.tsx';

interface SubmissionActivityInstructorContentProps extends PropsFromConnect {
  // content: ContentWithNebulousDetails;
  // submissionActivity: SubmissionActivity;
  onLoaded?: () => void | (() => void);
  printView?: boolean;
}

const SubmissionActivityInstructorContent: React.FC<SubmissionActivityInstructorContentProps> = ({
  content,
  submissionActivity,
  onLoaded,
}) => {
  useEffect(() => onLoaded?.(), [onLoaded]);
  return (
    <div>
      <ContentInstructions instructions={submissionActivity.assessment.instructions} />

      {submissionActivity.assessment.rubric && (
        <div className="mx-3 my-4">
          <RubricGrid rubric={submissionActivity.assessment.rubric} />
        </div>
      )}

      <SubmissionActivityInfo
        content={content}
        submissionActivity={submissionActivity}
        noAttemptNumber
      />

      {content.hasCompetencies && <SubmissionCompetencies competencies={content.competencies} />}
    </div>
  );
};

const connector = connect(selectSubmissionActivityComponent);
type PropsFromConnect = ConnectedProps<typeof connector>;

// I have to cast this to `any` so I'm allowed to pass `content={content}` below so
// when I'm in print module view, it doesn't try to pull the content id from the URL.
const SubmissionActivityInstructorContentC = connector(SubmissionActivityInstructorContent) as any;

interface SubmissionActivityInstructorProps {
  content: ContentWithNebulousDetails;
  viewingAs: ViewingAs;
  onLoaded?: () => void | (() => void);
  printView?: boolean;
}

const SubmissionActivityInstructor: React.FC<SubmissionActivityInstructorProps> = ({
  content,
  viewingAs,
  onLoaded,
  printView,
}) => (
  <SubmissionActivityLoader
    content={content}
    viewingAs={viewingAs}
  >
    <SubmissionActivityInstructorContentC
      content={content}
      // viewingAs={viewingAs}
      // actualUser={actualUser}
      onLoaded={onLoaded}
      printView={printView}
    />
  </SubmissionActivityLoader>
);

export default SubmissionActivityInstructor;
