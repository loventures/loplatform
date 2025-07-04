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
  CourseLinkEditor,
  LikertScaleQuestionEditor,
  LtiEditor,
  QuestionEditor,
  RatingScaleQuestionEditor,
  ScormEditor,
  SurveyChoiceQuestionEditor,
  SurveyEssayQuestionEditor,
} from '../editors';
import {
  AudioEditor,
  FileEditor,
  ImageEditor,
  PdfEditor,
  VideoEditor,
  WebDependencyEditor,
} from '../editors/file';
import { RubricTable } from '../editors/rubric/RubricEditor';
import { HtmlEditor } from '../HtmlEditor';
import { NarrativeEditor } from '../story';

const NullEditor: NarrativeEditor<any> = () => null;

export const narrativeEditors: Partial<Record<TypeId, NarrativeEditor<any>>> = {
  'course.1': NullEditor,
  'unit.1': NullEditor,
  'module.1': NullEditor,
  'lesson.1': NullEditor,
  'html.1': HtmlEditor,
  'lti.1': LtiEditor,
  'scorm.1': ScormEditor,
  'multipleChoiceQuestion.1': QuestionEditor,
  'multipleSelectQuestion.1': QuestionEditor,
  'trueFalseQuestion.1': QuestionEditor,
  'essayQuestion.1': QuestionEditor,
  'fillInTheBlankQuestion.1': QuestionEditor,
  'matchingQuestion.1': QuestionEditor,
  'courseLink.1': CourseLinkEditor,
  'survey.1': NullEditor,
  'surveyEssayQuestion.1': SurveyEssayQuestionEditor,
  'likertScaleQuestion.1': LikertScaleQuestionEditor,
  'ratingScaleQuestion.1': RatingScaleQuestionEditor,
  'surveyChoiceQuestion.1': SurveyChoiceQuestionEditor,
  'rubric.1': RubricTable, // this is  really for preview mode but works
  'audio.1': AudioEditor,
  'file.1': FileEditor,
  'image.1': ImageEditor,
  'pdf.1': PdfEditor,
  'video.1': VideoEditor,
  'webDependency.1': WebDependencyEditor,
};
