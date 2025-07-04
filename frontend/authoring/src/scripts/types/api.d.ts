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

import { AssetNode, AssetNodeData, EdgeName, NodeName, TypeId } from './asset';
import { EdgeData, EdgeGroup, FullEdge, Includes, SlimEdge } from './edge';

export interface AssetAndIncludes {
  asset: AssetNode;
  includes: Includes;
}

export interface EdgeWebResponse {
  count: number;
  objects: FullEdge[];
}

export interface CommitInfo {
  id: number;
  created: string;
}

export interface StructureWebResponse {
  edges: Record<string, SlimEdge>;
  nodes: Record<string, AssetNode>;
  commit: CommitInfo;
  branchId: number | null;
  homeNodeName: string | null;
  rootNodeName: string | null;
  branchCommits: Record<number, number> | null;
  assetBranches: Record<string, number> | null;
  customizedAssets: string[] | null;
  delta: boolean;
}

export interface WriteOpsResponse {
  nodes: Record<NodeName, AssetNode>;
  edges: Record<EdgeName, FullEdge>;
  deletedEdges: EdgeName[];
  newNodes: Record<string, NodeName>;
  newEdges: Record<string, EdgeName>;
  customizedAssets: NodeName[];
  priorCommit: CommitInfo;
  commit: CommitInfo;
  squashed: boolean;
}

export type WriteOpType =
  | 'setNodeData'
  | 'addNode'
  | 'addEdge'
  | 'deleteEdge'
  | 'setEdgeOrder'
  | 'setEdgeData'
  | 'addDependency'
  | 'updateDependency';

export interface WriteOp {
  op: WriteOpType;

  [others: string]: any;
}

export interface AddNodeWriteOp extends WriteOp {
  op: 'addNode';
  typeId: TypeId;
  name: string;
  data: AssetNodeData;
}

export interface SetNodeDataWriteOp extends WriteOp {
  op: 'setNodeData';
  name: NodeName;
  data: AssetNodeData;
}

export type BeforeEdge = { before: NodeName };
export type AfterEdge = { after: NodeName };
export type EdgePosition = 'start' | 'end' | BeforEdge | AfterEdge;

export interface AddEdgeWriteOp extends WriteOp {
  op: 'addEdge';
  name: EdgeName;
  sourceName: NodeName;
  targetName: NodeName;
  group: EdgeGroup;
  traverse: boolean;
  data: EdgeData<any>;
  position?: EdgePosition;
}

export interface DeleteEdgeWriteOp extends WriteOp {
  op: 'deleteEdge';
  name: EdgeName;
}

export interface SetEdgeOrderWriteOp extends WriteOp {
  op: 'setEdgeOrder';
  sourceName: NodeName;
  group: EdgeGroup;
  ordering: EdgeName[];
}

export interface SetEdgeDataWriteOp extends WriteOp {
  op: 'setEdgeData';
  name: EdgeName;
  data: EdgeData<any>;
}

// synthetic WriteOp
export interface AddDependencyWriteOp extends WriteOp {
  op: 'addDependency';
  projectId: number;
  projectTitle: string;
  projectCode?: string;
}

// synthetic WriteOp
export interface UpdateDependencyWriteOp extends WriteOp {
  op: 'updateDependency';
  projectId: number;
  projectTitle: string;
  projectCode?: string;
  narrativelyUpdatedNodeNames: NodeName[];
  narrativelyAddedNodeNames: NodeName[];
  narrativelyRemovedNodeNames: NodeName[];
  reportId: number;
}
