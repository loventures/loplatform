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

import { omitBy } from 'lodash';

import { INITIALIZE_DCM } from '../dcmStoreConstants';
import { ProjectResponse } from '../story/NarrativeMultiverse';
import { AssetNode, EdgeName, NodeName } from '../types/asset';
import { SlimEdge } from '../types/edge';
import { UPDATE_ASSET_NODES } from './contentProjectGraphActions.js';
import {
  MODIFY_PROJECT_GRAPH,
  RECEIVE_NODES,
  RECEIVE_PROJECT_GRAPH,
  RECEIVE_PROJECT_GRAPH_EXTRAS,
  RECEIVE_PROJECT_GRAPH_PROJECTS,
} from './projectGraphActions';

export type ProjectGraph = {
  edges: Record<EdgeName, SlimEdge>;
  nodes: Record<NodeName, AssetNode>;
  outEdgesByNode: Record<NodeName, EdgeName[]>;
  outEdgesByNode2: Record<NodeName, SlimEdge[]>;
  inEdgesByNode: Record<NodeName, EdgeName[]>;
  inEdgesByNode2: Record<NodeName, SlimEdge[]>;
  loading: Record<NodeName, true>;
  commit: {
    id: number;
    created: string;
  };
  branchId: number | null;
  rootNodeName: NodeName | null;
  homeNodeName: NodeName | null;
  branchCommits: Record<number, number>;
  assetBranches: Record<NodeName, number>;
  branchProjects: Record<number, ProjectResponse>;
  customizedAssets: Set<NodeName>; // remotely dirty
};

const initialState: ProjectGraph = {
  edges: {},
  nodes: {},
  outEdgesByNode: {},
  outEdgesByNode2: {},
  inEdgesByNode: {},
  inEdgesByNode2: {},
  loading: {},
  commit: {
    id: 0,
    created: '',
  },
  branchId: null,
  rootNodeName: null,
  homeNodeName: null,
  branchCommits: {},
  assetBranches: {},
  branchProjects: {},
  customizedAssets: new Set(),
};

export default function projectGraph(state = initialState, action) {
  switch (action.type) {
    case INITIALIZE_DCM: {
      return initialState;
    }

    case RECEIVE_PROJECT_GRAPH: {
      const {
        partial,
        edges,
        nodes,
        commit,
        branchId,
        rootNodeName,
        homeNodeName,
        branchCommits,
        assetBranches,
        customizedAssets,
      } = action;

      return partial
        ? {
            ...state,
            nodes: { ...state.nodes, ...nodes },
            edges: { ...state.edges, ...edges },
            assetBranches: { ...state.assetBranches, ...assetBranches },
          }
        : {
            ...state,
            nodes,
            edges,
            commit,
            branchId,
            rootNodeName,
            homeNodeName,
            branchCommits,
            assetBranches,
            customizedAssets: new Set(customizedAssets),
          };
    }
    case MODIFY_PROJECT_GRAPH: {
      const {
        modifiedEdges,
        modifiedNodes,
        removedEdgeNames,
        removedNodeNames,
        customizedAssets,
        commit,
      } = action;
      return {
        ...state,
        commit,
        edges: {
          ...omitBy(state.edges, e => removedEdgeNames.includes(e.name)),
          ...modifiedEdges,
        },
        nodes: {
          ...omitBy(state.nodes, n => removedNodeNames.includes(n.name)),
          ...modifiedNodes,
        },
        customizedAssets: new Set(Array.from(state.customizedAssets).concat(customizedAssets)),
      };
    }
    case RECEIVE_NODES: {
      return {
        ...state,
        nodes: { ...state.nodes, ...action.nodes },
      };
    }
    case UPDATE_ASSET_NODES: {
      const newNodes = action.assets.reduce((acc, asset) => ({ ...acc, [asset.name]: asset }), {});

      return {
        ...state,
        nodes: {
          ...state.nodes,
          ...newNodes,
        },
      };
    }
    case RECEIVE_PROJECT_GRAPH_EXTRAS: {
      return {
        ...state,
        inEdgesByNode: action.inEdgesByNode,
        inEdgesByNode2: action.inEdgesByNode2,
        outEdgesByNode: action.outEdgesByNode,
        outEdgesByNode2: action.outEdgesByNode2,
      };
    }
    case RECEIVE_PROJECT_GRAPH_PROJECTS: {
      return {
        ...state,
        branchProjects: action.branchProjects,
      };
    }
    default: {
      return state;
    }
  }
}
