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
import { ContentWithNebulousDetails } from '../../../../courseContentModule/selectors/contentEntry';
import React from 'react';

import ActivityInfo from '../../../parts/ActivityInfo.tsx';

interface SubmissionActivityInfoProps {
  content: ContentWithNebulousDetails;
  submissionActivity: SubmissionActivity;
  noAttemptNumber?: boolean;
}

const SubmissionActivityInfo: React.FC<SubmissionActivityInfoProps> = ({
  content,
  submissionActivity,
  noAttemptNumber,
}) => (
  <ActivityInfo
    content={content}
    gradeCalculationType={submissionActivity.assessment.settings.gradingPolicy}
    maximumAttempts={submissionActivity.assessment.settings.maxAttempts}
    attemptNumber={noAttemptNumber ? undefined : submissionActivity.attemptNumber}
  />
);

export default SubmissionActivityInfo;
