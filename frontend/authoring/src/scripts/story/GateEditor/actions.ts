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

import {
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  getEditedAsset,
} from '../../graphEdit';
import { GateContentModalData } from '../../modals/GateContentModal';
import { openModal } from '../../modals/modalActions';
import { ModalIds } from '../../modals/modalIds';
import { NodeName } from '../../types/asset';
import { Thunk } from '../../types/dcmState';
import { EdgeGroup, NewEdge } from '../../types/edge';
import { plural } from '../story';
import { setNarrativeAssetState } from '../storyActions';

export const addContentGateAction =
  (name: NodeName, group: EdgeGroup): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const asset = getEditedAsset(name, projectGraph, graphEdits);

    const modalData: GateContentModalData = {
      parent: asset,
      group,
      callback: names => {
        const typeStr = group === 'testsOut' ? 'test out' : 'content gate';
        dispatch(beginProjectGraphEdit('Add ' + plural(names.length, typeStr)));
        for (const target of names) {
          const newEdge: NewEdge = {
            name: crypto.randomUUID(),
            sourceName: name,
            targetName: target,
            group,
            data: {
              performanceGate: { threshold: 0.8 },
            },
            traverse: false,
            newPosition: 'end',
          };
          dispatch(addProjectGraphEdge(newEdge));
        }
        dispatch(autoSaveProjectGraphEdits());
        dispatch(setNarrativeAssetState(name, { gated: true }));
        setTimeout(() => dispatch(setNarrativeAssetState(name, { gated: undefined })), 500);
      },
    };
    dispatch(openModal(ModalIds.GateContentModal, modalData));
  };
