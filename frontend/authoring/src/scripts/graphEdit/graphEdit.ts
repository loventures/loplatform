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

import edgeRules from '../editor/EdgeRuleConstants';
import { CopiedAsset } from '../story/storyReducer';
import { ProjectGraph } from '../structurePanel/projectGraphReducer';
import { EdgePosition } from '../types/api';
import { NewAsset, NodeName, TypeId } from '../types/asset';
import { EdgeGroup, NewAssetWithEdge, NewEdge } from '../types/edge';
import { FakeEdge, ProjectGraphEditState } from './graphEditReducer';

const isBefore = (pos?: EdgePosition): pos is { before: NodeName } =>
  pos != null && typeof (pos as any).before === 'string';
const isAfter = (pos?: EdgePosition): pos is { after: NodeName } =>
  pos != null && typeof (pos as any).after === 'string';

const EmptyList = [];

export const computeEditedOutEdges = (
  source: NodeName,
  group: EdgeGroup | EdgeGroup[],
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): Array<NewEdge> => {
  const { edges, outEdgesByNode } = projectGraph;
  const { edgeOrders, addEdges, editEdges, deleteEdges } = graphEdits;
  const added = addEdges[source] ?? EmptyList;
  const deleted = deleteEdges[source];
  const outEdges = new Array<NewEdge>();
  for (const grp of typeof group === 'string' ? [group] : group) {
    const ordered = new Set<NodeName>(); // nodes we've added by edge order
    for (const name of edgeOrders[source]?.[grp] ?? outEdgesByNode[source] ?? EmptyList) {
      const edge = edges[name] ?? added.find(edge => edge.name === name);
      if (edge?.group === grp && !deleted?.[name]) {
        const edit = editEdges[edge.name];
        outEdges.push(edit ? { ...edge, data: { ...edge.data, ...edit } } : edge);
        ordered.add(edge.name);
      }
    }
    for (const edge of added) {
      if (edge.group === grp && !ordered.has(edge.name)) {
        let index = outEdges.length;
        const pos = edge.newPosition;
        if (pos === 'start') {
          index = 0;
        } else if (isBefore(pos)) {
          index = outEdges.findIndex(e => e.name === pos.before);
        } else if (isAfter(pos)) {
          index = 1 + outEdges.findIndex(e => e.name === pos.after);
        }
        outEdges.splice(index, 0, edge);
      }
    }
  }
  return outEdges;
};

export const getEditedAsset = (
  name: NodeName,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): NewAsset<any> | undefined => graphEdits.editedAssets[name] ?? projectGraph.nodes[name];

const NoEdges = [];

export const getAllEditedInEdges = (
  name: NodeName,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): NewEdge[] => graphEdits.editedInEdges[name] ?? projectGraph.inEdgesByNode2[name] ?? NoEdges;

export const getAllEditedOutEdges = (
  name: NodeName,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): NewEdge[] => graphEdits.editedOutEdges[name] ?? projectGraph.outEdgesByNode2[name] ?? NoEdges;

export const isNewEdge = (edge: NewEdge | FakeEdge): edge is NewEdge => (edge as any).name;

// kill
// mixing in edge is highly sus but otherwise we have to return a 2-tuple
// which is equally highly sus and much less convenient. index is the original
// index in the out edges.
export const computeEditedTargets = <T extends TypeId = any>(
  source: NodeName,
  group: EdgeGroup,
  typeId: T | undefined,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): Array<NewAssetWithEdge<T>> => {
  let index = 0;
  return computeEditedOutEdges(source, group, projectGraph, graphEdits)
    .map(edge => {
      const editedAsset = getEditedAsset(edge.targetName, projectGraph, graphEdits);
      const asset = editedAsset ?? graphEdits.restoredNodes[edge.targetName];
      return asset && (typeId == null || typeId === asset.typeId) && !asset.data.archived
        ? {
            ...asset,
            edge,
            index: index++,
            restored: !editedAsset,
          }
        : undefined;
    })
    .filter(node => node != null);
};

export const getCopiedAsset = (
  name: NodeName,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): CopiedAsset => {
  const cache: Record<NodeName, CopiedAsset> = {};
  const loop = (asset: NewAsset<any>): CopiedAsset => {
    const cached = cache[asset.name];
    if (cached != null) return cached;
    const copied: CopiedAsset = {
      ...asset,
      edgeTargets: {},
    };
    cache[asset.name] = copied;
    for (const group of Object.keys(edgeRules[asset.typeId] ?? {}) as EdgeGroup[]) {
      const targets = new Array<CopiedAsset>();
      for (const edge of computeEditedOutEdges(asset.name, group, projectGraph, graphEdits)) {
        const target = getEditedAsset(edge.targetName, projectGraph, graphEdits);
        const traverse = isNewEdge(edge) && edge.traverse; // overlays never traverse
        const copied = traverse ? loop(target) : { ...target, edgeTargets: {} };
        targets.push({
          ...copied,
          edgeTraverse: traverse,
          edgeId: isNewEdge(edge) ? edge.edgeId : undefined,
          edgeData: edge.data,
        });
      }
      if (targets.length) copied.edgeTargets[group] = targets;
    }
    return copied;
  };

  return loop(getEditedAsset(name, projectGraph, graphEdits));
};

// The unsaved changes check machinery kicks in when we navigate to a preview/test section
// even if we save/discard on the popup modal, because we redirect before the redux store
// has updated to not dirty. So we have this impressive global var.
export const suppressPromptForUnsavedGraphEdits = (value?: boolean): boolean => {
  if (value != null) suppress = value;
  return suppress;
};

let suppress = false;
