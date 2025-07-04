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

import { uniq } from 'lodash';

import QUESTION_TYPES from '../asset/constants/questionTypes.constants';
import SURVEY_QUESTION_TYPES from '../asset/constants/surveyQuestionTypes.constants';
import remedialFileTypes from '../editors/constants/remedialFileTypes.constants';
import { TypeId } from '../types/asset';
import { EdgeGroup } from '../types/edge';
import * as T from '../types/typeIds';

export const FILE_TYPES: TypeId[] = [
  'image.1',
  'video.1',
  'audio.1',
  'pdf.1',
  'file.1',
  'videoCaption.1',
];

export const COMP_TYPES: TypeId[] = [T.Level1Competency, T.Level2Competency, T.Level3Competency];

const gradebookCategory = [T.GradebookCategory];

export const ELEMENT_TYPES: TypeId[] = [
  T.Assessment,
  T.Assignment,
  T.Checkpoint,
  T.CourseLink,
  T.Diagnostic,
  T.Discussion,
  T.FileBundle,
  T.Html,
  T.ObservationAssessment,
  T.Lti,
  T.PoolAssessment,
  T.Resource1,
  T.Scorm,
];

export const CONTAINER_AND_ELEMENT_TYPES: TypeId[] = [T.Unit, T.Module, T.Lesson, ...ELEMENT_TYPES];

export const GATE_TYPES: TypeId[] = CONTAINER_AND_ELEMENT_TYPES;

const hyperlinks: TypeId[] = [T.Course, ...CONTAINER_AND_ELEMENT_TYPES];

const survey: TypeId[] = [T.Survey];

const edgeRules = {
  'root.1': {
    courses: ['course.1 '],
    competencySets: ['competencySet.1'],
  },
  'competencySet.1': {
    level1Competencies: ['level1Competency.1'],
  },
  'level1Competency.1': {
    level2Competencies: ['level2Competency.1'],
  },
  'level2Competency.1': {
    level3Competencies: ['level3Competency.1'],
  },
  'level3Competency.1': {},
  'course.1': {
    elements: [T.Unit, T.Module],
    image: ['image.1'],
    gradebookCategories: ['gradebookCategory.1'],
  },
  'gradebookCategory.1': {},
  'unit.1': {
    elements: [T.Module, ...ELEMENT_TYPES], // course-lw can't handle a lesson here..
  },
  'module.1': {
    elements: [T.Lesson, ...ELEMENT_TYPES],
  },
  'lesson.1': {
    elements: ELEMENT_TYPES,
    teaches: COMP_TYPES,
  },
  'assessment.1': {
    questions: QUESTION_TYPES,
    gates: GATE_TYPES,
    gradebookCategory,
    hyperlinks,
    survey,
  },
  'checkpoint.1': {
    questions: QUESTION_TYPES,
    gates: GATE_TYPES,
    hyperlinks,
    survey,
  },
  'diagnostic.1': {
    questions: QUESTION_TYPES,
    testsOut: GATE_TYPES,
    hyperlinks,
    survey,
  },
  'html.1': {
    dependencies: ['webDependency.1'],
    resources: FILE_TYPES,
    teaches: COMP_TYPES,
    hyperlinks,
    survey,
  },
  'poolAssessment.1': {
    questions: QUESTION_TYPES,
    gates: GATE_TYPES,
    gradebookCategory,
    hyperlinks,
    survey,
  },
  'video.1': {
    captions: ['videoCaption.1'],
  },
  'webDependency.1': {
    scripts: ['js.1'],
    stylesheets: ['css.1'],
  },
  'binDropQuestion.1': {
    remediationResources: remedialFileTypes,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'fillInTheBlankQuestion.1': {
    remediationResources: remedialFileTypes,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'matchingQuestion.1': {
    remediationResources: remedialFileTypes,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'multipleChoiceQuestion.1': {
    remediationResources: remedialFileTypes,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'multipleSelectQuestion.1': {
    remediationResources: remedialFileTypes,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'orderingQuestion.1': {
    remediationResources: remedialFileTypes,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'trueFalseQuestion.1': {
    remediationResources: remedialFileTypes,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'essayQuestion.1': {
    cblRubric: ['rubric.1'],
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
  },
  'assignment.1': {
    cblRubric: ['rubric.1'],
    gates: GATE_TYPES,
    assesses: COMP_TYPES,
    gradebookCategory,
    hyperlinks,
    survey,
  },
  'discussion.1': {
    cblRubric: ['rubric.1'],
    assesses: COMP_TYPES,
    gradebookCategory,
    hyperlinks,
    survey,
  },
  'observationAssessment.1': {
    gates: GATE_TYPES,
    cblRubric: ['rubric.1'],
    assesses: COMP_TYPES,
    gradebookCategory,
    hyperlinks,
    survey,
  },
  'resource.1': {
    teaches: COMP_TYPES,
    hyperlinks,
    survey,
  },
  'css.1': {
    cssResources: ['image.1'],
  },
  'lti.1': {
    gates: GATE_TYPES,
    teaches: COMP_TYPES,
    assesses: COMP_TYPES,
    gradebookCategory,
    hyperlinks,
    survey,
  },
  'courseLink.1': {
    gates: GATE_TYPES,
    teaches: COMP_TYPES,
    assesses: COMP_TYPES,
    resources: FILE_TYPES,
    gradebookCategory,
    hyperlinks,
    survey,
  },
  'fileBundle.1': {
    teaches: COMP_TYPES,
    hyperlinks,
    survey,
  },
  'survey.1': {
    questions: SURVEY_QUESTION_TYPES,
  },
  'surveyEssayQuestion.1': {
    resources: FILE_TYPES,
  },
  'surveyChoiceQuestion.1': {
    resources: FILE_TYPES,
  },
  'scorm.1': { gradebookCategory, hyperlinks, survey },
  'rubric.1': { criteria: ['rubricCriterion.1'] },
  'rubricCriterion.1': { assesses: COMP_TYPES },
} as const;

export const AllEdgeGroups = uniq(
  Object.values(edgeRules).flatMap(rules => Object.keys(rules))
).sort() as EdgeGroup[];

export default edgeRules;
