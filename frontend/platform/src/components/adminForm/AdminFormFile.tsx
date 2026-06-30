/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import axios from 'axios';
import classNames from 'classnames';
import Polyglot from 'node-polyglot';
import React, { useState } from 'react';
import { Button, Col, FormFeedback, FormGroup, FormText, Input, Label } from 'reactstrap';
import _ from 'underscore';

import { ContentTypeMultipart } from '../../services';
import { formatSize } from '../../services/formatSize';
import WaitDotGif from '../WaitDotGif';

const uploadsUrl = '/api/v2/uploads';

interface UploadFile {
  guid?: string;
  fileName?: string;
  mimeType?: string;
  size?: number;
  width?: number;
  height?: number;
  [key: string]: unknown;
}

interface AdminFormFileProps {
  defaultUrl?: string;
  entity?: string;
  field?: string;
  image?: boolean;
  accept?: string[];
  label?: boolean;
  labelWidth?: number;
  help?: React.ReactNode;
  fieldUrl?: string;
  invalid?: string;
  onChange?: (state: any) => void;
  onClear?: () => void;
  onReset?: () => void;
  noUpload?: boolean;
  removable?: boolean;
  required?: boolean;
  value?: UploadFile;
  T: Polyglot;
}

const noop = () => '';

const AdminFormFile: React.FC<AdminFormFileProps> = ({
  accept,
  entity,
  field,
  fieldUrl,
  help,
  invalid,
  label = true,
  labelWidth = 2,
  required,
  image,
  T,
  value: propValue,
  noUpload,
  onChange: cb = noop,
  onClear = noop,
  onReset = noop,
}) => {
  const [value, setValue] = useState<UploadFile | null | undefined>(propValue);
  const [guid, setGuid] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  const clearForm = () => {
    onClear();
    setValue(null);
    setGuid('remove');
    setError(null);
  };

  const resetForm = () => {
    onReset();
    setValue(propValue);
    setGuid('');
    setError(null);
  };

  // inexplicably in the production bundle the event that gets passed is the
  // string name of the file and nothing else so we can't use e.target.files
  const onChange = () => {
    const id = `${entity}-${field}`;
    const target = document.getElementById(id) as HTMLInputElement;
    if (noUpload) return cb(target.files?.[0]);
    const formData = new FormData();
    formData.append('upload', target.files![0]);
    setError(null);
    setUploading(true);
    cb({ error: null, uploading: true });
    axios
      .post(uploadsUrl, formData, ContentTypeMultipart)
      .then(r => {
        const data = r.data;
        cb({ value: data, guid: data.guid, uploading: false });
        setValue(data);
        setGuid(data.guid);
        setUploading(false);
      })
      .catch(e => {
        console.log(e);
        cb({ error: T.t('adminForm.file.error.uploadFile'), uploading: false });
        setError(T.t('adminForm.file.error.uploadFile'));
        setUploading(false);
        target.value = '';
      });
  };

  const formatFileInfo = (file: UploadFile, t: Polyglot) => {
    const params = {
      mimeType: file.mimeType,
      size: formatSize(file.size ?? 0, t),
      dimensions: t.t('format.dimensions.pixels', file as any),
    };
    return t.t(
      file.width && file.height ? 'adminForm.file.imageInfo' : 'adminForm.file.fileInfo',
      params
    );
  };

  const id = `${entity}-${field}`;
  const url = guid ? `/api/v2/uploads/${guid}` : `${fieldUrl}/view`;
  const changed = !!guid;
  const problem = error || invalid;
  const acceptProp = !_.isEmpty(accept) ? { accept: accept!.join(',') } : {};
  const name = T.t(`adminPage.${entity}.fieldName.${field}`);
  return (
    <FormGroup
      row
      className={classNames({ 'has-danger': problem, 'is-required': required })}
    >
      {label && (
        <Label
          id={id + '-label'}
          lg={labelWidth}
          for={id}
        >
          {name}
        </Label>
      )}
      <Col lg={label ? 12 - labelWidth : 12}>
        <Input
          type="hidden"
          name={field + 'Upload'}
          value={guid}
        />
        {value && (
          <div className="upload-file-current mb-2">
            {image && (
              <img
                className="upload-file-preview mb-2"
                src={url}
                alt=""
              />
            )}
            <span className="upload-file-info">
              <a
                id={id + '-fileName'}
                className="upload-file-fileName"
                href={url + '?download=true'}
                title={T.t('adminForm.file.downloadFile', { name })}
              >
                {value.fileName}
              </a>
              <span
                id={id + '-fileInfo'}
                className="upload-file-mimeType ms-1 me-2"
              >
                {formatFileInfo(value, T)}
              </span>
            </span>
            {changed ? (
              <Button
                id={id + '-reset'}
                size="sm"
                onClick={resetForm}
                title={T.t('adminForm.file.resetFile', { name })}
              >
                {T.t('adminForm.file.action.reset')}
              </Button>
            ) : (
              <Button
                id={id + '-remove'}
                size="sm"
                onClick={clearForm}
                title={T.t('adminForm.file.removeFile', { name })}
              >
                {T.t('adminForm.file.action.remove')}
              </Button>
            )}
          </div>
        )}
        {changed && !value && (
          <div>
            <span className="upload-file-removeLabel me-2">
              {T.t('adminForm.file.label.removeThisFile')}
            </span>
            <Button
              id={id + '-reset'}
              size="sm"
              onClick={resetForm}
            >
              Reset
            </Button>
          </div>
        )}
        {!changed && (
          <div className="upload-file-new position-relative">
            {uploading && (
              <WaitDotGif
                color="muted"
                size={16}
                style={{ position: 'absolute', top: '15px', right: '11px', zIndex: '1' }}
              />
            )}
            <Input
              className="upload-file-input"
              type="file"
              id={id}
              name={field}
              onChange={onChange}
              {...acceptProp}
              label={T.t('adminForm.file.uploadFile', { name })}
              aria-required={required}
            />
          </div>
        )}
        {problem && (
          <FormFeedback
            id={id + '-problem'}
            style={{ display: 'block' }}
          >
            {problem}
          </FormFeedback>
        )}
        {help && <FormText id={id + '-help'}>{help}</FormText>}
      </Col>
    </FormGroup>
  );
};

export default AdminFormFile;
