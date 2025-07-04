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
import { useDcmSelector } from '../hooks';
import { BlockPart, HtmlPart, NewAsset } from '../types/asset';
import { useExitEditingOnSave } from './editorUtils';
import { useExitEditingOnOnUnfocused } from './HtmlEditor/hooks';
import { useEditSession, useEscapeToDiscardEdits } from './story';
import { useIsEditable, useStorySelector } from './storyHooks';
import { PartSourceEditor } from './PartEditor/PartSourceEditor.tsx';
import { PartViewer } from './PartEditor/PartViewer.tsx';
import { PartSummerNote } from './PartEditor/PartSummerNote.tsx';

// TODO: this could switch from simple edit to code mirror when the element is on-screen, which would
// preserve global findability and performance. However the size change might be unacceptable.
export const PartEditor: React.FC<{
  id: string;
  concurrent?: string;
  asset: NewAsset<any>;
  className?: string;
  compact?: boolean;
  noMinHeight?: boolean;
  placeholder: string;
  sometimes?: boolean; // only show this if either present or in edit mode
  readOnly?: boolean;
  autoedit?: boolean; // start in edit mode
  fillInTheBlank?: boolean; // fill in the blank button
  part: BlockPart | HtmlPart | undefined;
  onChange: (html: HtmlPart, session: string) => void;
}> = ({
  id,
  concurrent,
  asset,
  className,
  compact,
  noMinHeight,
  sometimes,
  readOnly,
  autoedit,
  fillInTheBlank,
  placeholder,
  part,
  onChange,
}) => {
  const editMode = useIsEditable(asset.name) && !readOnly;
  const omegaEdit = useStorySelector(s => s.omegaEdit);
  const divRef = useRef<HTMLDivElement | undefined>(undefined);
  const [focused0, setFocused] = useState(false);
  const [editing0, setEditing] = useState(!!autoedit);
  const [viewSource0, setViewSource] = useState(false);

  const viewSource = viewSource0 || omegaEdit;
  const editing = editing0 || (omegaEdit && editMode);
  const focused = focused0 || (omegaEdit && editing);
  const session = useEditSession(editing);

  const exitEditing = useCallback(() => {
    setEditing(false);
    setViewSource(false);
  }, [editing]);

  useEffect(() => {
    if (!editMode) exitEditing();
  }, [editMode, exitEditing]);

  const editHtml = useCallback(
    (html: string) => onChange({ partType: 'html', html }, session),
    [session, onChange]
  );

  useExitEditingOnOnUnfocused(divRef, editing, focused, exitEditing);
  useExitEditingOnSave(exitEditing);

  const keyHandler = useEscapeToDiscardEdits(session, exitEditing);

  const userCanEdit = useDcmSelector(state => state.layout.userCanEdit);

  return editing || viewSource ? (
    <div
      className={classNames('block-editing', className, { compact })}
      onKeyDown={keyHandler}
      ref={divRef}
      data-editor-id={id}
    >
      {viewSource ? (
        <PartSourceEditor
          id={id}
          asset={asset}
          part={part}
          placeholder={placeholder}
          editing={editing}
          compact={compact}
          noMinHeight={noMinHeight}
          omegaEdit={omegaEdit}
          editHtml={editHtml}
          exitEditing={exitEditing}
        />
      ) : (
        <PartSummerNote
          id={id}
          asset={asset}
          part={part}
          placeholder={placeholder}
          compact={compact}
          fillInTheBlank={fillInTheBlank}
          setFocused={setFocused}
          exitEditing={exitEditing}
          editHtml={editHtml}
        />
      )}
    </div>
  ) : (
    <PartViewer
      id={id}
      asset={asset}
      part={part}
      sometimes={sometimes}
      concurrent={concurrent}
      compact={compact}
      noMinHeight={noMinHeight}
      className={className}
      editMode={editMode}
      userCanEdit={userCanEdit}
      readOnly={readOnly}
      placeholder={placeholder}
      editing={editing}
      setEditing={setEditing}
      setViewSource={setViewSource}
    />
  );
};
