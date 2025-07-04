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

import React from 'react';
import { useDispatch } from 'react-redux';
import Textarea from '../../../react-textarea-autosize';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../../graphEdit';
import { NarrativeEditor, useEditSession } from '../../story';
import { useIsEditable, useNarrativeAssetState } from '../../storyHooks';

const deUntitled = (s: string) => (s === 'Untitled' ? '' : s);

export const LikertScaleQuestionEditor: NarrativeEditor<'likertScaleQuestion.1'> = ({
  asset: question,
  readOnly,
}) => {
  const dispatch = useDispatch();
  const editMode = useIsEditable(question.name) && !readOnly;

  const { created } = useNarrativeAssetState(question);

  const session = useEditSession();
  const title = question.data.title;
  const editPrompt = (title: string) => {
    dispatch(beginProjectGraphEdit('Question prompt', session));
    dispatch(
      editProjectGraphNodeData(question.name, {
        title: title || 'Untitled',
      })
    );
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };

  return (
    <div className="mb-3 mx-2">
      {!editMode ? (
        <div className="input-padding">{title}</div>
      ) : (
        <Textarea
          value={deUntitled(title)}
          autoFocus={created}
          onChange={e => editPrompt(e.target.value)}
          className="form-control secret-input mb-3 py-2 px-3"
          style={{ resize: 'none' }}
          placeholder="Likert prompt"
          maxLength={255}
        />
      )}
      <div className="text-muted text-center">
        Strongly agree ... Agree ... Neither agree nor disagree ... Disagree ... Strongly disagree
      </div>
    </div>
  );
};
