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

import axios from 'axios';
import { loConfig } from '../bootstrap/loConfig';
import { uniqueId } from 'lodash';
import { Dispatch, SetStateAction, useCallback, useMemo } from 'react';

export type StagedFile = {
  guid: string;
  fileName: string;
  size: number;
  mimeType: string;
  width: number;
  height: number;
};

export type UploadingFile = { guid: string; progress?: number };

export type IoFile = UploadingFile | StagedFile;

export const isStagedFile = (f: IoFile): f is StagedFile => typeof (f as any).size === 'number';

export type FileUploadApi = {
  stageFile: (file: File, onProgressUpdate: (n: number) => void) => Promise<StagedFile>;
};

export const useFileUploadApi = (): FileUploadApi => {
  const gretchen = axios; //useContext(gretchen);
  return useMemo(
    () => ({
      stageFile: (file: File, onProgressUpdate: (n: number) => void) => {
        //without putting it inside FormData
        //the file will not be sent as a file
        const formData = new FormData();
        formData.append('file', file);
        // loConfig.fileUpload.upload
        return gretchen
          .post<StagedFile>(loConfig.fileUpload.upload, formData, {
            headers: {
              'Content-Type': 'multipart/form-data',
            },
            onUploadProgress: progress => {
              onProgressUpdate(Math.round(progress.loaded / progress.total));
            },
          })
          .then(response => response.data);
      },
    }),
    [gretchen]
  );
};

export const useFileUploader = (setAttachments: Dispatch<SetStateAction<IoFile[]>>) => {
  const uploadApi = useFileUploadApi();
  return useCallback(
    (files: File[]) => {
      const uploading = new Array<UploadingFile>();
      for (const file of files) {
        const guid = uniqueId();
        uploading.push({ guid });
        uploadApi
          .stageFile(file, progress =>
            setAttachments(files => files.map(u => (u.guid === guid ? { guid, progress } : u)))
          )
          .then(uploaded =>
            setAttachments(files => files.map(u => (u.guid === guid ? uploaded : u)))
          )
          .catch(() => setAttachments(file => file.filter(u => u.guid !== guid)));
      }
      setAttachments(files => [...files, ...uploading]);
    },
    [setAttachments]
  );
};
