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

import typeViewMappings from '../editors/constants/assetTypeMappings.constants';
import { AssetAndIncludes, EdgeWebResponse, StructureWebResponse } from '../types/api';
import { AssetNode, TypeId } from '../types/asset';
import { Edge } from '../types/edge';
import {
  defaultProviderUrl,
  getMd5Hash,
  getMimeFromExtension,
  getPutAttachmentUrl,
  md5HashToObjectKey,
  objectKeyExistsUrl,
} from './blobstoreUtilities';
import { dcmStore } from '../dcmStore';
import { getBranchId } from '../router/ReactRouterService';

class AuthoringApiService {
  private previousRequestAbort: AbortController;

  // TODO: Deal more gracefully with abort signals.
  public fetchAssetAndIncludes(assetName: string, typeId: TypeId): Promise<AssetAndIncludes> {
    const abortController = this.getAbortController();
    return gretchen
      .get(`/api/v2/authoring/${getBranchId()}/nodes/${assetName}`)
      .params({ includeGroup: typeViewMappings[typeId].embed })
      .makeCancellable(abortController)
      .exec();
  }

  public fetchIncomingEdges(target: AssetNode): Promise<EdgeWebResponse> {
    return gretchen
      .get(`/api/v2/authoring/${getBranchId()}/edges`)
      .params({
        options: JSON.stringify({
          direction: 'incoming',
          node: target.name,
        }),
      })
      .exec();
  }

  // requires typeId and data.title.
  public uploadToBlobstore(asset: Partial<AssetNode>, file) {
    const domainId = dcmStore.getState().configuration.domain.id;

    const { typeId } = asset;
    const fileType = file.type === '' ? getMimeFromExtension(file) : file.type;

    const getDefaultProvider = gretchen
      .get(defaultProviderUrl)
      .exec()
      .then(response => response)
      .catch(err => err);

    function objectKeyExists(objectKey) {
      return gretchen
        .post(objectKeyExistsUrl)
        .data({ objectKey })
        .exec()
        .then(() => true) // if this returns successfully the object key already exists
        .catch(() => false); // if this returns unsuccessfully the object key does not already exists
    }

    function blobInfo(defaultProviderName, blobName) {
      return {
        provider: defaultProviderName,
        name: blobName,
        filename: file.name || asset.data.title,
        contentType: fileType,
        size: file.size,
      };
    }

    // get a PUT url to blobstore
    const getPutUrl = blobName => {
      return gretchen
        .post(getPutAttachmentUrl)
        .params({ typeId })
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
          'X-CSRF': 'true',
          'Content-Type': fileType,
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
  }

  public deleteEdge(edge: Edge): Promise<any> {
    return gretchen.delete(`/api/v2/authoring/${getBranchId()}/edges/${edge.name}`).exec();
  }

  public fetchStructure(source: AssetNode): Promise<StructureWebResponse> {
    return gretchen.get(`/api/v2/authoring/${getBranchId()}/nodes/${source.name}/structure`).exec();
  }

  ////

  private getAbortController() {
    this.previousRequestAbort && this.previousRequestAbort.abort();
    this.previousRequestAbort = new AbortController();
    return this.previousRequestAbort;
  }

  // unused functions
  /**
   * fetchAssetById: (id) => gretchen.get(`/api/v2/authoring/nodes/${id}`).exec()
   * */
}

export default new AuthoringApiService();
