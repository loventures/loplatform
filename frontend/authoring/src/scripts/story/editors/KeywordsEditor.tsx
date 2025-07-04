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

import classNames from 'classnames';
import React, { useCallback, useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { Input, InputGroup, InputGroupText } from 'reactstrap';

import { trackAuthoringEvent } from '../../analytics';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  discardProjectGraphEdit,
  editProjectGraphNodeData,
  useEditedAssetKeywords,
} from '../../graphEdit';
import { useDcmSelector } from '../../hooks';
import { NodeName } from '../../types/asset';
import { useEditSession, useEscapeOrEnterToStopEditing } from '../story';
import { setNarrativeAssetState } from '../storyActions';
import { useIsEditable, useNarrativeAssetState, useStorySelector } from '../storyHooks';

export const KeywordsEditor: React.FC<{ name?: NodeName; readOnly?: boolean }> = ({
  name,
  readOnly,
}) => {
  const dispatch = useDispatch();
  const keywordsMode = useStorySelector(s => s.keyWords);
  const keywords = useEditedAssetKeywords(name);
  const editMode = useIsEditable(name, 'EditSettings') && !readOnly;
  const editing = useNarrativeAssetState(name).keywording;
  const startEditing = useCallback(() => {
    trackAuthoringEvent('Narrative Editor - Edit Keywords');
    dispatch(setNarrativeAssetState(name, { keywording: true }));
  }, [name]);

  return editing ? (
    <KeywordsInputGroup name={name} />
  ) : keywords && keywordsMode ? (
    <div className="mt-2 d-flex justify-content-center">
      <div
        className={classNames('keywords', editMode && 'editable')}
        onClick={editMode ? startEditing : undefined}
        title="Keywords"
      >
        {keywords}
      </div>
    </div>
  ) : null;
};

const KeywordsInputGroup: React.FC<{ name?: NodeName }> = ({ name }) => {
  const dispatch = useDispatch();
  const keywords = useEditedAssetKeywords(name);
  const editing = useNarrativeAssetState(name).keywording;
  const stopEditing = useCallback(() => {
    dispatch(setNarrativeAssetState(name, { keywording: undefined }));
  }, [name]);
  const session = useEditSession(editing);

  const generation = useDcmSelector(state => state.graphEdits.generation);

  const initialGeneration = useRef(generation);
  useEffect(() => {
    if (generation !== initialGeneration.current) stopEditing();
  }, [generation, stopEditing]);

  const onChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const keywords = e.target.value.trim();
      dispatch(beginProjectGraphEdit('Edit keywords', session));
      dispatch(editProjectGraphNodeData(name, { keywords }));
    },
    [name, session]
  );

  const endEditing = useCallback(() => {
    stopEditing();
    dispatch(autoSaveProjectGraphEdits());
  }, []);

  const finishEditing = useCallback(
    (enter: boolean) => {
      if (enter) {
        endEditing();
      } else {
        dispatch(discardProjectGraphEdit(session));
        stopEditing();
      }
    },
    [endEditing, session]
  );

  const keyHandler = useEscapeOrEnterToStopEditing(finishEditing);

  return (
    <div>
      <InputGroup
        size="sm"
        className="keywords-editor fade-in"
      >
        <InputGroupText>Keywords</InputGroupText>
        <Input
          type="text"
          defaultValue={keywords}
          onChange={onChange}
          onBlur={endEditing}
          onKeyDown={keyHandler}
          autoFocus
        />
      </InputGroup>
    </div>
  );
};
