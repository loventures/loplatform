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

import { Asset, LikertScaleQuestion1 } from '../../authoring';
import { TranslationContext } from '../../i18n/translationContext';
import React, { useContext, useState } from 'react';

import SurveyRadioButton from './SurveyRadioButton';

export interface SurveyLikertProps {
  question: Asset<LikertScaleQuestion1>;
  setResponse: (r: string) => void;
  disabled: boolean;
}

const SurveyLikertScaleQuestion1Body: React.FC<SurveyLikertProps> = ({
  question,
  setResponse,
  disabled,
}) => {
  const translate = useContext(TranslationContext);
  const [value, setValue] = useState('');
  const buttonLabels = [
    'LIKERT_STRONGLY_AGREE',
    'LIKERT_AGREE',
    'LIKERT_NEITHER',
    'LIKERT_DISAGREE',
    'LIKERT_STRONGLY_DISAGREE',
  ];

  return (
    <>
      {buttonLabels.map((label, idx) => (
        <SurveyRadioButton
          key={idx}
          name={question.name}
          value={idx}
          selected={value}
          setSelected={newValue => {
            setValue(newValue);
            setResponse(newValue);
          }}
          disabled={disabled}
        >
          {translate(label)}
        </SurveyRadioButton>
      ))}
    </>
  );
};

export default SurveyLikertScaleQuestion1Body;
