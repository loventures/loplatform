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

import React, { useRef, useState } from 'react';
import { BsRecordCircle } from 'react-icons/bs';

import { HtmlPart, NewAsset, SurveyChoice } from '../../../types/asset';
import { FocusLoser } from '../../components/FocusLoser';
import { PartEditor } from '../../PartEditor';
import { isBlankHtml } from '../../editorUtils.ts';

export const SurveyQuestionChoice: React.FC<{
  question: NewAsset<'surveyChoiceQuestion.1'>;
  choice: SurveyChoice;
  index: number;
  isAdd: boolean;
  deleteChoice: () => void;
  updateChoice: (label: HtmlPart, session: string) => void;
  onFocus: () => void;
  onBlur: (event: React.SyntheticEvent) => void;
}> = ({ question, choice, isAdd, deleteChoice, updateChoice, index }) => {
  const divRef = useRef<HTMLDivElement>();
  const [dirty, setDirty] = useState(false);

  const maybeDelete = () => {
    if (dirty && isBlankHtml(choice.label.html)) deleteChoice();
  };

  return (
    <FocusLoser
      divRef={divRef}
      focusLost={maybeDelete}
    >
      {onFocusHandler => (
        <div
          ref={divRef}
          onFocus={onFocusHandler}
          className="d-flex align-items-start survey-choice"
          style={{ marginLeft: '2rem', marginRight: '.5rem' }}
        >
          <div
            className="d-flex"
            style={{ width: '1rem', marginTop: '.75rem' }}
          >
            {!isAdd && <BsRecordCircle className="text-gray-500" />}
          </div>
          <PartEditor
            id={`choice-${index}`}
            placeholder={isAdd ? 'Add choice' : 'Choice label'}
            asset={question}
            part={choice.label}
            onChange={(label, session) => {
              setDirty(true);
              updateChoice(label, session);
            }}
            compact
            className="flex-grow-1"
          />
        </div>
      )}
    </FocusLoser>
  );
};
