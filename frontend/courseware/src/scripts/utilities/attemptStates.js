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

export const LEGACY_ATTEMPT_NOT_STARTED = 'NotStarted';
export const LEGACY_ATTEMPT_IN_PROGRESS = 'InProgress';
export const LEGACY_ATTEMPT_PENDING_GRADE = 'PendingGrading';
export const LEGACY_ATTEMPT_COMPLETED = 'Completed';
export const LEGACY_ATTEMPT_INVALID = 'Invalidated';

export const LEGACY_RESPONSE_NOT_STARTED = 'InitState';
export const LEGACY_RESPONSE_IN_PROGRESS = 'AnsweringState';
export const LEGACY_RESPONSE_SAVED = 'SavedState';
export const LEGACY_RESPONSE_PENDING_GRADE = 'PendingGradingState';
export const LEGACY_RESPONSE_GRADING = 'GradingInProgressState';
export const LEGACY_RESPONSE_SUBMITTED = 'SubmittedState';

export const ATTEMPT_OPEN = 'Open';
export const ATTEMPT_SUBMITTED = 'Submitted';
export const ATTEMPT_FINALIZED = 'Finalized';

export const RESPONSE_NOT_SUBMITTED = 'NotSubmitted';
export const RESPONSE_SUBMITTED = 'ResponseSubmitted';
export const RESPONSE_SCORED = 'ResponseScored';
export const RESPONSE_SCORED_RELEASED = 'ResponseScoreReleased';

export const isScoreFinalized = attempt => {
  return attempt.state === ATTEMPT_FINALIZED;
};
