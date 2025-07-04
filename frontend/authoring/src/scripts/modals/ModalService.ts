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

import ContentPartTemplateUtils from '../code/contentPartTemplateUtils';
import { FullEdge } from '../types/edge';
import { closeModal, openModal } from './modalActions';
import { ModalIds } from './modalIds';
import { dcmStore } from '../dcmStore';

class ModalService {
  public openAddFileModal(typeId): Promise<string> {
    const { assetEditor } = dcmStore.getState();
    const sourceName = assetEditor.assetNode.name;
    const resources = assetEditor.includes.resources || [];
    return new Promise<string>((resolve, reject) => {
      const modalConfig = {
        typeId,
        sourceName,
        existingEdges: resources.map(e => e.name),
        callback: (newEdge: FullEdge) => {
          const uploadedFileAsset = newEdge.target;
          resources.push(newEdge);
          if (typeId === 'image.1') {
            resolve(ContentPartTemplateUtils.image(uploadedFileAsset));
          } else {
            resolve(ContentPartTemplateUtils.file(uploadedFileAsset));
          }
          dcmStore.dispatch(closeModal());
        },
        failureCallback: reject,
      };

      dcmStore.dispatch(openModal(ModalIds.FileAdd, modalConfig));
    });
  }
}

export default new ModalService();
