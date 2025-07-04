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

import { Content } from '../api/contentsApi.ts';
import { ElementValue } from '../types/arrays.ts';
import { ContentThinnedWithLearningIndex } from './contentResponse.ts';

import { AssetTypeId } from './assetTypes.ts';

const ContentAssetTypeKeys: AssetTypeId[] = [
  'unit.1',
  'module.1',
  'lesson.1',
  'assessment.1',
  'checkpoint.1',
  'diagnostic.1',
  'poolAssessment.1',
  'assignment.1',
  'discussion.1',
  'resource.1',
  'lti.1',
  'fileBundle.1',
  'scorm.1',
  'html.1',
  'observationAssessment.1',
];

export type ContentAssetType = ElementValue<typeof ContentAssetTypeKeys>;

export const CONTENT_TYPE_UNIT = 'unit.1';
export const CONTENT_TYPE_MODULE = 'module.1';
export const CONTENT_TYPE_LESSON = 'lesson.1';

export const CONTENT_TYPE_DISCUSSION = 'discussion.1';

export const CONTENT_TYPE_ASSESSMENT = 'assessment.1';
export const CONTENT_TYPE_CHECKPOINT = 'checkpoint.1';
export const CONTENT_TYPE_DIAGNOSTIC = 'diagnostic.1';
export const CONTENT_TYPE_POOLED_ASSESSMENT = 'poolAssessment.1';

export const CONTENT_TYPE_ASSIGNMENT = 'assignment.1';

export const CONTENT_TYPE_OBSERVATION_ASSESSMENT = 'observationAssessment.1';

export const CONTENT_TYPE_RESOURCE = 'resource.1' as const;
export type CONTENT_TYPE_RESOURCE = typeof CONTENT_TYPE_RESOURCE;
export const CONTENT_TYPE_LTI = 'lti.1' as const;
export type CONTENT_TYPE_LTI = typeof CONTENT_TYPE_LTI;
export const CONTENT_TYPE_HTML = 'html.1';
export const CONTENT_TYPE_SCORM = 'scorm.1' as const;
export type CONTENT_TYPE_SCORM = typeof CONTENT_TYPE_SCORM;
export const CONTENT_TYPE_FILE_BUNDLE = 'fileBundle.1' as const;
export type CONTENT_TYPE_FILE_BUNDLE = typeof CONTENT_TYPE_FILE_BUNDLE;

export const CONTENT_TYPE_COURSE_LINK = 'courseLink.1' as const;
export type CONTENT_TYPE_COURSE_LINK = typeof CONTENT_TYPE_COURSE_LINK;

export const CONTENT_ITEM_TYPE_QUIZ = 'loi.cp.quiz.contentitem.QuizContentItem';

export const isQuiz = (content: { typeId: string }) =>
  [
    CONTENT_TYPE_ASSESSMENT,
    CONTENT_TYPE_CHECKPOINT,
    CONTENT_TYPE_DIAGNOSTIC,
    CONTENT_TYPE_POOLED_ASSESSMENT,
  ].indexOf(content.typeId) !== -1;

export const isSubmission = (content: { typeId: string }) =>
  [CONTENT_TYPE_ASSIGNMENT, CONTENT_TYPE_OBSERVATION_ASSESSMENT].indexOf(content.typeId) !== -1;

export const isCheckpoint = (content: { typeId: string }) =>
  content.typeId === CONTENT_TYPE_CHECKPOINT;

export const isAssessment = (content: { typeId: string }) =>
  isSubmission(content) || isQuiz(content);

export const isInstructorControlled = (content: { typeId: string }) =>
  [CONTENT_TYPE_OBSERVATION_ASSESSMENT].indexOf(content.typeId) !== -1;

export const isDiscussion = (content: { typeId: string }) =>
  content.typeId === CONTENT_TYPE_DISCUSSION;

export const isGradedLTI = (content: Content | ContentThinnedWithLearningIndex) =>
  content.typeId === CONTENT_TYPE_LTI && content.hasGradebookEntry;

export const isGradedSCORM = (content: Content | ContentThinnedWithLearningIndex) =>
  content.typeId === CONTENT_TYPE_SCORM && content.hasGradebookEntry;

export const isGradedCourseLink = (content: Content | ContentThinnedWithLearningIndex) =>
  content.typeId === CONTENT_TYPE_COURSE_LINK && content.hasGradebookEntry;

export const isAssignment = (content: Content | ContentThinnedWithLearningIndex) =>
  (isAssessment(content) && !isCheckpoint(content)) ||
  isDiscussion(content) ||
  isGradedLTI(content) ||
  isGradedCourseLink(content) ||
  isGradedSCORM(content);

// if this excludes checkpoint then the instructor can't view student attempts.
export const isGradableAssignment = (content: Content) => isAssessment(content);
