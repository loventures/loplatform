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

/* eslint-disable redux-constants/redux-constants */

export const QUIZ_NAVIGATION_UNRESTRICTED = 'unrestricted';
export const QUIZ_NAVIGATION_REQUIRE_ANSWER = 'requireAnswer';
export const QUIZ_NAVIGATION_ENFORCE_LINEAR = 'enforceLinear';
export const QUIZ_NAVIGATION_YOLO = 'YOLO';

export const ResultReleaseTimes = {
  OnResponseScore: 'OnResponseScore',
  OnAttemptScore: 'OnAttemptScore',
};

export const ReleaseRemediationConditions = {
  AnyResponse: 'AnyResponse',
  OnCorrectResponse: 'OnCorrectResponse',
};

export const LEGACY_ASSESSMENT_SETTING_MULTI_PAGE = 'QUESTION_AT_A_TIME';
export const LEGACY_ASSESSMENT_SETTING_SINGLE_PAGE = 'ASSESSMENT_AT_A_TIME';

export const QUIZ_SETTING_MULTI_PAGE = 'paged';
export const QUIZ_SETTING_SINGLE_PAGE = 'singlePage';

export const DRIVER_INSTRUCTOR = 'Observation';
export const DRIVER_LEARNER = 'SubjectDriven';
