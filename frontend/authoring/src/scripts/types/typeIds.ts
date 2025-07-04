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

/**
 * WARNING: Only export valid typeIds and their aliases. Exports from this file are considered the
 *          source of truth and the complete Set of valid typeIds.
 *
 * How to use this file:
 * import * as T from './typeIds'
 * You may now do `Asset<T.Survey>` as well as `const surv = { name: 'abc', typeId: T.Survey }`
 * Alternatively (assuming no other naming conflicts):
 * import { Survey } from './typeIds'
 * `Asset<Survey>` and `const surv = { name: 'abc', typeId: Survey }`
 *
 * Since every member of this module is overloaded with a const and type, the compiler chooses the
 * appropriate context from the module namespace. I have not been able to achieve this with
 *   object literals (b/c `Asset<typeof T.Survey>` is too verbose and creating a mapped type results
 *     in `Asset<T['Survey']>`, which is inarguably worse),
 *   enums (b/c same as object literals),
 *   interfaces (b/c they are not namespace for T.Survey access and creating a runtime map to gain
 *     this has the same drawbacks as an object literal),
 *   types (b/c same as interfaces)
 *
 * And thus we land on es6 modules for the namespacing and overloading for compile time and runtime
 * access. I have probably spent more time deciding this is the only path forward over the last few
 * months than all the time it will never save.
 * */

export const Survey = 'survey.1';
export type Survey = typeof Survey;

export const LikertQuestion = 'likertScaleQuestion.1';
export type LikertQuestion = typeof LikertQuestion;

export const RatingQuestion = 'ratingScaleQuestion.1';
export type RatingQuestion = typeof RatingQuestion;

export const SurveyEssayQuestion = 'surveyEssayQuestion.1';
export type SurveyEssayQuestion = typeof SurveyEssayQuestion;

export const SurveyChoiceQuestion = 'surveyChoiceQuestion.1';
export type SurveyChoiceQuestion = typeof SurveyChoiceQuestion;

export const MultipleChoiceQuestion = 'multipleChoiceQuestion.1';
export type MultipleChoiceQuestion = typeof MultipleChoiceQuestion;

export const MultipleSelectQuestion = 'multipleSelectQuestion.1';
export type MultipleSelectQuestion = typeof MultipleSelectQuestion;

export const TrueFalseQuestion = 'trueFalseQuestion.1';
export type TrueFalseQuestion = typeof TrueFalseQuestion;

export const EssayQuestion = 'essayQuestion.1';
export type EssayQuestion = typeof EssayQuestion;

export const FillInTheBlankQuestion = 'fillInTheBlankQuestion.1';
export type FillInTheBlankQuestion = typeof FillInTheBlankQuestion;

export const MatchingQuestion = 'matchingQuestion.1';
export type MatchingQuestion = typeof MatchingQuestion;

export const Course = 'course.1';
export type Course = typeof Course;

export const Unit = 'unit.1';
export type Unit = typeof Unit;

export const Module = 'module.1';
export type Module = typeof Module;

export const Lesson = 'lesson.1';
export type Lesson = typeof Lesson;

export const Assessment = 'assessment.1';
export type Assessment = typeof Assessment;

export const Checkpoint = 'checkpoint.1';
export type Checkpoint = typeof Checkpoint;

export const PoolAssessment = 'poolAssessment.1';
export type PoolAssessment = typeof PoolAssessment;

export const Diagnostic = 'diagnostic.1';
export type Diagnostic = typeof Diagnostic;

export const Assignment = 'assignment.1';
export type Assignment = typeof Assignment;

export const ObservationAssessment = 'observationAssessment.1';
export type ObservationAssessment = typeof ObservationAssessment;

export const Scorm = 'scorm.1';
export type Scorm = typeof Scorm;

export const Html = 'html.1';
export type Html = typeof Html;

export const FileBundle = 'fileBundle.1';
export type FileBundle = typeof FileBundle;

export const WebDependency = 'webDependency.1';
export type WebDependency = typeof WebDependency;

export const CompetencySet = 'competencySet.1';
export type CompetencySet = typeof CompetencySet;

export const Level1Competency = 'level1Competency.1';
export type Level1Competency = typeof Level1Competency;

export const Level2Competency = 'level2Competency.1';
export type Level2Competency = typeof Level2Competency;

export const Level3Competency = 'level3Competency.1';
export type Level3Competency = typeof Level3Competency;

export const Discussion = 'discussion.1';
export type Discussion = typeof Discussion;

export const Lti = 'lti.1';
export type Lti = typeof Lti;

export const Rubric = 'rubric.1';
export type Rubric = typeof Rubric;

export const RubricCriterion = 'rubricCriterion.1';
export type RubricCriterion = typeof RubricCriterion;

export const GradebookCategory = 'gradebookCategory.1';
export type GradebookCategory = typeof GradebookCategory;

export const Resource1 = 'resource.1';
export type Resource1 = typeof Resource1;

export const CourseLink = 'courseLink.1';
export type CourseLink = typeof CourseLink;

export const Root = 'root.1';
export type Root = typeof Root;

export const Image = 'image.1';
export type Image = typeof Image;

export const Pdf = 'pdf.1';
export type Pdf = typeof Pdf;

export const Video = 'video.1';
export type Video = typeof Video;

export const Audio = 'audio.1';
export type Audio = typeof Audio;

export const File = 'file.1';
export type File = typeof File;
