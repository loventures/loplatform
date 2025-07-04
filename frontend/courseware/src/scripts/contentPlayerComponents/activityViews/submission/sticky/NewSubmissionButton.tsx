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

import { SubmissionActivity } from '../submissionActivity';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../../../courseContentModule/selectors/contentEntry';
import { useTranslation } from '../../../../i18n/translationContext.tsx';
import { ATTEMPT_OPEN } from '../../../../utilities/attemptStates';
import React from 'react';
import { Button } from 'reactstrap';

interface LearnerStickyAttemptButtonProps {
  submissionActivity: SubmissionActivity;
  viewingAs: ViewingAs;
  content: ContentWithNebulousDetails;
  playAttempt: () => void;
}

const NewSubmissionButton: React.FC<LearnerStickyAttemptButtonProps> = ({
  submissionActivity,
  viewingAs,
  content,
  playAttempt,
}) => {
  const translate = useTranslation();
  const { assessment, orderedAttempts, isLatestAttemptOpen } = submissionActivity;
  const canPlayAttempt =
    !assessment.pastDeadline &&
    (submissionActivity?.latestAttempt?.isOpen ||
      assessment.settings.maxAttempts == null ||
      assessment.settings.maxAttempts -
        orderedAttempts.filter(a => a.state !== ATTEMPT_OPEN).length >
        0);
  const disableCreateOrResumeAttempt: boolean =
    !canPlayAttempt || // no attempt in progress and past limit
    viewingAs.isPreviewing || // instructors cannot create submissions
    content.availability.isReadOnly; // read-only content cannot be attempted
  return (
    <div className="flex-center-center mt-3 mb-0 py-2 py-md-3">
      <Button
        className="sticky-new-or-resume-btn"
        color="success"
        size="lg"
        onClick={playAttempt}
        disabled={disableCreateOrResumeAttempt}
      >
        {translate(
          isLatestAttemptOpen ? 'ASSIGNMENT_RESUME_SUBMISSION' : 'ASSIGNMENT_NEW_SUBMISSION'
        )}
      </Button>
    </div>
  );
};

export default NewSubmissionButton;
