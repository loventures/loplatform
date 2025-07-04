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

import { Asset, SurveyEssayQuestion1 } from '../../authoring';
import { TranslationContext } from '../../i18n/translationContext';
import React, { useContext, useState } from 'react';
import { FormFeedback, Input } from 'reactstrap';

export interface SurveyEssayProps {
  question: Asset<SurveyEssayQuestion1>;
  setResponse: (r: string) => void;
  disabled: boolean;
}

const SURVEY_ESSAY_MAXLENGTH = 4096;

const SurveyEssayQuestionBody: React.FC<SurveyEssayProps> = ({
  question,
  setResponse,
  disabled,
}) => {
  const translate = useContext(TranslationContext);

  const [value, setValue] = useState('');
  const invalid = value.length >= SURVEY_ESSAY_MAXLENGTH;
  return (
    <>
      <Input
        type="textarea"
        name={question.name}
        value={value}
        disabled={disabled}
        onChange={val => {
          setValue(val.target.value);
          setResponse(val.target.value);
        }}
        maxLength={SURVEY_ESSAY_MAXLENGTH}
        invalid={invalid}
        aria-invalid={invalid}
      />
      {invalid && (
        <FormFeedback>
          {translate('SURVEY_ESSAY_QUESTION_VALIDATION', { max: SURVEY_ESSAY_MAXLENGTH })}
        </FormFeedback>
      )}
    </>
  );
};

export default SurveyEssayQuestionBody;
