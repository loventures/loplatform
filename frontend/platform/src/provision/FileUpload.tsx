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
import Polyglot from 'node-polyglot';
import React, { useRef, useState } from 'react';
import { Button, FormText } from 'reactstrap';

import { ContentTypeMultipart } from '../services';

const Uploads = '/api/v2/uploads';

interface FileUploadProps {
  T: Polyglot;
  id: string;
  defImgSrc: string;
  updateFormData: (guid: string) => void;
  setErrors: (errors: string[]) => void;
}

const FileUpload: React.FC<FileUploadProps> = ({ T, id, defImgSrc, updateFormData, setErrors }) => {
  const [imgSrc, setImgSrc] = useState(defImgSrc);

  const updateSrc = (guid: string) => {
    setImgSrc(`${Uploads}/${guid}`);
    updateFormData(guid);
  };

  const file = useRef<HTMLInputElement>(null);
  const fileChanged = (files: FileList | null) => {
    if (files && files.length > 0) {
      const data = new FormData();
      data.append('upload', files[0]);
      axios
        .post(Uploads, data, ContentTypeMultipart)
        .then(({ data: { guid } }) => updateSrc(guid))
        .catch(() => setErrors([T.t('adminForm.file.error.uploadFile')]));
    }
  };

  const promptUrl = (prompt: string) => {
    let url = window.prompt(prompt) as string;
    if (!url.startsWith('http')) {
      url = `http://${url}`;
      if (!url.endsWith('/')) url = `${url}/`;
    }
    if (url.endsWith('/') && id === 'favicon') url = `${url}favicon.ico`;
    axios
      .post(`/api/v2/uploads/fetch?url=${url}`)
      .then(({ data }) => {
        if (!data.mimeType.startsWith('image/')) {
          setErrors([T.t('adminPage.provision.error.download.wrongFileType', { url })]);
        } else {
          updateSrc(data.guid);
        }
      })
      .catch(() => setErrors([T.t('adminPage.provision.error.download', { url })]));
  };

  const label = T.t(`adminPage.provision.field.${id}.label`);
  const prompt = T.t(`adminPage.provision.field.${id}.prompt`);
  return (
    <div className="mt-2">
      <Button
        id={`${id}-upload-btn`}
        className="btn-light me-2"
        onClick={() => file.current?.click()}
      >
        {T.t('adminPage.provision.step2.uploadFile')}
      </Button>
      <Button
        id={`${id}-enterUrl-btn`}
        className="btn-light me-2"
        onClick={() => promptUrl(prompt)}
      >
        {T.t('adminPage.provision.step2.enterURL')}
      </Button>
      <img
        className="upload-file-preview"
        alt={label}
        id={`${id}-img`}
        src={imgSrc}
      />
      <FormText> {label} </FormText>
      <input
        ref={file}
        id={`${id}-file`}
        hidden={true}
        type="file"
        onChange={ev => fileChanged(ev.target.files)}
        accept=".jpg,.gif,.png,.ico"
      />
      {/*TODO check what I can accept*/}
    </div>
  );
};

export default FileUpload;
