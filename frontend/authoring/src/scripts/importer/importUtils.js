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

import gretchen from '../grfetchen/';
import { sum } from 'lodash';

import {
  defaultProviderUrl,
  getGenericPutAttachmentUrl,
  getMd5Hash,
  md5HashToObjectKey,
  objectKeyExistsUrl,
} from '../services/blobstoreUtilities';

export const requestQtiImportPreview = (file, branchId, domainId, importType) =>
  putImportFileInBlobstore(file, 'application/zip', domainId).then(blobRef =>
    gretchen
      .post('/api/v2/authoring/:branchId/import/qti/validate')
      .params({ branchId })
      .data({
        unconvertedSource: blobRef,
        type: importType,
      })
      .exec()
  );

export const putImportFileInBlobstore = (file, fileType, domainId) => {
  // get the blobstore name for the domain
  // response = { name, container }
  const getDefaultProvider = gretchen
    .get(defaultProviderUrl)
    .exec()
    .then(response => response)
    .catch(err => err);

  // do a headObject check with the blobstore to see if a selected file already exists
  function objectKeyExists(objectKey) {
    return gretchen
      .post(objectKeyExistsUrl)
      .data({
        objectKey,
      })
      .exec()
      .then(() => {
        // if this returns successfully the object key already exists
        return true;
      })
      .catch(() => {
        // if this returns unsuccessfully the object key does not already exists
        return false;
      });
  }

  // data.source blobInfo object
  function blobInfo(defaultProviderName, blobName) {
    return {
      provider: defaultProviderName,
      name: blobName,
      filename: file.name,
      contentType: fileType,
      size: file.size,
    };
  }

  // get a PUT url to blobstore
  const getPutUrl = blobName => {
    return gretchen
      .post(getGenericPutAttachmentUrl)
      .data({
        blobName,
        mediaType: fileType,
        contentLength: file.size,
      })
      .exec()
      .then(({ location }) => location);
  };

  // PUT a file in blobstore
  function putFileInBlobstore(putLocation) {
    return window.fetch(putLocation, {
      credentials: 'include',
      method: 'PUT',
      headers: {
        'X-CSRF': true,
        'Content-Type': file.type,
      },
      body: file,
    });
  }

  return Promise.all([getDefaultProvider, getMd5Hash(file)]).then(([defaultProvider, md5]) => {
    const { name } = defaultProvider;
    const objectKey = md5HashToObjectKey(domainId, md5);

    const blobData = blobInfo(name, objectKey);

    return objectKeyExists(objectKey).then(objectKeyExists => {
      if (objectKeyExists) {
        return blobData;
      } else {
        return getPutUrl(objectKey)
          .then(location => putFileInBlobstore(location))
          .then(() => blobData);
      }
    });
  });
};

export const createPreview = (typeIdsToSize, polyglot, destination, itemName) => {
  const itemMessage = () => {
    const itemCount = sum(Object.values(typeIdsToSize));
    const itemTypeCount = `(${Object.entries(typeIdsToSize)
      .map(([typeId, size]) => `${size} ${polyglot.t(typeId)}`)
      .join(', ')})`;
    return `${itemCount} ${polyglot.t(
      `${itemName}${itemCount !== 1 ? '-plural' : ''}`
    )} ${itemTypeCount}`;
  };
  return `${polyglot.t('ADD')} ${itemMessage()} ${polyglot.t('IMPORT_MODAL.TO')} ${destination}.`;
};

export const pollForTaskReport = (importReceiptId, taskReportId, onEachPoll = () => {}) =>
  new Promise((resolve, reject) => {
    const intervalId = setInterval(() => {
      gretchen
        .get('/api/v2/taskReports/:id')
        .params({ id: taskReportId })
        .exec()
        .then(report => {
          const { status } = report;
          onEachPoll(report);
          if (status === 'SUCCESS') {
            clearInterval(intervalId);
            gretchen
              .get('/api/v2/authoring/imports/:id')
              .params({ id: importReceiptId })
              .exec()
              .then(resp => {
                resolve(resp);
              });
          } else if (status === 'FAILURE') {
            clearInterval(intervalId);
            reject(report);
          }
        });
    }, 1500);
  });
