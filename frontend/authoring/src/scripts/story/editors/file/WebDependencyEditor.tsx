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

/* eslint-disable react/jsx-no-target-blank */

import React, { useMemo } from 'react';
import { BiUpload } from 'react-icons/bi';
import { useDispatch } from 'react-redux';
import { Input, Label } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useAllEditedOutEdges,
  useEditedAsset,
} from '../../../graphEdit';
import { useBranchId, usePolyglot } from '../../../hooks';
import authoringApiService from '../../../services/AuthoringApiService';
import { openToast } from '../../../toast/actions';
import { NewEdge } from '../../../types/edge';
import { NarrativeEditor, storyTypeName } from '../../story';
import { useIsEditable } from '../../storyHooks';
import { serveUrl } from './util';

export const WebDependencyEditor: NarrativeEditor<'webDependency.1'> = ({ asset, readOnly }) => {
  const editMode = useIsEditable(asset.name) && !readOnly;

  const outEdges = useAllEditedOutEdges(asset.name);
  const scripts = useMemo(() => outEdges.filter(edge => edge.group === 'scripts'), [outEdges]);

  const stylesheets = useMemo(
    () => outEdges.filter(edge => edge.group === 'stylesheets'),
    [outEdges]
  );

  return (
    <div className="mx-3 my-4">
      <h3 className="h4 px-2">Javascripts</h3>
      {scripts.map(edge => (
        <WebDependencyRow
          key={edge.name}
          edge={edge}
          editMode={editMode}
        />
      ))}
      <h3 className="h4 mt-3 px-2">Stylesheets</h3>
      {stylesheets.map(edge => (
        <WebDependencyRow
          key={edge.name}
          edge={edge}
          editMode={editMode}
        />
      ))}
    </div>
  );
};

const WebDependencyRow: React.FC<{
  edge: NewEdge;
  editMode: boolean;
}> = ({ edge, editMode }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const asset = useEditedAsset(edge.targetName);
  const branchId = useBranchId();
  const url = serveUrl(branchId, asset.name, asset.data.source);

  const onUpload = (file: File) => {
    authoringApiService
      .uploadToBlobstore(asset, file)
      .then(source => {
        dispatch(beginProjectGraphEdit('Upload file'));
        dispatch(
          editProjectGraphNodeData(asset.name, {
            source,
          })
        );
        dispatch(autoSaveProjectGraphEdits());
      })
      .catch(e => {
        console.log(e);
        dispatch(openToast('File upload failed.', 'danger'));
      });
  };
  return (
    <div className="d-flex story-index-item px-2 align-items-center">
      <a
        href={url}
        target="_blank"
        className="text-decoration-none flex-grow-1 py-1"
      >
        <span className="text-muted">{storyTypeName(polyglot, asset.typeId) + ' – '}</span>
        <span className="hover-underline">
          {asset.data.title}
          {asset.data.source ? ` (${asset.data.source.filename})` : ''}
        </span>
      </a>
      {editMode && (
        <Label
          className="btn btn-outline-secondary d-flex p-1 border-0 mb-0"
          title="Upload file"
        >
          <Input
            type="file"
            className="d-none"
            accept={edge.group === 'scripts' ? 'text/javascript' : 'text/css'}
            onChange={e => onUpload(e.target.files[0])}
          />
          <BiUpload />
        </Label>
      )}
    </div>
  );
};
