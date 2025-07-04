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
import { pickBy } from 'lodash';

import {
  computeDeltaGraphEditCache,
  computeGraphEditCache,
  setProjectGraphRemote,
} from '../graphEdit';
import { Project } from '../layout/dcmLayoutReducer';
import { ProjectResponse } from '../story/NarrativeMultiverse';
import { isCopiedAsset } from '../story/storyReducer';
import { openToast } from '../toast/actions';
import { CommitInfo } from '../types/api';
import type { AssetNode, EdgeName, NodeName } from '../types/asset';
import { Thunk } from '../types/dcmState';
import { SlimEdge } from '../types/edge';
import { useProjectGraphSelector } from './projectGraphHooks';
import { ProjectGraph } from './projectGraphReducer';
import { endFetchingStructure, startFetchingStructure } from './projectStructureActions';

export type ProjectStructure = {
  edges: Record<EdgeName, SlimEdge>;
  nodes: Record<NodeName, AssetNode>;
  commit: CommitInfo;
  branchId: number | null;
  rootNodeName: NodeName | null;
  homeNodeName: NodeName | null;
  branchCommits: Record<number, number> | null;
  assetBranches: Record<NodeName, number> | null;
  customizedAssets: NodeName[] | null;
  delta: boolean;
};

export const loadStructure = (
  branchId: string | number,
  root: NodeName,
  commit?: number,
  since?: number, // try to fetch a delta since this commit
  include?: string[] // include assets that may not be in the project graph
): Promise<ProjectStructure> =>
  gretchen
    .get(
      commit
        ? `/api/v2/authoring/${branchId}/commits/${commit}/structure`
        : `/api/v2/authoring/${branchId}/nodes/${root}/structure`
    )
    .params({ since, include })
    .exec();

export const fetchStructure =
  (
    root: NodeName | undefined = undefined,
    commit: number | undefined = undefined,
    partial = false
  ): Thunk =>
  (dispatch, getState) => {
    const { layout, projectGraph, projectStructure, story } = getState();
    // unloaded project graph has id 0 hence the ||
    const since =
      (!commit && !projectStructure.fetchedCommit && !partial && projectGraph.commit.id) ||
      undefined;

    // If you cut and save and refetch then the cut asset will not be in the project graph
    // and so if you paste, the asset will appear to be deleted. Similarly if you
    // cut and save and paste and refetch then the pasted asset will not be there. In both
    // cases we have to ask the server to include the asset in the returned graph.
    const include = [
      story.clipboard && !isCopiedAsset(story.clipboard) && story.clipboard.name,
      story.pasteboard,
    ].filter(s => !!s);

    dispatch(startFetchingStructure());
    return loadStructure(
      layout.branchId,
      root ?? layout.project.homeNodeName,
      commit,
      since,
      include
    )
      .then(content => {
        dispatch(receiveProjectGraph(content, partial));
        dispatch(endFetchingStructure(commit ?? null, content.delta ? 'Delta' : 'Full'));
      })
      .catch(e => {
        console.log(e);
        dispatch(openToast('Project failed to load.', 'danger'));
        dispatch(endFetchingStructure(undefined, 'Error')); // undefined because the commit did not load
      });
  };

export const RECEIVE_PROJECT_GRAPH = 'RECEIVE_PROJECT_GRAPH';

export function receiveProjectGraph(
  {
    edges,
    nodes,
    commit,
    branchId,
    rootNodeName,
    homeNodeName,
    branchCommits,
    assetBranches,
    customizedAssets,
    delta,
  }: ProjectStructure,
  partial: boolean
) {
  return dispatch => {
    if (delta) {
      dispatch({
        type: MODIFY_PROJECT_GRAPH,
        modifiedNodes: nodes,
        modifiedEdges: {},
        removedEdgeNames: [],
        removedNodeNames: [],
        customizedAssets: [],
        commit,
      });
      dispatch(computeDeltaGraphEditCache(Object.keys(nodes)));
      dispatch(setProjectGraphRemote(commit.id));
    } else {
      dispatch({
        type: RECEIVE_PROJECT_GRAPH,
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
      });
      dispatch(computeProjectGraphExtras());
      dispatch(computeGraphEditCache());
      if (!partial) dispatch(setProjectGraphRemote(commit.id));
    }
  };
}

export const MODIFY_PROJECT_GRAPH = 'MODIFY_PROJECT_GRAPH';

export function modifyProjectGraph(
  modifiedEdges: Record<EdgeName, SlimEdge>,
  modifiedNodes: Record<NodeName, AssetNode>,
  removedEdgeNames: EdgeName[],
  removedNodeNames: NodeName[],
  customizedAssets: NodeName[],
  commit: CommitInfo
): Thunk {
  return dispatch => {
    // Note that we no longer remove nodes from the graph if their last incoming edge is
    // deleted; this allows cut/paste to work across a save boundary, and the existence
    // of stray nodes in the project graph causes no harm.
    dispatch({
      type: MODIFY_PROJECT_GRAPH,
      modifiedEdges,
      modifiedNodes,
      removedEdgeNames,
      removedNodeNames,
      computeOutEdges: true,
      customizedAssets,
      commit,
    });

    dispatch(computeProjectGraphExtras());
    dispatch(computeGraphEditCache());
  };
}

export const RECEIVE_PROJECT_GRAPH_EXTRAS = 'RECEIVE_PROJECT_GRAPH_EXTRAS';

export const RECEIVE_PROJECT_GRAPH_PROJECTS = 'RECEIVE_PROJECT_GRAPH_PROJECTS';

export function computeProjectGraphExtras() {
  return (dispatch, getState) => {
    const state = getState();
    const { homeNodeName }: Project = state.layout.project;
    if (homeNodeName) {
      const { edges, branchCommits, branchProjects }: ProjectGraph = state.projectGraph;
      const { inEdgesByNode, inEdgesByNode2, outEdgesByNode, outEdgesByNode2 } =
        computeInOutEdgesByNode(edges, homeNodeName);
      dispatch({
        type: RECEIVE_PROJECT_GRAPH_EXTRAS,
        inEdgesByNode,
        inEdgesByNode2,
        outEdgesByNode,
        outEdgesByNode2,
      });
      const missing = Object.keys(branchCommits).filter(id => !branchProjects[id]);
      if (missing.length) {
        // this is heck
        const known = pickBy(branchProjects, p => branchCommits[p.branchId]);
        gretchen
          .get(`/api/v2/authoring/project2s/bulk`)
          .params({ id: missing })
          .exec()
          .then(({ projects }: { projects: ProjectResponse[] }) => {
            dispatch({
              type: RECEIVE_PROJECT_GRAPH_PROJECTS,
              branchProjects: projects.reduce((a, p) => ({ ...a, [p.branchId]: p }), known),
            });
          });
      }
    }
  };
}

const transformEdges = (edgesByNode: Record<string, SlimEdge[]>) => {
  const edgeNamesByNode: Record<string, string[]> = {};
  for (const [sourceName, edges] of Object.entries(edgesByNode)) {
    edges.sort((a, b) => a.position - b.position || a.targetName.localeCompare(b.targetName));
    const edgeNames = new Array<string>();
    for (const { name } of edges) {
      edgeNames.push(name);
    }
    edgeNamesByNode[sourceName] = edgeNames;
  }
  return edgeNamesByNode;
};

export const computeInOutEdgesByNode = (
  edges: Record<string, SlimEdge>,
  homeNodeName: NodeName
) => {
  const edgesArray: SlimEdge[] = Object.values(edges);
  const outEdgesByNode: Record<string, SlimEdge[]> = {
    [homeNodeName]: [],
  };
  const inEdgesByNode: Record<string, SlimEdge[]> = {
    [homeNodeName]: [],
  };
  for (const edge of edgesArray) {
    const { sourceName, targetName } = edge;
    const outEdges = outEdgesByNode[sourceName];
    if (outEdges == null) {
      outEdgesByNode[sourceName] = [edge];
    } else {
      outEdges.push(edge);
    }
    // this preserves prior behaviour of empty lists of unused targets
    if (outEdgesByNode[targetName] == null) outEdgesByNode[targetName] = [];
    const inEdges = inEdgesByNode[targetName];
    if (inEdges == null) {
      inEdgesByNode[targetName] = [edge];
    } else {
      inEdges.push(edge);
    }
  }
  return {
    outEdgesByNode: transformEdges(outEdgesByNode),
    outEdgesByNode2: outEdgesByNode,
    inEdgesByNode: transformEdges(inEdgesByNode),
    inEdgesByNode2: inEdgesByNode,
  };
};

export const RECEIVE_NODES = 'RECEIVE_NODES';

export function receiveNodes(nodes) {
  return {
    type: RECEIVE_NODES,
    nodes,
  };
}

export const useProjectGraph = () => useProjectGraphSelector(state => state);

export const useProjectNodes = () => useProjectGraphSelector(state => state.nodes);

export const useProjectNode = (name: NodeName | undefined) =>
  useProjectGraphSelector(state => state.nodes[name]);

// This isn't in the edited graph store because we can never have an unsaved multiversal link
// to a *new* node in the edited project graph. Limitations on our abilities mean whenever we
// link to a new node, we reload the whole project graph.
export const useRemoteAssetBranches = () => useProjectGraphSelector(state => state.assetBranches);

export const useRemoteAssetBranch = (name?: NodeName): number | undefined =>
  useProjectGraphSelector(state => state.assetBranches[name]);

export const useCustomizedAsset = (name?: NodeName): boolean =>
  useProjectGraphSelector(state => state.customizedAssets.has(name));

export const useRemoteAssetProject = (name?: NodeName): ProjectResponse | undefined =>
  useProjectGraphSelector(state => state.branchProjects[state.assetBranches[name]]);
