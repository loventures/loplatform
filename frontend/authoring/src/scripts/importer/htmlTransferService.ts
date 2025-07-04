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

import gretchen, { fileApi } from '../grfetchen/';

export type StagedFile = {
  guid: string;
  fileName: string;
  size: number;
  mimeType: string;
  width: number;
  height: number;
};

//grfetchen sends `file.name` as a `X-Filename` header which only supports ASCII so breaks
export const uploadToCampusPack = (file: File): Promise<string> =>
  fileApi
    .post(`/api/v2/uploads?fileName=${encodeURIComponent(file.name)}`)
    .headers({ 'X-Filename': '' })
    .params({})
    .file(file)
    .exec()
    .then(({ guid }) => guid);

export const deleteFromCampusPack = (guid: string): Promise<void> =>
  gretchen.delete('/api/v2/uploads/:guid').params({ guid }).exec();

// because gretchen is so bad, why not axios...
export const uploadToCampusPack2 = (
  file: Blob,
  onProgressUpdate: (n: number) => void
): Promise<StagedFile> => {
  const xhr = new XMLHttpRequest();
  return new Promise((resolve, reject) => {
    xhr.upload.addEventListener('progress', event => {
      if (event.lengthComputable) {
        onProgressUpdate(Math.round(event.loaded / event.total));
      }
    });
    xhr.addEventListener('loadend', () => {
      if (xhr.readyState === 4 && xhr.status === 200) {
        resolve(xhr.response);
      } else {
        reject(xhr);
      }
    });
    xhr.responseType = 'json';
    xhr.withCredentials = true;
    const qs = file instanceof File ? `?fileName=${encodeURIComponent(file.name)}` : '';
    xhr.open('POST', `/api/v2/uploads${qs}`, true);
    xhr.setRequestHeader('Content-Type', file.type);
    xhr.setRequestHeader('X-CSRF', 'true');
    xhr.send(file);
  });
};

export const htmlZipExportUrl = (branchId: number, name: string, commit?: number): string =>
  commit
    ? `/api/v2/authoring/${branchId}/commits/${commit}/asset/${name}/export/html.zip`
    : `/api/v2/authoring/${branchId}/asset/${name}/export/html.zip`;

/** Returns a list of the titles of modified assets. */
export const validateHtmlZipImport = (
  branchId: number,
  name: string,
  guid: string
): Promise<Array<string>> =>
  gretchen
    .get('/api/v2/authoring/:branchId/asset/:name/import/html?upload=:guid')
    .params({ branchId, name, guid })
    .exec();

/** Returns the number of assets modified. */
export const performHtmlZipImport = (
  branchId: number,
  name: string,
  guid: string
): Promise<number> =>
  gretchen
    .post('/api/v2/authoring/:branchId/asset/:name/import/html?upload=:guid')
    .params({ branchId, name, guid })
    .exec();

export const htmlDocExportUrl = (branchId: number, name: string, commit?: number): string =>
  commit
    ? `/api/v2/authoring/${branchId}/commits/${commit}/asset/${name}/export/doc.html`
    : `/api/v2/authoring/${branchId}/asset/${name}/export/doc.html`;

export const validateHtmlDocImport = (
  branchId: number,
  module: string,
  guid: string
): Promise<{
  warnings?: string[];
  errors?: string[];
  added?: string[];
  modified?: string[];
  alignmentsAdded?: number;
  alignmentsRemoved?: number;
}> =>
  gretchen
    .get(`/api/v2/authoring/:branchId/asset/:module/import/document?upload=:guid`)
    .params({ branchId, guid, module })
    .exec();

export const performHtmlDocImport = (
  branchId: number,
  module: string,
  guid: string
): Promise<void> =>
  gretchen
    .post(`/api/v2/authoring/:branchId/asset/:module/import/document?upload=:guid`)
    .params({ branchId, guid, module })
    .exec();
