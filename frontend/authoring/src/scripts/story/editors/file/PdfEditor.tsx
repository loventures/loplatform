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

import React from 'react';
import { PiFilePdf } from 'react-icons/pi';
import { useDispatch } from 'react-redux';
import { Col, Input, Label, Row } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../../graphEdit';
import { useBranchId } from '../../../hooks';
import authoringApiService from '../../../services/AuthoringApiService';
import { openToast } from '../../../toast/actions';
import { NarrativeEditor } from '../../story';
import { useIsEditable } from '../../storyHooks';
import { serveUrl } from './util';

export const PdfEditor: NarrativeEditor<'pdf.1'> = ({ asset, readOnly }) => {
  const dispatch = useDispatch();
  const branchId = useBranchId();

  const editMode = useIsEditable(asset.name) && !readOnly;

  const onUpload = (file: File) => {
    authoringApiService
      .uploadToBlobstore(asset, file)
      .then(source => {
        dispatch(beginProjectGraphEdit('Upload PDF'));
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

  const url = serveUrl(branchId, asset.name, asset.data.source);
  return (
    <div
      className="my-4"
      style={{ marginLeft: 'calc(1.5rem - 1px)', marginRight: '.5rem' }}
    >
      <div className="d-flex flex-column align-items-center">
        {asset.data.source ? (
          <a
            href={url}
            target="_blank"
            className="mb-4"
          >
            <PiFilePdf size="8rem" />
            <div className="unhover-muted small mt-1">{asset.data.source.filename}</div>
          </a>
        ) : (
          <PiFilePdf
            className="gray-300 mb-4"
            size="8rem"
          />
        )}
      </div>
      {editMode && (
        <Row className="mt-4">
          <Label md={2}>PDF</Label>
          <Col md={10}>
            <Input
              id={`${asset.name}-upload`}
              type="file"
              accept="application/pdf"
              onChange={e => onUpload(e.target.files[0])}
            />
          </Col>
        </Row>
      )}
    </div>
  );
};
