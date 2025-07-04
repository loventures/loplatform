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
import { BiLink } from 'react-icons/bi';
import { IoTrashOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Badge, Button, Label } from 'reactstrap';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  useAllEditedOutEdges,
  useEditedAsset,
} from '../../graphEdit';
import { useBranchId, usePolyglot } from '../../hooks';
import { openToast, TOAST_TYPES } from '../../toast/actions';
import { NewAsset } from '../../types/asset';
import { NewEdge } from '../../types/edge';
import { editorUrl, storyTypeName } from '../story';
import { useIsEditable } from '../storyHooks';
import { useCollapse } from 'react-collapsed';
import { ChevronPlain } from '../icons/ChevronPlain.tsx';
import { QuotedLoEdgeRE } from '../editorUtils.ts';
import { HtmlEditorAspect } from './util.ts';
import { useHtmlSource } from './HtmlSourceEditor.tsx';
import { serveUrl } from '../editors/file/util.ts';

const FakeHtml: NewAsset<'html.1'> = {
  name: '',
  typeId: 'html.1',
  data: {
    source: null,
  },
} as any;

// TODO: This should link to a separate page that shows the resources because
// it cannot correctly determine that newly-added images are used in the
// edited HTML source and so flags them as unused.
export const HtmlResources: React.FC<HtmlEditorAspect> = ({
  asset: html,
  contextPath,
  readOnly,
  projectGraph,
}) => {
  const outEdges = useAllEditedOutEdges(html.name);
  const editMode = useIsEditable(html.name) && !readOnly;
  const resources = useMemo(
    () => outEdges.filter(edge => edge.group === 'resources' || edge.group === 'dependencies'),
    [outEdges]
  );
  const [isExpanded, setExpanded] = useState(false);
  const onExpand = useCallback(() => setExpanded(s => !s), []);
  const { getCollapseProps } = useCollapse({
    defaultExpanded: false,
    isExpanded,
  });

  const { value } = useHtmlSource(isExpanded ? html : FakeHtml, projectGraph);
  const used = useMemo(
    () => new Set([...value.matchAll(QuotedLoEdgeRE)].map(match => match[1] ?? '')),
    [value]
  );

  return resources.length ? (
    <div className="mx-3 mt-4 no-exit-edit">
      <h3 className="h5 pe-2 d-flex align-items-end">
        <Button
          id={`resources-${html.name}`}
          className="p-0 d-flex br-50 me-1"
          color="transparent"
          onClick={onExpand}
        >
          <ChevronPlain
            size="1.25rem"
            style={{
              transform: `rotate(${isExpanded ? 90 : 0}deg)`,
              transition: 'transform .1s ease-in',
            }}
          />
        </Button>
        <Label
          className="mb-0"
          htmlFor={`resources-${html.name}`}
        >
          Attachments ({resources.length})
        </Label>
      </h3>
      <div {...getCollapseProps()}>
        {resources.map(edge =>
          edge.group === 'resources' ? (
            <HtmlResourceRow
              key={edge.name}
              edge={edge}
              editMode={editMode}
              contextPath={contextPath}
              used={used.has(edge.edgeId)}
            />
          ) : (
            <WebDependencyRow
              key={edge.name}
              edge={edge}
              contextPath={contextPath}
              editMode={editMode}
            />
          )
        )}
      </div>
    </div>
  ) : null;
};

const HtmlResourceRow: React.FC<{
  edge: NewEdge;
  contextPath: string;
  editMode: boolean;
  used: boolean;
}> = ({ edge, contextPath, editMode, used }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const target = useEditedAsset(edge.targetName);
  const branchId = useBranchId();
  const url = serveUrl(branchId, edge.targetName);

  const copyEmbedCode = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      const clipBoard = navigator.clipboard;
      clipBoard.writeText(url).then(() => {
        dispatch(openToast('Link URL copied.', TOAST_TYPES.SUCCESS));
      });
    },
    [url]
  );

  const deleteResource = useCallback(() => {
    dispatch(beginProjectGraphEdit('Delete HTML resource'));
    dispatch(deleteProjectGraphEdge(edge));
    dispatch(autoSaveProjectGraphEdits());
  }, [edge]);

  return (
    <div className="d-flex story-index-item px-2 align-items-center edit-mode">
      <Link
        to={editorUrl('story', branchId, target.name, `${contextPath}.${edge.sourceName}`)}
        className="text-decoration-none flex-grow-1 py-1"
      >
        <span className="text-muted">{storyTypeName(polyglot, target.typeId) + ' – '}</span>
        <span className="hover-underline">{target.data.title}</span>
        {!used && (
          <Badge
            color="warning"
            className="text-dark ms-2"
          >
            Unused
          </Badge>
        )}
      </Link>
      <div className="controls d-flex">
        <a
          href={url}
          onClick={copyEmbedCode}
          className="btn btn-outline-secondary d-flex p-1 border-0"
          title="Copy Image URL"
        >
          <BiLink />
        </a>
        {editMode && (
          <Button
            outline
            color="danger"
            onClick={deleteResource}
            className="d-flex p-1 border-0"
            title="Delete Resource"
          >
            <IoTrashOutline />
          </Button>
        )}
      </div>
    </div>
  );
};

const WebDependencyRow: React.FC<{
  edge: NewEdge;
  contextPath: string;
  editMode: boolean;
}> = ({ edge, contextPath, editMode }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const target = useEditedAsset(edge.targetName);
  const branchId = useBranchId();

  const deleteResource = useCallback(() => {
    dispatch(beginProjectGraphEdit('Delete HTML resource'));
    dispatch(deleteProjectGraphEdge(edge));
    dispatch(autoSaveProjectGraphEdits());
  }, [edge]);

  return (
    <div className="d-flex story-index-item px-2 align-items-center edit-mode">
      <Link
        to={editorUrl('story', branchId, target.name, `${contextPath}.${edge.sourceName}`)}
        className="text-decoration-none flex-grow-1 py-1"
      >
        <span className="text-muted">{storyTypeName(polyglot, target.typeId) + ' – '}</span>
        <span className="hover-underline">{target.data.title}</span>
      </Link>
      <div className="controls d-flex">
        {editMode && (
          <Button
            outline
            color="danger"
            onClick={deleteResource}
            className="d-flex p-1 border-0"
            title="Delete Resource"
          >
            <IoTrashOutline />
          </Button>
        )}
      </div>
    </div>
  );
};
