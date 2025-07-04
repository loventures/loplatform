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

import React, { useEffect, useRef, useState } from 'react';

import { useGraphEditSelector, useIsAdded } from '../../graphEdit';
import { isStagedBlob } from '../story';
import { useRevisionCommit } from '../storyHooks';
import { useIFrameResizeMachinery } from './hooks';
import { HtmlEditorAspect } from './util';

// Much copy-pasta from HtmlViewer
export const HtmlPreview: React.FC<
  HtmlEditorAspect & {
    stagedGuid: string;
  }
> = ({ asset: html, contextPath, projectGraph, stagedGuid }) => {
  const branchId = projectGraph.branchId;
  const origins = useGraphEditSelector(state => state.origins);
  const originName = origins[html.name] ?? html.name;
  const isAdded = useIsAdded(originName);
  const commit = useRevisionCommit(); // eesh

  const source = html.data.source;
  const isStaged = isStagedBlob(source);
  const isUnset = !source;
  const sourceParams =
    source && (!isStaged || !stagedGuid) ? `?provider=${source.provider}&name=${source.name}` : '';
  const htmlSrcPrefix = commit
    ? `/api/v2/assets/${branchId}/commits/${commit}/html.1`
    : `/api/v2/assets/${branchId}/html.1`;
  const htmlSrcBase = `${htmlSrcPrefix}/${originName}/serve`;

  const newSrc =
    isUnset || (isStaged && !stagedGuid)
      ? undefined
      : !isStaged
        ? `${htmlSrcBase}${sourceParams}` // I may have reverted to a prior version
        : isAdded
          ? `${htmlSrcPrefix}/serve/${stagedGuid}`
          : `${htmlSrcBase}/${stagedGuid}`;

  const newId = `html-preview-${stagedGuid ?? html.name}`;

  // display previous version until next version is staged
  const [[htmlSrc, iFrameId], setHtmlStuff] = useState([newSrc, newId]);
  useEffect(() => {
    setHtmlStuff(stuff => (newSrc ? [newSrc, newId] : stuff));
  }, [newSrc, newId]);

  const iFrameRef = useRef<HTMLIFrameElement>();
  const height = useIFrameResizeMachinery(iFrameRef, htmlSrc);

  const path = contextPath ? `${contextPath}.${html.name}` : html.name;

  return (
    <iframe
      id={iFrameId}
      key={iFrameId}
      ref={iFrameRef}
      src={htmlSrc}
      data-path={path}
      className="no-exit-edit"
      style={height ? { height: `${height}px` } : undefined}
    />
  );
};
