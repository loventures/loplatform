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

import QUESTION_TYPES from '../../asset/constants/questionTypes.constants';
import SURVEY_QUESTION_TYPES from '../../asset/constants/surveyQuestionTypes.constants';

const allowedTypes = [
  'fileBundle.1',
  'poolAssessment.1',
  'observationAssessment.1',
  'resource.1',
  'lti.1',
  'assignment.1',
  'discussion.1',
  'diagnostic.1',
  'assessment.1',
  'checkpoint.1',
  'course.1',
  'module.1',
  'lesson.1',
  'image.1',
  'webDependency.1',
  'js.1',
  'css.1',
  'html.1',
  'scorm.1',
  'audio.1',
  'video.1',
  'pdf.1',
  'rubric.1',
  'videoCaption.1',
  'file.1',
  'survey.1',
  'gradebookCategory.1',
  'courseLink.1',
  ...SURVEY_QUESTION_TYPES,
  ...QUESTION_TYPES,
];

// TODO: all these embed mappings belong in EdgeRuleConstants.ts
const mappings = {
  'surveyEssayQuestion.1': { embed: ['resources'] },
  'surveyChoiceQuestion.1': { embed: ['resources'] },
  'binDropQuestion.1': {
    views: {
      edit: 'binDropAssetQuestionEditor',
    },
    embed: ['assesses', 'remediationResources', 'resources'],
  },
  'essayQuestion.1': {
    views: {
      edit: 'essayAssetQuestionEditor',
    },
    embed: ['cblRubric', 'assesses', 'remediationResources', 'resources'],
  },
  'fillInTheBlankQuestion.1': {
    views: {
      edit: 'fillInTheBlankQuestionEditor',
    },
    embed: ['assesses', 'remediationResources', 'resources'],
  },
  'matchingQuestion.1': {
    views: {
      edit: 'matchingAssetQuestionEditor',
    },
    embed: ['assesses', 'remediationResources', 'resources'],
  },
  'multipleChoiceQuestion.1': {
    views: {
      edit: 'multipleChoiceAssetQuestionEditor',
    },
    embed: ['assesses', 'remediationResources', 'resources'],
  },
  'multipleSelectQuestion.1': {
    views: {
      edit: 'multipleSelectAssetQuestionEditor',
    },
    embed: ['assesses', 'remediationResources', 'resources'],
  },
  'orderingQuestion.1': {
    views: {
      edit: 'orderingAssetQuestionEditor',
    },
    embed: ['assesses', 'remediationResources', 'resources'],
  },
  'trueFalseQuestion.1': {
    views: {
      edit: 'trueFalseAssetQuestionEditor',
    },
    embed: ['assesses', 'remediationResources', 'resources'],
  },
  'poolAssessment.1': {
    views: {
      edit: 'poolAssessmentAssetEditor',
    },
    embed: ['questions', 'resources', 'gates', 'gradebookCategory', 'survey'],
    validStructureGroups: ['questions'],
  },
  'assessment.1': {
    views: {
      edit: 'assessmentAssetEditor',
    },
    embed: ['questions', 'resources', 'gates', 'gradebookCategory', 'survey'],
    validStructureGroups: ['questions'],
  },
  'checkpoint.1': {
    views: {
      edit: 'assessmentAssetEditor',
    },
    embed: ['questions', 'resources', 'gates', 'survey'],
    validStructureGroups: ['questions'],
  },
  'diagnostic.1': {
    views: {
      edit: 'simpleAssessmentAssetEditor',
    },
    embed: ['questions', 'resources', 'gradebookCategory', 'survey'],
    validStructureGroups: ['questions'],
  },
  'observationAssessment.1': {
    views: {
      edit: 'observationAssessmentAssetEditor',
    },
    embed: ['cblRubric', 'resources', 'gates', 'assesses', 'gradebookCategory', 'survey'],
  },
  'rubric.1': {
    views: {},
    embed: ['criteria'],
  },
  'rubricCriterion.1': {
    views: {},
    embed: [],
  },
  'competencySet.1': {
    views: {},
    embed: ['level1Competencies'],
  },
  'discussion.1': {
    views: {
      edit: 'discussionAssetEditor',
    },
    embed: ['cblRubric', 'resources', 'assesses', 'gradebookCategory', 'survey'],
  },
  'pdf.1': {
    views: {
      edit: 'pdfAssetEditor',
    },
    embed: [],
  },
  'css.1': {
    views: {
      edit: 'stylesheetAssetEditor',
    },
    embed: ['cssResources'],
  },
  'fileBundle.1': {
    views: {
      edit: 'fileBundleAssetEditor',
    },
    embed: ['teaches', 'survey'],
  },
  'html.1': {
    views: {
      edit: 'htmlAssetEditor',
    },
    embed: ['resources', 'dependencies', 'survey', 'teaches'],
  },
  'scorm.1': {
    views: {
      edit: 'scormAssetEditor',
    },
    embed: ['gradebookCategory', 'survey'],
  },
  'image.1': {
    views: {
      edit: 'imageAssetEditor',
    },
    embed: [],
  },
  'video.1': {
    views: {
      edit: 'videoAssetEditor',
    },
    embed: ['captions', 'poster', 'transcript'],
  },
  'file.1': {
    views: {
      edit: 'fileAssetEditor',
    },
    embed: [],
  },
  'videoCaption.1': {
    views: {
      edit: 'videoCaptionAssetEditor',
    },
    embed: [],
  },
  'audio.1': {
    views: {
      edit: 'audioAssetEditor',
    },
    embed: [],
  },
  'js.1': {
    views: {
      edit: 'jsFileAssetEditor',
    },
    embed: [],
  },
  'webDependency.1': {
    views: {
      edit: 'webDependencyAssetEditor',
    },
    embed: ['stylesheets', 'scripts'],
  },
  'assignment.1': {
    views: {
      edit: 'assignmentAssetEditor',
    },
    embed: ['cblRubric', 'resources', 'assesses', 'gates', 'gradebookCategory', 'survey'],
  },
  'level1Competency.1': {
    views: {},
    embed: [],
  },
  'level2Competency.1': {
    views: {},
    embed: [],
  },
  'level3Competency.1': {
    views: {},
    embed: [],
  },
  'course.1': {
    views: {
      edit: 'courseAssetEditor',
    },
    embed: ['elements', 'image', 'gradebookCategories'],
    validStructureGroups: ['elements'],
  },
  'lesson.1': {
    views: {
      edit: 'lessonAssetEditor',
    },
    embed: ['elements', 'teaches'],
    validStructureGroups: ['elements'],
  },
  'module.1': {
    views: {
      edit: 'moduleAssetEditor',
    },
    embed: ['elements'],
    validStructureGroups: ['elements'],
  },
  'root.1': {
    embed: ['competencySets', 'courses'],
  },
  'resource.1': {
    views: {
      edit: 'legacyResourceAssetEditor',
    },
    embed: ['inSystemResource', 'teaches', 'resources', 'survey'],
  },
  'lti.1': {
    views: {
      edit: 'ltiAssetEditor',
    },
    embed: ['teaches', 'assesses', 'resources', 'gates', 'gradebookCategory', 'survey'],
  },
  'survey.1': {
    views: {
      edit: 'surveyAssetEditor',
    },
    embed: ['questions'],
  },
  'gradebookCategory.1': {
    views: {},
  },
  'courseLink.1': {
    views: {},
  },
};

export default mappings;

export { allowedTypes };
