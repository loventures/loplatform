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

import BMF from 'browser-md5-file';

export const defaultProviderUrl = '/api/v2/authoring/defaultProvider';

export const objectKeyExistsUrl = '/api/v2/authoring/objectKeyExists';

export const createAssetWithBlobDataUrl = '/api/v2/authoring/:branchId/nodes';

export const getPutAttachmentUrl = '/api/v2/authoring/:typeId/attachmentUrl';

export const getGenericPutAttachmentUrl = '/api/v2/authoring/attachmentUrl';

// get the MD5 hash of a file
export function getMd5Hash(file) {
  return new Promise((resolve, reject) => {
    new BMF().md5(file, function (err, md5) {
      resolve(md5);
      reject(err);
    });
  });
}

// turn an md5 hash into an objectKey for an LO bucket
export function md5HashToObjectKey(domainId, md5) {
  return `authoring/${domainId}/${md5[0]}/${md5[1]}/${md5[2]}/${md5.slice(3)}`;
}

export function getMimeFromExtension(file) {
  const extPattern = /\.[0-9a-z]{1,5}$/i;
  const extention = extPattern.exec(file.name);

  switch (extention[0]) {
    case '.eot':
      return 'application/vnd.ms-fontobject';
    case '.otf':
      return 'application/x-font-opentype';
    case '.ttf':
      return 'application/x-font-ttf';
    case '.woff':
      return 'application/font-woff';
    case '.woff2':
      return 'application/font-woff2';
    case '.svg':
      return 'image/svg+xml';
    case '.vtt':
      return 'application/octet-stream';
    default:
      return 'application/octet-stream';
  }
}
