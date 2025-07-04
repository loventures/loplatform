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
  AccessByContentStatus,
  ContentAccessRights,
  ContentStatus,
  noAccess,
} from '../story/contentStatus';
import { ProjectGraph } from '../structurePanel/projectGraphReducer';
import { EdgeName, NewAsset, NodeName } from '../types/asset';
import { EdgeGroup, NewEdge } from '../types/edge';
import { getAllEditedOutEdges, getEditedAsset } from './graphEdit';
import { ContentTree, ContextPath, ProjectGraphEditState } from './graphEditReducer';

export const computeEditedAsset = (
  name: NodeName,
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): NewAsset<any> | undefined => {
  const { nodes } = projectGraph;
  const { addNodes, editNodes } = graphEdits;

  const asset = nodes[name] ?? addNodes[name];
  const edit = editNodes[name];
  return asset && edit
    ? {
        ...asset,
        data: {
          ...asset.data,
          ...edit,
        },
      }
    : asset;
};

const EmptyList = [];

export const computeEditedInEdges = (
  target: NodeName,
  group: EdgeGroup | EdgeGroup[],
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState
): Array<NewEdge> => {
  const { edges, inEdgesByNode } = projectGraph;
  const { addEdges, editEdges, deleteEdges } = graphEdits;
  const inEdges = new Array<NewEdge>();
  const deletedEdges = new Set<EdgeName>();
  const groupMatch = groupMatcher(group);
  for (const assetEdgeDeletes of Object.values(deleteEdges)) {
    for (const edgeName of Object.keys(assetEdgeDeletes)) {
      deletedEdges.add(edgeName);
    }
  }
  for (const edgeName of inEdgesByNode[target] ?? EmptyList) {
    if (!deletedEdges.has(edgeName)) {
      const edge = edges[edgeName];
      if (groupMatch(edge.group)) {
        const edit = editEdges[edge.name];
        inEdges.push(edit ? { ...edge, data: { ...edge.data, ...edit } } : edge);
      }
    }
  }
  for (const assetEdgeAdds of Object.values(addEdges)) {
    for (const edge of assetEdgeAdds) {
      if (edge.targetName === target && groupMatch(edge.group)) {
        inEdges.push(edge);
      }
    }
  }
  return inEdges;
};

const groupMatcher = (group: EdgeGroup | EdgeGroup[]) =>
  typeof group === 'string' ? (g?: EdgeGroup) => g === group : (g?: EdgeGroup) => group.includes(g);

export const computeContentTree = (
  projectGraph: ProjectGraph,
  graphEdits: ProjectGraphEditState,
  accessByStatus: AccessByContentStatus
): ContentTree => {
  const { homeNodeName } = projectGraph;
  const page: Record<ContextPath, number> = {};
  const count: Record<ContextPath, number> = {};
  const playlist = new Array<ContextPath>(homeNodeName);
  const contextPaths: Record<NodeName, ContextPath> = {};
  const accessRights: Record<NodeName, ContentAccessRights> = {};
  const contentStatuses: Record<NodeName, ContentStatus> = {};
  const contentReuse = new Set<NodeName>();
  const loop = (
    name: NodeName,
    contextPath: ContextPath,
    inheritedRights: ContentAccessRights,
    inheritedStatus?: ContentStatus
  ): void => {
    const pages: Record<string, number> = {};
    const contentStatus = getEditedAsset(name, projectGraph, graphEdits)?.data.contentStatus;
    const effectiveStatus = contentStatus ?? inheritedStatus;
    contentStatuses[name] = effectiveStatus;
    const rights =
      accessByStatus['*'] ??
      (contentStatus ? (accessByStatus[contentStatus] ?? noAccess) : inheritedRights);
    accessRights[name] = rights;
    for (const edge of getAllEditedOutEdges(name, projectGraph, graphEdits)) {
      const { group, targetName } = edge;
      if (!treeGroups.has(group)) continue;
      const subcontextId = `${contextPath}.${targetName}`;
      if (group === 'elements' || group === 'questions') {
        const num = (pages[group] ?? 0) + 1;
        page[subcontextId] = pages[group] = num;
      }
      if (group === 'elements') playlist.push(subcontextId);
      if (!contextPaths[targetName]) {
        contextPaths[targetName] = contextPath;
      } else {
        contentReuse.add(targetName);
      }
      loop(targetName, subcontextId, rights, effectiveStatus);
    }
    // If ever a container had both then we would need to store count under `contextPath.edgeGroup`
    count[contextPath] = pages.elements ?? pages.questions;
  };
  loop(homeNodeName, homeNodeName, noAccess, undefined);
  return {
    loaded: true,
    page,
    count,
    playlist,
    contextPaths,
    accessRights,
    contentStatuses,
    contentReuse,
  };
};

// These are the edges, children of which we need context paths for
export const treeGroups = new Set<EdgeGroup>(['elements', 'questions', 'survey', 'cblRubric']);
