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
import gretchen from '../../grfetchen/';
import qs from 'qs';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Spinner } from 'reactstrap';

import { beautifyCode } from '../../code/beautifyCode';
import CodeEditor from '../../code/CodeEditor';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useAllEditedOutEdges,
  useGraphEditSelector,
  useIsAdded,
} from '../../graphEdit';
import { useDcmSelector } from '../../hooks';
import {
  derenderLoEdges,
  edgeTargetMap,
  renderLoEdges,
  useCmsInsert,
  useCmsUpload,
} from '../editorUtils';
import { isStagedBlob, useEditSession, useEscapeToDiscardEdits } from '../story';
import { useRemoteEditor } from '../storyHooks';
import { useExitEditingOnOnUnfocused } from './hooks';
import { HtmlEditorAspect } from './util';
import { WordCount } from './WordCount';

// Summernote is different to source edit because you give it an initial value and then
// it maintains its own state
export const HtmlSummernoteEditor: React.FC<
  HtmlEditorAspect & {
    exitEditing: () => void;
  }
> = ({ asset: html, projectGraph, exitEditing }) => {
  const dispatch = useDispatch();
  const branchId = projectGraph.branchId;
  const [focused, setFocused] = useState(false);
  const divRef = useRef<HTMLDivElement | undefined>(undefined);
  const session = useEditSession();
  const origins = useGraphEditSelector(state => state.origins);
  const originName = origins[html.name] ?? html.name;
  const isAdded = useIsAdded(originName);
  const { eBookSupportEnabled } = useDcmSelector(state => state.configuration);

  const source = html.data.source;
  const isStaged = isStagedBlob(source);
  const sourceParams = source && !isStaged ? qs.stringify(source, { addQueryPrefix: true }) : '';
  const htmlSrcPrefix = `/api/v2/authoring/${branchId}/nodes`;
  const htmlSrcBase = `${htmlSrcPrefix}/${originName}/serve`;

  const original = useRef(source);
  const [loaded, setLoaded] = useState(!source || isStagedBlob(source));
  const [initial, setInitial] = useState(isStagedBlob(source) ? source.get() : '');

  const edges = useAllEditedOutEdges(html.name);
  const edgeTargets = useMemo(() => edgeTargetMap(edges), [edges]);

  useEffect(() => {
    if (source && !isStagedBlob(source) && !isAdded)
      gretchen
        .get(`${htmlSrcBase}${sourceParams}`)
        .exec()
        .then(value => {
          setInitial(value);
          setLoaded(true);
        });
  }, [source, isAdded]);

  // If the content doesn't end in a plain <p> then you can't add
  // more content to the page, you can only add inside the final container.
  // So we artifically add a blank paragraph at the end that you can continue
  // in, and we remove it on save.
  const mid = useMemo(
    () => renderLoEdges(initial, branchId, edgeTargets) + '\n<p><br></p>',
    [initial, branchId /*, edgeTargets*/] // ignoring this dep because we don't want to lose edits if another editor adds an edge
  );

  const content = isStagedBlob(source) && source !== original.current ? source.value : mid;

  // We use this staged html construct where we delay doing stuff
  // until get() so individual keystrokes can be relatively lightweight.
  const editHtml = useCallback(
    (value: string) => {
      let memo: string | undefined;
      dispatch(beginProjectGraphEdit('Edit page content', session));
      // get() could do more cleanup if we wished..
      dispatch(
        editProjectGraphNodeData(html.name, {
          source: {
            type: 'text/html',
            value: value,
            get: () => (memo ??= beautifyCode(derenderLoEdges(value, edgeTargets))),
          },
        })
      );
    },
    [session, html.name, edgeTargets]
  );

  // TODO: Preview cannot currently show newly-added staged images because the
  // loEdgeIds do not render. The staged unsaved content should be sent to the
  // server in its rendered form so the URLs are all of a shape that will
  // correctly render.
  const cmsUpload = useCmsUpload(html.name);
  const cmsInsert = useCmsInsert(html.name);

  useExitEditingOnOnUnfocused(divRef, true, focused, exitEditing);

  // save edits on end editing or nav away
  useEffect(() => () => void dispatch(autoSaveProjectGraphEdits()), []);

  const keyHandler = useEscapeToDiscardEdits(session, exitEditing);

  const remoteEditor = useRemoteEditor(html.name, 'html');

  return !loaded ? (
    <div className="edit-html">
      <div className="note-editor note-editable text-muted d-flex align-items-center justify-content-center">
        <Spinner size="sm" />
      </div>
    </div>
  ) : (
    <div
      className="edit-html"
      onKeyDown={keyHandler}
      ref={divRef}
      style={remoteEditor}
    >
      <CodeEditor
        id={`html-editor-${html.name}`}
        mode="htmlmixed"
        size="inline"
        value={content}
        onChange={editHtml}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        lineWrapping
        noCustomTags
        noCustomFiles
        noRareStyles
        findReplacePlugin
        cleanerPlugin
        tabDisable
        resizable
        contentLink={html.name}
        eBookLink={eBookSupportEnabled}
        onImageLink={cmsInsert}
        onImageUpload={cmsUpload}
        contentClass={classNames('default-styling', remoteEditor && 'remote-subedit')}
        doneEditing={exitEditing}
      />
      <WordCount
        html={html}
        content={content}
      />
    </div>
  );
};
