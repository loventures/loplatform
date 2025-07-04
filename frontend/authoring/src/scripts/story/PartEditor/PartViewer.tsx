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
import React, { useMemo } from 'react';
import { FiCode, FiEdit } from 'react-icons/fi';
import { IoCodeSlash } from 'react-icons/io5';
import { Button } from 'reactstrap';
import { BlockPart, HtmlPart, NewAsset } from '../../types/asset';
import { useRemoteEditor } from '../storyHooks';
import {
  edgeTargetMap,
  isBlankHtml,
  isHtmlPart,
  normalizeBlankHtml,
  renderLoEdges,
} from '../editorUtils.ts';
import { useAllEditedOutEdges, useGraphEditSelector } from '../../graphEdit';
import { useProjectGraphSelector } from '../../structurePanel/projectGraphHooks.ts';

export const PartViewer: React.FC<{
  id: string;
  concurrent?: string;
  asset: NewAsset<any>;
  part: BlockPart | HtmlPart | undefined;
  sometimes?: boolean;
  compact: boolean;
  className: string;
  editMode: boolean;
  userCanEdit: boolean;
  readOnly: boolean;
  noMinHeight: boolean;
  placeholder: string;
  editing: boolean;
  setEditing: (e: boolean) => void;
  setViewSource: (v: boolean) => void;
}> = ({
  id,
  concurrent,
  asset,
  part,
  sometimes,
  compact,
  className,
  editMode,
  editing,
  setEditing,
  setViewSource,
  userCanEdit,
  readOnly,
  noMinHeight,
  placeholder,
}) => {
  const remoteEditor = useRemoteEditor(asset.name, concurrent ?? id, editing);

  const branchId = useProjectGraphSelector(state => state.branchId);
  const blobRefs = useGraphEditSelector(state => state.blobRefs)
  const edges = useAllEditedOutEdges(asset.name);
  const value = useMemo(() => {
    const html = isHtmlPart(part)
      ? part.html.trim()
      : (part?.parts
          .map(part => part.html)
          .join('')
          .trim() ?? '');
    return renderLoEdges(normalizeBlankHtml(html), branchId, edgeTargetMap(edges), blobRefs);
  }, [part, branchId, edges, blobRefs]);

  const isBlank = isBlankHtml(value);

  return editMode || !isBlank || !sometimes ? (
    <div
      className={classNames('block-editor', { compact }, className, isBlank && 'is-blank')}
      data-editor-id={id}
    >
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
            title="Edit Content"
          >
            <FiEdit size="1.1rem" />
          </Button>
          <Button
            key="edit-source"
            color="primary"
            outline
            className="edit-source-button d-flex p-2 mt-2"
            onClick={() => {
              setEditing(true);
              setViewSource(true);
            }}
            title="Edit HTML"
          >
            <FiCode size="1.1rem" />
          </Button>
        </div>
      ) : userCanEdit && !readOnly ? (
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
      <div
        className={classNames(
          'rich-content feedback-context clearfix',
          className,
          editMode && 'edit-mode',
          noMinHeight && 'no-min-height',
          remoteEditor && 'remote-edit'
        )}
        style={remoteEditor}
        onClick={editMode && isBlank ? () => setEditing(true) : undefined}
      >
        {isBlank ? (
          <div className="no-html">{placeholder}</div>
        ) : (
          <div
            data-id={id}
            dangerouslySetInnerHTML={{ __html: value }}
          />
        )}
      </div>
    </div>
  ) : null;
};
