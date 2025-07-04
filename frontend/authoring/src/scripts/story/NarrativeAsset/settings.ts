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

import { TypeId } from '../../types/asset';
import {
  AssessmentSettings,
  AssignmentSettings,
  CheckpointSettings,
  CourseLinkSettings,
  CourseSettings,
  DiagnosticSettings,
  DiscussionSettings,
  LtiSettings,
  ObservationAssessmentSettings,
  PoolAssessmentSettings,
  ScormSettings,
  SurveySettings,
} from '../settings';
import { NarrativeSettings } from '../story';

export const narrativeSettings: Partial<Record<TypeId, NarrativeSettings<any>>> = {
  'assessment.1': AssessmentSettings,
  'assignment.1': AssignmentSettings,
  'checkpoint.1': CheckpointSettings,
  'diagnostic.1': DiagnosticSettings,
  'discussion.1': DiscussionSettings,
  'observationAssessment.1': ObservationAssessmentSettings,
  'lti.1': LtiSettings,
  'poolAssessment.1': PoolAssessmentSettings,
  'scorm.1': ScormSettings,
  'courseLink.1': CourseLinkSettings,
  'survey.1': SurveySettings,
  'course.1': CourseSettings,
}; // not doing: resource.1, fileBundle.1
