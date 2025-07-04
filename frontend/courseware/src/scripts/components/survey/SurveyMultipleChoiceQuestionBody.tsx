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

import { Asset, SurveyChoiceQuestion1 } from '../../authoring';
import { HtmlWithMathJax } from '../../components/HtmlWithMathjax';
import React, { useState } from 'react';

import SurveyRadioButton from './SurveyRadioButton';

export interface SurveyMultiChoiceProps {
  question: Asset<SurveyChoiceQuestion1>;
  setResponse: (r: string) => void;
  disabled: boolean;
}

const SurveyMultipleChoiceQuestionBody: React.FC<SurveyMultiChoiceProps> = ({
  question,
  setResponse,
  disabled,
}) => {
  const [value, setValue] = useState('');
  return (
    <>
      {question.data.choices.map((choice, i: number) => (
        <SurveyRadioButton
          key={i}
          name={question.name}
          value={choice.value}
          selected={value}
          setSelected={newValue => {
            setValue(newValue);
            setResponse(newValue);
          }}
          disabled={disabled}
        >
          <HtmlWithMathJax
            className="survey-question-distractor"
            html={choice.label.renderedHtml}
          />
        </SurveyRadioButton>
      ))}
    </>
  );
};

export default SurveyMultipleChoiceQuestionBody;
