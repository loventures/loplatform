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

import { groupBy, keyBy, omit } from 'lodash';
import AssetInEditService from '../editor/AssetInEditService';
import { modifyProjectGraph, receiveProjectGraph } from '../structurePanel/projectGraphActions';
import { openToast, TOAST_TYPES, ToastType } from '../toast/actions';
import { DeleteEdgeWriteOp, WriteOp, WriteOpsResponse } from '../types/api';
import { AssetNode, TypeId } from '../types/asset';
import { EdgeGroup, FullEdge, SlimEdge } from '../types/edge';
import { Polyglot } from '../types/polyglot';
import { generateEdgeData } from './AssetGenerators';
import AuthoringApiService from './AuthoringApiService';
import AuthoringOpsService from './AuthoringOpsService';
import { dcmStore } from '../dcmStore';

/** Pure function that transforms the WriteOpsResponse into hydrated new edges for a given parent
 *  Note: What if we don't filter by parent?
 * */
export const filterResponseToNewEdges =
  (parentName: string) =>
  ({ edges, newEdges }: WriteOpsResponse): FullEdge[] => {
    return Object.values(newEdges)
      .map(edgeName => edges[edgeName])
      .filter(edge => edge.source.name === parentName);
  };

/** Pure function that takes a hydrated edge and replaces source/target with sourceName/targetName. */
export const normalizeEdge = (edge: FullEdge): SlimEdge => {
  const norm = omit(edge, ['source', 'target', 'traverse']) as SlimEdge;
  norm.sourceName = edge.source.name;
  norm.targetName = edge.target.name;
  return norm;
};

class EdgeEditorService {
  private traverse(group: EdgeGroup) {
    return !['teaches', 'assesses', 'gradebookCategory'].includes(group); // for now but we need to configure this.
  }

  public addEdge(parent: AssetNode, group: EdgeGroup, asset: AssetNode) {
    const edgeData = generateEdgeData(group);
    const addEdgeOp = AuthoringOpsService.addEdgeOp(
      parent.name,
      asset.name,
      group,
      this.traverse(group),
      edgeData
    );
    const setOrderOp = AuthoringOpsService.setEdgeOrderOp(parent.name, group, [
      ...this.getEdgesInGroup(group).map(e => e.name),
      addEdgeOp.name,
    ]);

    return this.postAddEdge([addEdgeOp, setOrderOp], parent.name).catch(this.handleError);
  }

  /**
   * postAddEdge method posts almost any series of writeOps and manages post-processing for the response.
   *
   * NOTE: caller is responsible for catching errors.
   *
   * NOTE: this is poorly named. We want to be able to send any series of ops and handle updating
   * all parts of the app with the results. We may also be able to remove the parentName param.
   * */
  public postAddEdge(ops: WriteOp[], parentName: string) {
    const deleteOps = ops.filter(op => op.op === 'deleteEdge') as DeleteEdgeWriteOp[];
    return AuthoringOpsService.postWriteOps(ops)
      .then(this.sendEdgesToStructurePanel)
      .then(this.sendEdgesToAssetInEdit(deleteOps))
      .then(filterResponseToNewEdges(parentName))
      .then(this.fetchChildEdgesForStructurePanel);
  }

  /**
   *
   * sendEdgesToStructurePanel method accepts the deleteOps and write ops response to send all changed
   * edges and nodes to the structure panel. They may not be needed for render but there is no harm
   * in sending everything.
   *
   * Note: we could possibly combine this with fetchChildEdgesForStructurePanel.
   *
   * */
  public sendEdgesToStructurePanel = (response: WriteOpsResponse) => {
    const addedOrModifiedEdges = Object.values(response.edges);
    const nodesFromEdges = keyBy(
      addedOrModifiedEdges.map(e => e.target),
      'name'
    );
    const allNodes = { ...response.nodes, ...nodesFromEdges };
    dcmStore.dispatch(
      modifyProjectGraph(
        keyBy(addedOrModifiedEdges.map(normalizeEdge), 'name'),
        allNodes,
        response.deletedEdges,
        [],
        response.customizedAssets,
        response.commit
      )
    );

    return response;
  };

  public fetchChildEdgesForStructurePanel = (addedEdges: FullEdge[]) => {
    addedEdges.forEach(edge =>
      AuthoringApiService.fetchStructure(edge.target).then(response =>
        dcmStore.dispatch(receiveProjectGraph(response, true))
      )
    );
    return addedEdges;
  };

  /**
   * sendEdgesToAssetInEdit method accepts deleteOps and write ops response in order to update the includes
   * object in [[AssetInEditService]].
   *
   * Note: We do this as generically as possible but some of this logic can be simplified.
   *
   * */
  public sendEdgesToAssetInEdit =
    (deleteOps: DeleteEdgeWriteOp[] = []) =>
    (response: WriteOpsResponse) => {
      const removedEdges = deleteOps.map(e => e.name);
      const { assetNode, includes } = dcmStore.getState().assetEditor;
      const edgesToSend = Object.values(response.edges).filter(
        edge => edge.source.name === assetNode.name
      );
      if (edgesToSend.length === 0) {
        // edgesToSend could be empty if we dragged the last item into the panel.
        Object.keys(includes).forEach(grp => {
          const existingEdges = [...includes[grp]];
          const filteredEdges = existingEdges.filter(e => !removedEdges.includes(e.name));
          if (existingEdges.length !== filteredEdges.length) {
            AssetInEditService.replaceEdges(filteredEdges, grp as EdgeGroup);
          }
          // else we simply don't have any edges to update.
        });
      } else {
        const keyedToSend = groupBy(edgesToSend, 'group');
        Object.keys(includes).forEach(grp => {
          const existingEdges = includes[grp].filter(e => !removedEdges.includes(e.name));
          const keyed = keyBy(existingEdges, 'name');
          keyedToSend[grp]?.forEach(edge => (keyed[edge.name] = edge));
          const sorted = Object.values(keyed).sort((a, b) => a.position - b.position);
          AssetInEditService.replaceEdges(sorted, grp as EdgeGroup);
        });
      }
      return response;
    };

  public handleError(err) {
    switch (err) {
      case 'backdrop click':
      case 'cancel':
      case 'escape key press':
      case undefined:
        return;
      default:
        console.error('unable to add relationship ', err);
        if (typeof err === 'string') {
          dcmStore.dispatch(openToast(this.polyglot.t(err), TOAST_TYPES.DANGER as ToastType));
        } else {
          dcmStore.dispatch(
            openToast(this.polyglot.t('CONTENT_HAS_BEEN_UPDATED'), TOAST_TYPES.DANGER as ToastType)
          );
        }
    }
  }

  private get polyglot(): Polyglot {
    return dcmStore.getState().configuration.translations;
  }

  private get assetInEdit(): AssetNode {
    return dcmStore.getState().assetEditor.assetNode;
  }

  public getEdgesInGroup(group: EdgeGroup): FullEdge[] {
    return dcmStore.getState().assetEditor.includes[group] || [];
  }
}

export default new EdgeEditorService();
