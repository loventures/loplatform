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
import htmldiff from 'node-htmldiff';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { FiCode, FiEdit } from 'react-icons/fi';
import { IoCodeSlash } from 'react-icons/io5';
import VisibilitySensor from 'react-visibility-sensor';
import { Button } from 'reactstrap';

import { useGraphEditSelector, useIsAdded } from '../../graphEdit';
import { useDcmSelector } from '../../hooks';
import { useContentAccess } from '../hooks/useContentAccess';
import { isStagedBlob } from '../story';
import {
  useDiffCommit,
  useIsStoryEditMode,
  useRemoteEditor,
  useRevisionCommit,
  useStorySelector,
} from '../storyHooks';
import { useIFrameResizeMachinery } from './hooks';
import { HtmlEditorAspect } from './util';
import { WordCount } from './WordCount';
import { isBlankHtml } from '../editorUtils.ts';

// We can't truly show a *reverted* HTML asset in the unsaved preview mode because
// we will not use the historic state of the HTML asset web resources

export const HtmlViewer: React.FC<
  HtmlEditorAspect & {
    setEditing: (editing: boolean) => void;
    setViewSource: (viewSource: boolean) => void;
    stagedGuid: string;
  }
> = ({
  asset: html,
  contextPath,
  mode,
  projectGraph,
  readOnly,
  setEditing,
  setViewSource,
  stagedGuid,
}) => {
  const branchId = projectGraph.branchId;
  const [appeared, setAppeared] = useState(false);
  const activeIFrame = useStorySelector(state => state.activeIFrame);
  const contentAccess = useContentAccess(html.name);
  const editMode = useIsStoryEditMode() && !readOnly && contentAccess.EditContent;
  const origins = useGraphEditSelector(state => state.origins);
  const saving = useGraphEditSelector(state => state.saving);
  const originName = origins[html.name] ?? html.name;
  const isAdded = useIsAdded(originName);
  const commit = useRevisionCommit(); // eesh
  const diffCommit = useDiffCommit();
  const userCanEdit = useDcmSelector(state => state.layout.userCanEdit);
  const remoteEditor = useRemoteEditor(html.name, 'html');

  const source = html.data.source;
  const isUnset = !source;
  const isStaged = isStagedBlob(source);
  const sourceParams =
    source && !isStaged ? `?provider=${source.provider}&name=${source.name}` : '';
  const htmlSrcPrefix = commit
    ? `/api/v2/assets/${branchId}/commits/${commit}/html.1`
    : `/api/v2/assets/${branchId}/html.1`;
  const htmlSrcBase = `${htmlSrcPrefix}/${originName}/serve`;

  const [loaded, setLoaded] = useState('');
  const [original, setOriginal] = useState<string>(isStaged ? source.value : '');
  const [versus, setVersus] = useState('');
  const value = isStaged ? source.value : original;
  // We can't flag an unloaded document as blank because the resulting iframe url change breaks browser back
  const isBlank = isBlankHtml(value) && (isStaged || isUnset || !!original);

  const htmlSrc =
    !appeared || isUnset || isBlank || (isStaged && (saving || !stagedGuid))
      ? undefined
      : !isStaged
        ? `${htmlSrcBase}${sourceParams}` // I may have reverted to a prior version
        : isAdded
          ? `${htmlSrcPrefix}/serve/${stagedGuid}`
          : `${htmlSrcBase}/${stagedGuid}`;

  const iFrameId = `html-iframe-${stagedGuid ?? html.name}`;
  const iframeActive = iFrameId === activeIFrame;

  const iFrameRef = useRef<HTMLIFrameElement>();
  const height = useIFrameResizeMachinery(iFrameRef, htmlSrc);

  useEffect(() => {
    const url = commit
      ? `/api/v2/authoring/${branchId}/commits/${commit}/nodes/${originName}/serve`
      : `/api/v2/authoring/${branchId}/nodes/${originName}/serve`;
    if (appeared && loaded !== url) {
      setLoaded(url);
      // TODO: there's a bug here where it fetches the asset before it has been uploaded..
      // scenario appears to be create page, paste content, click checkbox
      if (!isAdded && source) {
        gretchen
          .get(url)
          .exec()
          .then(r => {
            // TODO: this is garbage but if the page hasn't been uploaded per bug above, the response
            // is 204 no content which gives a success {} response here, not just empty text.
            if (typeof r === 'string') setOriginal(r);
          });
      }
    }
  }, [commit, loaded, appeared, isAdded]);

  useEffect(() => {
    if (mode === 'revision' && diffCommit) {
      gretchen
        .get(`/api/v2/authoring/${branchId}/commits/${diffCommit}/nodes/${originName}/serve`)
        .exec()
        .then(setVersus);
    }
  }, [mode, diffCommit, originName]);

  useEffect(() => {
    const messageListener = (e: MessageEvent) => {
      if (e.data?.fn === 'highlightElement' && e.data?.name === html.name) {
        iFrameRef.current?.contentWindow?.postMessage(e.data, '*');
      }
    };
    window.addEventListener('message', messageListener);
    return () => {
      window.removeEventListener('message', messageListener);
    };
  }, [html.name]);

  const diff = useMemo(() => original && versus && htmldiff(versus, original), [original, versus]);

  const path = contextPath ? `${contextPath}.${html.name}` : html.name;

  const css = useMemo(
    () =>
      remoteEditor || height
        ? {
            ...(remoteEditor ?? {}),
            ...(height ? { height: `${height}px` } : {}),
          }
        : undefined,
    [height, remoteEditor]
  );

  return (
    <VisibilitySensor
      partialVisibility
      onChange={visible => setAppeared(v => v || visible)}
    >
      <div className="view-html">
        {editMode ? (
          <div className="edit-html-button-wrapper d-flex align-items-start">
            <Button
              key="edit-html"
              color="primary"
              outline
              className="edit-html-button d-flex p-2 mt-2 me-2"
              onClick={() => {
                setEditing(true);
                setViewSource(false);
              }}
              title="Edit Page"
            >
              <FiEdit size="1.1rem" />
            </Button>
            <Button
              key="edit-source"
              color="primary"
              outline
              className="edit-src-button d-flex p-2 mt-2"
              onClick={() => {
                setEditing(true);
                setViewSource(true);
              }}
              title="Edit HTML"
            >
              <FiCode size="1.1rem" />
            </Button>
          </div>
        ) : userCanEdit && !diffCommit && !readOnly ? (
          <div className="edit-html-button-wrapper">
            <Button
              key="view-source"
              color="primary"
              outline
              className="view-source-button d-flex p-2 mt-2"
              onClick={() => setViewSource(true)}
              title="View Source"
            >
              <IoCodeSlash size="1.1rem" />
            </Button>
          </div>
        ) : null}
        {diffCommit ? (
          <div
            className="default-styling difference"
            dangerouslySetInnerHTML={{ __html: diff }}
          />
        ) : isUnset || isBlank ? (
          <div
            id={iFrameId}
            key={iFrameId}
            className={classNames(
              'html-editor d-flex align-items-center justify-content-center text-muted is-blank',
              editMode && 'edit-mode',
              remoteEditor && 'remote-edit'
            )}
            style={css}
            onClick={editMode ? () => setEditing(true) : undefined}
          >
            Page content
          </div>
        ) : (
          <iframe
            id={iFrameId}
            key={iFrameId}
            ref={iFrameRef}
            src={htmlSrc}
            data-path={path}
            className={classNames(
              'html-editor',
              editMode && 'edit-mode',
              iframeActive && 'iframe-active',
              remoteEditor && 'remote-edit'
            )}
            style={css}
          />
        )}
        {!readOnly && !diffCommit /* don't show in preview */ && (
          <WordCount
            html={html}
            content={value}
          />
        )}
      </div>
    </VisibilitySensor>
  );
};
