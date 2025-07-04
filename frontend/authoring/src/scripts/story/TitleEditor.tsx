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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  discardProjectGraphEdit,
  editProjectGraphNodeData,
  useEditedAssetTitle,
} from '../graphEdit';
import { useDcmSelector, usePolyglot } from '../hooks';
import TextareaAutosize from '../react-textarea-autosize';
import { NodeName, TypeId } from '../types/asset';
import { KeywordsEditor } from './editors/KeywordsEditor';
import { cap, storyTypeName, useEditSession, useEscapeOrEnterToStopEditing } from './story';
import { useIsEditable, useNarrativeAssetState, useRemoteEditor } from './storyHooks';

type TitleEditorProps = {
  name: NodeName;
  typeId: TypeId;
  tag?: 'h1' | 'h2' | 'h3';
  className?: string;
  readOnly: boolean;
  bannerUrl?: string;
};

// TODO: the autofocus after create doesn't reliably work in inline mode?.
export const TitleEditor: React.FC<TitleEditorProps> = ({
  name,
  typeId,
  tag: Hn = 'h3',
  className,
  readOnly,
  bannerUrl,
}) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const assetTitle = useEditedAssetTitle(name);
  const initialTitle = assetTitle === 'Untitled' ? '' : assetTitle;
  const editMode = useIsEditable(name, 'EditContent') && !readOnly;
  const { created } = useNarrativeAssetState(name);
  const [editing, setEditing] = useState(created && !initialTitle);
  const session = useEditSession(editing);
  const generation = useDcmSelector(state => state.graphEdits.generation);
  const remoteEditor = useRemoteEditor(name, 'title', editing);

  const onTitleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const value = e.target.value;
      const trimmed = value.trim();
      const title = !trimmed ? 'Untitled' : trimmed;
      dispatch(beginProjectGraphEdit('Edit title', session));
      dispatch(editProjectGraphNodeData(name, { title }));
    },
    [name, session]
  );

  const initialGeneration = useRef(generation);
  useEffect(() => {
    // blur on save
    if (generation !== initialGeneration.current) setEditing(false);
  }, [generation]);

  const endEditing = useCallback(() => {
    setEditing(false);
    // TODO: these should all be dispatch(endProjectGraphEdit(undoaction);
    dispatch(autoSaveProjectGraphEdits());
  }, []);

  const finishEditing = useCallback(
    (enter: boolean) => {
      if (enter) {
        endEditing();
      } else {
        dispatch(discardProjectGraphEdit(session));
        setEditing(false);
      }
    },
    [endEditing, session]
  );

  const keyHandler = useEscapeOrEnterToStopEditing(finishEditing);

  const typeName = cap(storyTypeName(polyglot, typeId));

  const caretToEnd = useCallback((e?: HTMLTextAreaElement) => {
    e?.setSelectionRange(e.value.length, e.value.length);
  }, []);

  return (
    <div
      className={classNames(
        'asset-title d-flex flex-column mx-2 feedback-context',
        !editMode && 'align-items-center',
        editMode && 'edit-mode',
        className,
        bannerUrl && 'banner-preview'
      )}
      data-id="title"
      style={{ backgroundImage: bannerUrl ? `url(${bannerUrl})` : undefined }}
    >
      {editing && editMode ? (
        <TextareaAutosize
          key={generation}
          ref={caretToEnd}
          className={classNames('editor', Hn)}
          autoFocus
          defaultValue={initialTitle}
          onChange={onTitleChange}
          maxLength={255}
          placeholder={polyglot.t('STORY_TITLE_PLACEHOLDER', { typeName })}
          onBlur={endEditing}
          onKeyDown={keyHandler}
          style={{ resize: 'none', overflow: 'hidden' }}
        />
      ) : (
        <Hn
          tabIndex={editMode ? 0 : undefined}
          onFocus={() => setEditing(editMode)}
          className={classNames('title-editor', remoteEditor && 'remote-edit')}
          style={remoteEditor}
        >
          {assetTitle}
        </Hn>
      )}
      <KeywordsEditor
        name={name}
        readOnly={readOnly}
      />
    </div>
  );
};
