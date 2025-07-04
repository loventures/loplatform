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

import { AttachmentInfo, BasicFeedback, RubricFeedback } from '../../../api/quizApi.ts';
import { SubmissionAssessment, SubmissionAttempt } from '../../../api/submissionApi.ts';

export type DisplaySubmissionAttempt = SubmissionAttempt & {
  isOpen: boolean;
  attachments: AttachmentInfo[];
  feedback: (RubricFeedback | (BasicFeedback & { attachments: AttachmentInfo[] }))[];
};
export type SubmissionActivity = {
  assessment: SubmissionAssessment;
  orderedAttempts: DisplaySubmissionAttempt[];
  orderedAttemptsLength: number;
  latestAttempt?: DisplaySubmissionAttempt;
  isLatestAttemptOpen?: boolean;
  openAttempt?: DisplaySubmissionAttempt | null;
  hasSubmittedAttempts: boolean;
  attemptsRemaining: number;
  attemptNumber: number;
  unlimitedAttempts: boolean;
  canPlayAttempt: boolean;
  learnerCanDrive: boolean;
  latestSubmittedAttempt?: DisplaySubmissionAttempt;
};
