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

import { replace } from 'connected-react-router';

import { trackAuthoringEvent } from '../../analytics';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedOutEdges,
  deleteProjectGraphEdge,
  editProjectGraphNodeData,
  getCopiedAsset,
  getEditedAsset,
} from '../../graphEdit';
import { openToast } from '../../toast/actions';
import { NodeName } from '../../types/asset';
import { Thunk } from '../../types/dcmState';
import { isQuestion } from '../questionUtil';
import { childEdgeGroup, editorUrl } from '../story';
import { setNarrativeAssetState, setNarrativeState } from '../storyActions';

export const copyAssetAction =
  (name: NodeName): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const self = getEditedAsset(name, projectGraph, graphEdits);
    const copy = getCopiedAsset(self.name, projectGraph, graphEdits);
    const title = isQuestion(self.typeId) ? 'question' : self.data.title;
    trackAuthoringEvent(`Narrative Editor - Copy`, self.typeId);
    dispatch(openToast(`Copied ${title}.`, 'success'));
    dispatch(setNarrativeState({ clipboard: copy }));
  };

export const removeAssetAction =
  (
    parentName: NodeName,
    name: NodeName,
    contextPath: string,
    cut: boolean,
    redirect?: boolean
  ): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const parent = getEditedAsset(parentName, projectGraph, graphEdits);
    const group = childEdgeGroup(parent.typeId);
    const edges = computeEditedOutEdges(parentName, group, projectGraph, graphEdits);
    const index = edges.findIndex(edge => edge.targetName === name);
    const edge = edges[index];
    const self = getEditedAsset(name, projectGraph, graphEdits);
    const { branchId } = projectGraph;
    const title = isQuestion(self.typeId) ? 'question' : self.data.title;
    trackAuthoringEvent(`Narrative Editor - ${cut ? 'Cut' : 'Remove'}`, self.typeId);
    dispatch(openToast(cut ? `Cut ${title}.` : `Removed ${title}.`, 'success'));
    dispatch(setNarrativeAssetState(self.name, { deleted: true }));
    // Delay the actual graph edits to allow the UI to provide feedback of the deletion
    setTimeout(() => {
      // TODO: This is bad because it does graph edits 300ms after getting the prior
      // graph edit state, so changes could have occurred. This should probably be
      // a single dispatched action that itself can then look at the current graph
      // state and not require that the caller sends it.
      dispatch(beginProjectGraphEdit(cut ? 'Cut asset' : 'Remove asset'));
      dispatch(deleteProjectGraphEdge(edge));
      if (cut) dispatch(setNarrativeState({ clipboard: { ...self, edge, index } }));
      dispatch(setNarrativeAssetState(self.name, { deleted: undefined }));
      dispatch(autoSaveProjectGraphEdits());
      if (redirect) {
        const next = index < edges.length - 1 ? index + 1 : index - 1;
        if (next >= 0) {
          dispatch(
            replace(
              editorUrl('story', branchId, edges[next].targetName, contextPath, { confirm: false })
            )
          );
        } else {
          const supercontextPath = contextPath.substring(0, contextPath.lastIndexOf('.'));
          dispatch(
            replace(editorUrl('story', branchId, parent, supercontextPath, { confirm: false }))
          );
        }
      }
    }, 300);
  };

export const restoreAssetAction =
  (name: NodeName): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const self = getEditedAsset(name, projectGraph, graphEdits);
    const title = isQuestion(self.typeId) ? 'question' : self.data.title;
    trackAuthoringEvent(`Narrative Editor - Restore`, self.typeId);
    dispatch(beginProjectGraphEdit('Restore asset'));
    dispatch(editProjectGraphNodeData(self.name, { ...self.data, archived: false }));
    dispatch(autoSaveProjectGraphEdits());
    dispatch(openToast(`Restored ${title}.`, 'success'));
  };
