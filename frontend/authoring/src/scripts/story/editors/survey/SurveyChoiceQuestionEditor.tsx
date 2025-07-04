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

import React, { useCallback, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../../graphEdit';
import { HtmlPart } from '../../../types/asset';
import { PartEditor } from '../../PartEditor';
import { emptyHtml } from '../../questionUtil';
import { NarrativeEditor } from '../../story';
import { useFocusedRemoteEditor, useIsEditable, useNarrativeAssetState } from '../../storyHooks';
import { SurveyQuestionChoice } from './SurveyQuestionChoice';
import { computeNewGuid } from '../../../services/generators/surveyChoiceQuestion-generator';

const newChoice = () => ({ value: computeNewGuid(), label: emptyHtml() });

export const SurveyChoiceQuestionEditor: NarrativeEditor<'surveyChoiceQuestion.1'> = ({
  asset: question,
  readOnly,
}) => {
  const dispatch = useDispatch();
  const editMode = useIsEditable(question.name) && !readOnly;

  const { created } = useNarrativeAssetState(question);

  const editPrompt = useCallback(
    (prompt: HtmlPart, session: string) => {
      dispatch(beginProjectGraphEdit('Question prompt', session));
      dispatch(
        editProjectGraphNodeData(question.name, {
          prompt,
        })
      );
    },
    [dispatch]
  );

  const [nextChoice, setNextChoice] = useState(newChoice);

  const addEditChoice = (label: HtmlPart, session: string, index: number) => {
    dispatch(beginProjectGraphEdit('Choice label', session));
    if (index === question.data.choices.length) {
      dispatch(
        editProjectGraphNodeData(question.name, {
          choices: [...question.data.choices, { ...nextChoice, label }],
        })
      );
      setNextChoice(newChoice);
    } else {
      const choices = [...question.data.choices];
      choices[index] = { ...choices[index], label };
      dispatch(
        editProjectGraphNodeData(question.name, {
          choices,
        })
      );
    }
  };

  const deleteChoice = (index: number) => {
    if (index < question.data.choices.length) {
      dispatch(beginProjectGraphEdit('Delete choice'));
      const choices = [...question.data.choices];
      choices.splice(index, 1);
      dispatch(
        editProjectGraphNodeData(question.name, {
          choices,
        })
      );
      dispatch(autoSaveProjectGraphEdits());
    }
  };

  const choices = useMemo(
    () => (editMode ? [...question.data.choices, nextChoice] : question.data.choices),
    [question, nextChoice, editMode]
  );

  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(question.name, 'choices');

  return (
    <>
      <div
        className="mx-2"
        style={remoteEditor}
      >
        <PartEditor
          id="prompt"
          placeholder="Essay prompt"
          asset={question}
          part={question.data.prompt}
          onChange={editPrompt}
          autoedit={created}
          compact
        />
      </div>
      <div className="my-3">
        {choices.map((choice, index) => (
          <SurveyQuestionChoice
            key={choice.value}
            question={question}
            choice={choice}
            isAdd={choice === nextChoice}
            deleteChoice={() => deleteChoice(index)}
            updateChoice={(label, session) => addEditChoice(label, session, index)}
            onFocus={onFocus}
            onBlur={onBlur}
            index={index}
          />
        ))}
      </div>
    </>
  );
};
