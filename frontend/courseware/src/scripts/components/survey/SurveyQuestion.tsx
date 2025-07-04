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

import {
  Asset,
  LikertScaleQuestion1,
  RatingScaleQuestion1,
  SurveyChoiceQuestion1,
  SurveyEssayQuestion1,
} from '../../authoring';
import {
  QUESTION_ASSET_TYPE_ESSAY,
  QUESTION_ASSET_TYPE_LIKERT_SCALE,
  QUESTION_ASSET_TYPE_MULTIPLE_CHOICE,
  QUESTION_ASSET_TYPE_RATING_SCALE,
  QUESTION_ASSET_TYPE_SURVEY_CHOICE,
  QUESTION_ASSET_TYPE_SURVEY_ESSAY,
} from '../../utilities/questionTypes';
import React from 'react';

import SurveyEssayQuestionBody from './SurveyEssayQuestionBody';
import SurveyLikertScaleQuestion1Body from './SurveyLikertScaleQuestion1Body';
import SurveyMultipleChoiceQuestionBody from './SurveyMultipleChoiceQuestionBody';
import SurveyRatingScaleQuestion1Body from './SurveyRatingScaleQuestion1Body';

type PromptSurveyQuestion = SurveyEssayQuestion1 | SurveyChoiceQuestion1;

type TitleSurveyQuestion = LikertScaleQuestion1 | RatingScaleQuestion1;

type SurveyQuestion = PromptSurveyQuestion | TitleSurveyQuestion;

const questionBody = (question: Asset<SurveyQuestion>): any => {
  switch (question.typeId) {
    case QUESTION_ASSET_TYPE_ESSAY:
      return SurveyEssayQuestionBody;
    case QUESTION_ASSET_TYPE_LIKERT_SCALE:
      return SurveyLikertScaleQuestion1Body;
    case QUESTION_ASSET_TYPE_MULTIPLE_CHOICE:
      return () => null;
    case QUESTION_ASSET_TYPE_RATING_SCALE:
      return SurveyRatingScaleQuestion1Body;
    case QUESTION_ASSET_TYPE_SURVEY_ESSAY:
      return SurveyEssayQuestionBody;
    case QUESTION_ASSET_TYPE_SURVEY_CHOICE:
      return SurveyMultipleChoiceQuestionBody;
  }
};

function hasPrompt(question: Asset<SurveyQuestion>): question is Asset<PromptSurveyQuestion> {
  return 'prompt' in question.data;
}

function hasTitle(question: Asset<SurveyQuestion>): question is Asset<TitleSurveyQuestion> {
  return 'title' in question.data;
}

export interface Props {
  index: number;
  question: Asset<SurveyQuestion>;
  numQuestions: number;
  disabled: boolean;
  setResponse: (r: string) => void;
}

const SurveyQuestion: React.FC<Props> = ({
  index,
  question,
  numQuestions,
  disabled,
  setResponse,
}) => {
  const QuestionBody = questionBody(question);

  const questionText = hasPrompt(question) ? (
    <div
      className="question-text"
      dangerouslySetInnerHTML={{ __html: question.data.prompt.html }}
    ></div>
  ) : (
    <p className="question-text">{hasTitle(question) ? question.data.title : ''}</p>
  );

  return (
    <div className="question-container">
      {numQuestions > 1 ? (
        <div className="question-body">
          <div className="question-number-column">
            <div className="survey-question-number me-2">{index + 1}</div>
          </div>
          <div className="question-content-column">
            {questionText}
            <QuestionBody
              question={question}
              setResponse={setResponse}
              disabled={disabled}
            />
          </div>
        </div>
      ) : (
        <div className="question-body justify-content-center">
          <div className="question-content-column">
            {questionText}
            <QuestionBody
              question={question}
              setResponse={setResponse}
              disabled={disabled}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default SurveyQuestion;
