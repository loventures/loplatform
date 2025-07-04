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

import { html as lang_html } from '@codemirror/lang-html';
import { EditorView } from '@codemirror/view';
import CodeMirror from '@uiw/react-codemirror';
import classNames from 'classnames';
import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { AiOutlineCheck } from 'react-icons/ai';
import { FiEye } from 'react-icons/fi';
import { GiVacuumCleaner } from 'react-icons/gi';
import { Button } from 'reactstrap';

import { beautifyCode } from '../../code/beautifyCode';
import { autoSaveProjectGraphEdits, useCurrentAssetName } from '../../graphEdit';
import Textarea from '../../react-textarea-autosize';
import { BlockPart, HtmlPart, NewAsset } from '../../types/asset';
import { useStorySelector } from '../storyHooks';
import { useDispatch } from 'react-redux';
import { isHtmlPart, normalizeBlankHtml } from '../editorUtils.ts';

export const PartSourceEditor: React.FC<{
  id: string;
  asset: NewAsset<any>;
  part: BlockPart | HtmlPart | undefined;
  placeholder: string;
  editing: boolean;
  omegaEdit: boolean;
  compact: boolean;
  noMinHeight: boolean;
  editHtml: (html: string) => void;
  exitEditing: () => void;
}> = ({
  asset,
  part,
  placeholder,
  editing,
  omegaEdit,
  compact,
  noMinHeight,
  editHtml,
  exitEditing,
}) => {
  const dispatch = useDispatch();
  const currentAsset = useCurrentAssetName();
  const simpleEdit = omegaEdit && currentAsset !== asset.name;

  const onSimpleEdit = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => editHtml(e.target.value),
    [editHtml]
  );

  const findCount = useStorySelector(s => s.findCount);

  // CodeMirror only renders text strictly inside the viewport which makes cmd-F
  // useless. CM5 had viewportMargin: Infinity to work around this, but CM6 is too
  // principled and does not allow this. But in print mode it does render everything.
  // So we detect command F and go to print mode (never to leave). We could just
  // always go to print mode but this seems safer?
  // https://discuss.codemirror.net/t/viewport-issues-with-cm-6/3586/6
  const editorRef = useRef<any>(); // any because we are poking inside EditorView
  useEffect(() => {
    // on first render this will be null so we won't always default to print mode
    if (editorRef.current) {
      editorRef.current.viewState.printing = true;
      editorRef.current.measure();
    }
  }, [findCount]);

  const original = useRef(part);

  const initialValue = useMemo(() => {
    const html = isHtmlPart(part)
      ? part.html.trim()
      : (part?.parts
          .map(part => part.html)
          .join('')
          .trim() ?? '');
    return normalizeBlankHtml(html);
  }, [original.current]);

  const value =
    part === original.current
      ? initialValue
      : isHtmlPart(part)
        ? part.html
        : (part?.parts[0]?.html ?? '');

  // save edits on end editing or nav away
  useEffect(
    () => (editing ? () => void dispatch(autoSaveProjectGraphEdits()) : undefined),
    [editing]
  );

  return (
    <>
      <div className="edit-html-button-wrapper d-flex align-items-start">
        {editing && (
          <Button
            key="clean-source"
            color="primary"
            outline
            className="tidy-html-button d-flex p-2 mt-2"
            onClick={() =>
              editHtml(beautifyCode(value.includes('<p') ? value : `<p>${value}</p>`, false))
            }
            title="Tidy HTML"
          >
            <GiVacuumCleaner size="1.1rem" />
          </Button>
        )}
        {!omegaEdit && (
          <Button
            key="view-page"
            color="primary"
            outline
            className="done-edit-button d-flex p-2 mt-2 ms-2"
            onClick={exitEditing}
            title={editing ? 'Done Editing' : 'View Content'}
          >
            {editing ? <AiOutlineCheck size="1.1rem" /> : <FiEye size="1.1rem" />}
          </Button>
        )}
      </div>
      {simpleEdit ? (
        <Textarea
          className="simple-edit form-control html-source"
          value={value}
          readOnly={!editing}
          onChange={editing ? onSimpleEdit : undefined}
          placeholder={placeholder}
        />
      ) : (
        <CodeMirror
          value={value}
          onChange={editing ? editHtml : undefined}
          className={classNames('html-source', (compact || noMinHeight) && 'compact')}
          extensions={[lang_html(), EditorView.lineWrapping]}
          basicSetup={{ searchKeymap: false }}
          autoFocus={!omegaEdit}
          readOnly={!editing}
          onCreateEditor={view => (editorRef.current = view)}
          placeholder={placeholder}
        />
      )}
    </>
  );
};
