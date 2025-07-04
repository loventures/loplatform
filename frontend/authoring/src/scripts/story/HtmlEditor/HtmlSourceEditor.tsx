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
import gretchen from '../../grfetchen/';
import qs from 'qs';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AiOutlineCheck } from 'react-icons/ai';
import { FiEye } from 'react-icons/fi';
import { GiVacuumCleaner } from 'react-icons/gi';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { beautifyCode } from '../../code/beautifyCode';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useCurrentAssetName,
  useGraphEditSelector,
  useIsAdded,
} from '../../graphEdit';
import Textarea from '../../react-textarea-autosize';
import { isStagedBlob, useEditSession, useEscapeToDiscardEdits } from '../story';
import { useRemoteEditor, useRevisionCommit, useStorySelector } from '../storyHooks';
import { useExitEditingOnOnUnfocused } from './hooks';
import { HtmlEditorAspect } from './util';
import { WordCount } from './WordCount';
import { NewAsset } from '../../types/asset';
import { ProjectGraph } from '../../structurePanel/projectGraphReducer.ts';

export const useHtmlSource = (html: NewAsset<'html.1'>, projectGraph: ProjectGraph) => {
  const branchId = projectGraph.branchId;
  const commit = useRevisionCommit();
  const origins = useGraphEditSelector(state => state.origins);
  const originName = origins[html.name] ?? html.name;
  const isAdded = useIsAdded(originName);

  const source = html.data.source;
  const isStaged = isStagedBlob(source);
  const sourceParams = source && !isStaged ? qs.stringify(source, { addQueryPrefix: true }) : '';
  const htmlSrcPrefix = commit
    ? `/api/v2/authoring/${branchId}/commits/${commit}/nodes`
    : `/api/v2/authoring/${branchId}/nodes`;
  const htmlSrcBase = `${htmlSrcPrefix}/${originName}/serve`;

  const [loaded, setLoaded] = useState(!source || isStagedBlob(source));
  const [value, setValue] = useState(isStagedBlob(source) ? source.get() : '');

  useEffect(() => {
    if (source && !isStaged && !isAdded)
      gretchen
        .get(`${htmlSrcBase}${sourceParams}`)
        .exec()
        .then(value => {
          setValue(value);
          setLoaded(true);
        });
  }, [source, isAdded]);

  return { loaded, value, setValue };
};

export const HtmlSourceEditor: React.FC<
  HtmlEditorAspect & {
    editing: boolean;
    exitEditing: () => void;
  }
> = ({ asset: html, projectGraph, editing, exitEditing }) => {
  const dispatch = useDispatch();
  const divRef = useRef<HTMLDivElement | undefined>(undefined);
  const session = useEditSession();
  const [focused, setFocused] = useState(true);
  const omegaEdit = useStorySelector(s => s.omegaEdit);
  const findCount = useStorySelector(s => s.findCount);
  const currentAsset = useCurrentAssetName();
  const simpleEdit = omegaEdit && currentAsset !== html.name;

  const { loaded, value, setValue } = useHtmlSource(html, projectGraph);

  const editHtml = useCallback(
    (value: string) => {
      dispatch(beginProjectGraphEdit('Edit page content', session));
      dispatch(
        editProjectGraphNodeData(html.name, {
          source: {
            type: 'text/html',
            value: value,
            get: () => value,
          },
        })
      );
      setValue(value);
    },
    [session, html.name]
  );

  const onSimpleEdit = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => editHtml(e.target.value),
    [editHtml]
  );

  const tidyHtml = () => {
    const pretty = beautifyCode(value);
    editHtml(pretty);
  };

  useExitEditingOnOnUnfocused(divRef, editing, focused, exitEditing);

  // save edits on end editing or nav away
  useEffect(
    () => (editing ? () => void dispatch(autoSaveProjectGraphEdits()) : undefined),
    [editing]
  );

  const keyHandler = useEscapeToDiscardEdits(session, exitEditing);

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

  const onCreateEditor = useCallback(
    (view: EditorView) => {
      editorRef.current = view;
      // for automation...
      (view.dom as any).parentNode.CodeMirror = {
        getValue: () => view.state.doc.toString(),
        setValue: editHtml,
      };
    },
    [editHtml]
  );

  const remoteEditor = useRemoteEditor(html.name, 'html');

  return (
    <div
      className="edit-html"
      onKeyDown={keyHandler}
      ref={divRef}
      style={remoteEditor}
    >
      <div className="edit-html-button-wrapper d-flex align-items-start">
        {editing && (
          <Button
            key="clean-source"
            color="primary"
            outline
            className="tidy-html-button d-flex p-2 mt-2"
            onClick={tidyHtml}
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
          className={classNames(
            'simple-edit form-control html-source',
            remoteEditor && 'remote-edit'
          )}
          value={value}
          readOnly={!editing}
          onChange={editing ? onSimpleEdit : undefined}
        />
      ) : (
        <CodeMirror
          value={value}
          onChange={editing ? editHtml : undefined}
          className={classNames('html-source', remoteEditor && 'remote-edit')}
          extensions={[lang_html(), EditorView.lineWrapping]}
          autoFocus
          readOnly={!editing || !loaded}
          onBlur={() => setFocused(false)}
          onFocus={() => setFocused(true)}
          basicSetup={{ searchKeymap: false }}
          onCreateEditor={onCreateEditor}
        />
      )}
      <WordCount
        html={html}
        content={value}
      />
    </div>
  );
};
