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
import { flatMap, has, isEmpty } from 'lodash';
import React from 'react';
import { ThunkDispatch } from 'redux-thunk';

import { trackAuthoringEvent } from '../analytics';
import { UPDATE_BRANCH } from '../dcmStoreConstants';
import { AllEdgeGroups } from '../editor/EdgeRuleConstants';
import { Project } from '../layout/dcmLayoutReducer';
import { DiscardChangesModalData } from '../modals/DiscardChangesModal';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import AuthoringApiClient from '../services/AuthoringApiService';
import AuthoringOpsService from '../services/AuthoringOpsService';
import EdgeEditorService from '../services/EdgeEditorService';
import { MergeUpdatesModalData } from '../story/modals/MergeUpdatesModal';
import { OverwriteChangesModalData } from '../story/modals/OverwriteChangesModal';
import { isStagedBlob } from '../story/story';
import {
  fetchStructure,
  loadStructure,
  receiveProjectGraph,
} from '../structurePanel/projectGraphActions';
import { ProjectGraph } from '../structurePanel/projectGraphReducer';
import { openToast } from '../toast/actions';
import {
  AddEdgeWriteOp,
  AddNodeWriteOp,
  DeleteEdgeWriteOp,
  EdgePosition,
  SetEdgeDataWriteOp,
  SetNodeDataWriteOp,
  WriteOpsResponse,
} from '../types/api';
import { AssetNodeData, BlobRef, EdgeName, NewAsset, NodeName } from '../types/asset';
import { DcmState, Thunk } from '../types/dcmState';
import { EdgeData, EdgeGroup, NewEdge } from '../types/edge';
import {
  computeEditedOutEdges,
  getEditedAsset,
  suppressPromptForUnsavedGraphEdits,
} from './graphEdit';
import {
  computeContentTree,
  computeEditedAsset,
  computeEditedInEdges,
  treeGroups,
} from './graphEditCache';
import { ProjectGraphEditState } from './graphEditReducer';

export const RESET_PROJECT_GRAPH_EDITS = 'RESET_PROJECT_GRAPH_EDITS';

// When commitId is specified it is a commit that should be added to the committed undo history
// success only here because this same action is used for the (dead) Cancel button.
export const resetProjectGraphEdits =
  (
    autoSave?: boolean,
    commitId?: number,
    squashed?: boolean,
    idempotent?: boolean,
    success?: boolean,
    remoteHead?: number
  ): Thunk =>
  (dispatch, getState) => {
    const { graphEdits } = getState();
    dispatch({
      type: RESET_PROJECT_GRAPH_EDITS,
      autoSave,
      squashed,
      idempotent,
      commitId,
      success,
      remoteHead,
    });
    if (success) {
      // replay all the updates that have been enqueued since the save started.
      for (const action of graphEdits.unsaved) {
        dispatch(action);
      }
    }
    dispatch(computeGraphEditCache());
  };

export const RECEIVE_GRAPH_EDIT_CACHE = 'RECEIVE_GRAPH_EDIT_CACHE';

// sometimes graph edits are persisted across a project graph load (e.g. restoring from history)
// in which case the initial cache needs to be fully populated.
export const computeGraphEditCache = (): Thunk => (dispatch, getState) => {
  const { projectGraph, graphEdits } = getState();
  const editedAssets: Record<NodeName, NewAsset<any>> = {};
  const editedOutEdges: Record<NodeName, NewEdge[]> = {};
  const editedInEdges: Record<NodeName, NewEdge[]> = {};

  const processEditedAsset = (name: NodeName): void => {
    if (!editedAssets[name])
      editedAssets[name] = computeEditedAsset(name, projectGraph, graphEdits);
  };
  for (const name of Object.keys(graphEdits.addNodes)) {
    processEditedAsset(name);
  }
  for (const name of Object.keys(graphEdits.editNodes)) {
    processEditedAsset(name);
  }
  // restoredNodes are their own thing.

  const processOutEdges = (name: NodeName): void => {
    if (!editedOutEdges[name])
      editedOutEdges[name] = computeEditedOutEdges(name, AllEdgeGroups, projectGraph, graphEdits);
  };
  const processInEdges = (name: NodeName): void => {
    if (!editedInEdges[name])
      editedInEdges[name] = computeEditedInEdges(name, AllEdgeGroups, projectGraph, graphEdits);
  };
  const processEdge = (edge: NewEdge) => {
    processOutEdges(edge.sourceName);
    processInEdges(edge.targetName);
  };
  for (const addEdges of Object.values(graphEdits.addEdges)) {
    for (const edge of addEdges) processEdge(edge);
  }
  for (const name of Object.keys(graphEdits.editEdges)) {
    const edge = projectGraph.edges[name];
    if (edge) processEdge(edge);
  }
  for (const deleteEdges of Object.values(graphEdits.deleteEdges)) {
    for (const edge of Object.values(deleteEdges)) processEdge(edge);
  }
  for (const edgeOrders of Object.values(graphEdits.edgeOrders)) {
    for (const names of Object.values(edgeOrders)) {
      for (const name of names) {
        const edge = projectGraph.edges[name];
        if (edge) processEdge(edge);
      }
    }
  }

  dispatch({
    type: RECEIVE_GRAPH_EDIT_CACHE,
    editedAssets,
    editedInEdges,
    editedOutEdges,
  });
  dispatch(updateContentTree());
};

export const computeDeltaGraphEditCache =
  (delta: NodeName[]): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const ogEditedAssets = graphEdits.editedAssets;

    if (delta.some(name => ogEditedAssets[name])) {
      // let the delta assets be recomputed into the edit cache
      const editedAssets = { ...ogEditedAssets };

      for (const name of delta) {
        if (editedAssets[name])
          editedAssets[name] = computeEditedAsset(name, projectGraph, graphEdits);
      }

      dispatch({
        type: RECEIVE_GRAPH_EDIT_CACHE,
        editedAssets,
      });
    }
  };

export const updateEditedAsset =
  (name: NodeName): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const editedAssets: Record<NodeName, NewAsset<any>> = { ...graphEdits.editedAssets };
    editedAssets[name] = computeEditedAsset(name, projectGraph, graphEdits);
    dispatch({ type: RECEIVE_GRAPH_EDIT_CACHE, editedAssets });
  };

// TODO: Iterating through ALL edge groups is super sad. Could pick
// out/in edge groups from the edge group rules, or could look at the
// edges in the graph.
export const updateEditedEdge =
  (srcName: NodeName, tgtName?: NodeName): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const editedOutEdges: Record<NodeName, NewEdge[]> = {
      ...graphEdits.editedOutEdges,
      [srcName]: computeEditedOutEdges(srcName, AllEdgeGroups, projectGraph, graphEdits),
    };
    const editedInEdges: Record<NodeName, NewEdge[]> | undefined = tgtName
      ? {
          ...graphEdits.editedInEdges,
          [tgtName]: computeEditedInEdges(tgtName, AllEdgeGroups, projectGraph, graphEdits),
        }
      : undefined;
    dispatch({ type: RECEIVE_GRAPH_EDIT_CACHE, editedInEdges, editedOutEdges });
  };

export const updateContentTree = (): Thunk => (dispatch, getState) => {
  const { projectGraph, graphEdits, configuration, layout } = getState();
  const accessByStatus = configuration.contentRightsByRoleAndStatus[layout.role] ?? {};
  const contentTree = computeContentTree(projectGraph, graphEdits, accessByStatus);
  dispatch({ type: RECEIVE_GRAPH_EDIT_CACHE, contentTree });
};

export const ADD_PROJECT_GRAPH_NODE = 'ADD_PROJECT_GRAPH_NODE';

export const addProjectGraphNode =
  (node: NewAsset<any>): Thunk =>
  dispatch => {
    dispatch({
      type: ADD_PROJECT_GRAPH_NODE,
      node,
    });
    dispatch(updateEditedAsset(node.name));
  };

export const RESTORE_PROJECT_GRAPH_NODE = 'RESTORE_PROJECT_GRAPH_NODE';

/** This stores a placeholder for a restored node from a prior commit so it will display in content lists. */
export const restoreProjectGraphNode = (node: NewAsset<any>) => ({
  type: RESTORE_PROJECT_GRAPH_NODE,
  node,
});

export const insertProjectGraphNode =
  (
    name: NodeName,
    parentName: NodeName,
    group: EdgeGroup,
    after?: NodeName,
    before?: NodeName
  ): Thunk =>
  (dispatch, getState) => {
    // this needs to know the latest state of the union for the case where we add asset on
    // new page with a save during the transition.
    const { projectGraph, graphEdits } = getState();
    const siblings = computeEditedOutEdges(parentName, group, projectGraph, graphEdits);
    const newPosition: EdgePosition = after
      ? { after: siblings.find(edge => edge.targetName === after).name }
      : before
        ? { before: siblings.find(edge => edge.targetName === before).name }
        : 'end';
    const edge: NewEdge = {
      name: crypto.randomUUID(),
      sourceName: parentName,
      targetName: name,
      group,
      traverse: true,
      data: {},
      newPosition,
    };
    dispatch(addProjectGraphEdge(edge));
  };

export const EDIT_PROJECT_GRAPH_NODE_DATA = 'EDIT_PROJECT_GRAPH_NODE_DATA';

export const editProjectGraphNodeData =
  (name: NodeName, data: AssetNodeData): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const original = getEditedAsset(name, projectGraph, graphEdits);
    dispatch({
      type: EDIT_PROJECT_GRAPH_NODE_DATA,
      name,
      data,
      original,
    });
    dispatch(updateEditedAsset(name));
  };

export const ADD_PROJECT_GRAPH_EDGE = 'ADD_PROJECT_GRAPH_EDGE';

export const addProjectGraphEdge =
  (edge: NewEdge): Thunk =>
  dispatch => {
    dispatch({
      type: ADD_PROJECT_GRAPH_EDGE,
      edge,
    });
    dispatch(updateEditedEdge(edge.sourceName, edge.targetName));
    if (treeGroups.has(edge.group)) dispatch(updateContentTree());
  };

export const EDIT_PROJECT_GRAPH_EDGE_DATA = 'EDIT_PROJECT_GRAPH_EDGE_DATA';

export const editProjectGraphEdgeData =
  (sourceName: NodeName, name: EdgeName, data: EdgeData<any>): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const edge =
      projectGraph.edges[name] ?? graphEdits.addEdges[sourceName]?.find(edge => edge.name === name);
    dispatch({
      type: EDIT_PROJECT_GRAPH_EDGE_DATA,
      sourceName,
      name,
      data,
    });
    dispatch(updateEditedEdge(edge.sourceName, edge.targetName));
  };

export const DELETE_PROJECT_GRAPH_EDGE = 'DELETE_PROJECT_GRAPH_EDGE';

// This takes a whole edge rather than just a name so that I can resurrect it if it is re-added
export const deleteProjectGraphEdge =
  (edge: NewEdge): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const edgeOrder = computeEditedOutEdges(
      edge.sourceName,
      edge.group,
      projectGraph,
      graphEdits
    ).map(e => e.name);
    dispatch({
      type: DELETE_PROJECT_GRAPH_EDGE,
      edge,
      edgeOrder,
    });
    dispatch(updateEditedEdge(edge.sourceName, edge.targetName));
    if (treeGroups.has(edge.group)) dispatch(updateContentTree());
  };

export const SET_PROJECT_GRAPH_EDGE_ORDER = 'SET_PROJECT_GRAPH_EDGE_ORDER';

export const setProjectGraphEdgeOrder =
  (name: NodeName, group: EdgeGroup, order: EdgeName[]): Thunk =>
  dispatch => {
    dispatch({
      type: SET_PROJECT_GRAPH_EDGE_ORDER,
      name,
      group,
      order,
    });
    dispatch(updateEditedEdge(name));
    if (treeGroups.has(group)) dispatch(updateContentTree());
  };

export const BEGIN_PROJECT_GRAPH_EDIT = 'BEGIN_PROJECT_GRAPH_EDIT';

// If key matches prior checkpoint then they are coalesced. missing key means always new
export const beginProjectGraphEdit = (comment: string, key?: string) => ({
  type: BEGIN_PROJECT_GRAPH_EDIT,
  comment,
  key,
});

export const DISCARD_PROJECT_GRAPH_EDIT = 'DISCARD_PROJECT_GRAPH_EDIT';

export const discardProjectGraphEdit = (key: string) => ({
  type: DISCARD_PROJECT_GRAPH_EDIT,
  key,
});

export const escapeProjectGraphEdit = (): Thunk => (dispatch, getState) => {
  const { graphEdits } = getState();
  if (graphEdits.undo) dispatch(discardProjectGraphEdit(graphEdits.undo.key));
};

export const UNDO_PROJECT_GRAPH_EDIT = 'UNDO_PROJECT_GRAPH_EDIT';

export const undoProjectGraphEdit = (): Thunk => (dispatch, getState) => {
  const { graphEdits } = getState();
  if (graphEdits.undo) {
    dispatch({
      type: UNDO_PROJECT_GRAPH_EDIT,
    });
  } else if (graphEdits.undos.length) {
    dispatch(undoGraphEditCommit());
  }
};

export const REDO_PROJECT_GRAPH_EDIT = 'REDO_PROJECT_GRAPH_EDIT';

export const redoProjectGraphEdit = () => ({
  type: REDO_PROJECT_GRAPH_EDIT,
});

export const SET_PROJECT_GRAPH_SAVING = 'SET_PROJECT_GRAPH_SAVING';

export const setProjectGraphSaving = (saving: boolean, problem?: boolean) => ({
  type: SET_PROJECT_GRAPH_SAVING,
  saving,
  problem,
});

export const SET_PROJECT_GRAPH_ORIGINS = 'SET_PROJECT_GRAPH_ORIGINS';

export const setProjectGraphOrigins = (origins: Record<NodeName, NodeName>) => ({
  type: SET_PROJECT_GRAPH_ORIGINS,
  origins,
});

export const PUT_NODE_BLOB_REF = 'PUT_NODE_BLOB_REF';

// let the UI know about the blobref for an uploaded but not saved node
export const putNodeBlobRef = (name: NodeName, blobRef: BlobRef) => ({
  type: PUT_NODE_BLOB_REF,
  name,
  blobRef,
});

const uploadStagedBlobAsset = (asset: NewAsset<any>) =>
  isStagedBlob(asset.data.source)
    ? AuthoringApiClient.uploadToBlobstore(
        asset,
        new Blob([asset.data.source.get()], { type: 'text/html' })
      ).then(source => ({ ...asset, data: { ...asset.data, source } }))
    : asset;

export const confirmDiscardProjectGraphEdit =
  (session: string, andThen: Thunk): Thunk =>
  (dispatch, getState) => {
    if (getState().graphEdits.undo?.key === session) {
      dispatch(
        openModal<DiscardChangesModalData>(ModalIds.DiscardChanges, {
          escape: true,
          discard: () => {
            trackAuthoringEvent(`Narrative Editor - Discard`);
            dispatch(discardProjectGraphEdit(session));
            dispatch(andThen);
          },
        })
      );
    } else {
      dispatch(andThen);
    }
  };

export const confirmSaveProjectGraphEdits =
  (andThen: Thunk): Thunk =>
  async (dispatch, getState) => {
    const { graphEdits } = getState();
    if (!graphEdits.dirty) {
      dispatch(andThen);
    } else {
      trackAuthoringEvent(`Narrative Editor - Save`, 'Autosave');
      dispatch(safeSaveProjectGraphEdits(andThen));
    }
  };

// Delays a click on a hyperlink if the graph edit store is dirty, prompts to save or cancel.
export const confirmSaveProjectGraphEditsLink = (
  e: React.MouseEvent<HTMLElement>,
  dirty: boolean,
  dispatch: ThunkDispatch<DcmState, any, any>
) => {
  if (!dirty || suppressPromptForUnsavedGraphEdits()) return;
  const target = e.currentTarget;
  const newTab = e.ctrlKey || e.metaKey;
  e.preventDefault();
  dispatch(
    confirmSaveProjectGraphEdits(() => {
      if (newTab) {
        window.open(target.getAttribute('href'), '_blank');
      } else {
        suppressPromptForUnsavedGraphEdits(true);
        target?.click();
      }
    })
  );
};

const prepareSaveOps = async (projectGraph: ProjectGraph, graphEdits: ProjectGraphEditState) => {
  const { nodes, edges } = projectGraph;
  const { editNodes, addNodes, addEdges, editEdges, deleteEdges, edgeOrders } = graphEdits;

  const addNodeOps = new Array<AddNodeWriteOp>();
  for (const asset of Object.values(addNodes)) {
    const uploaded = await uploadStagedBlobAsset(asset);
    addNodeOps.push(AuthoringOpsService.addNodeOp(uploaded));
  }

  const updateNodeOps = new Array<SetNodeDataWriteOp>();
  for (const [name, update] of Object.entries(editNodes)) {
    const asset = nodes[name];
    if (!asset) continue; // deleted by merge
    const updated = {
      ...asset,
      data: {
        ...asset.data,
        title: asset.data.title || 'Untitled', // hack because if we save a question with no content it computes an empty title and becomes unsavable
        ...update,
      },
    };
    const uploaded = await uploadStagedBlobAsset(updated);
    updateNodeOps.push(AuthoringOpsService.updateNodeOp(uploaded));
  }

  // TODO: not merge safe, a merge could result in a source being deleted or could cause arity errors for singular edges
  // or could result in duplicate edges to the same target. anchors (before/after) could be deleted.
  const addEdgeOps = new Array<AddEdgeWriteOp>();
  for (const nodeEdges of Object.values(addEdges)) {
    for (const edge of nodeEdges) {
      addEdgeOps.push(AuthoringOpsService.addNewEdgeOp(edge));
    }
  }

  const updateEdgeOps = new Array<SetEdgeDataWriteOp>();
  for (const [name, update] of Object.entries(editEdges)) {
    const edge = edges[name];
    if (!edge) continue; // deleted by merge
    const updated = {
      ...edge.data,
      ...update,
    };
    updateEdgeOps.push(AuthoringOpsService.setEdgeDataOp(name, updated));
  }

  const deleteEdgeOps = new Array<DeleteEdgeWriteOp>();
  for (const nodeEdges of Object.values(deleteEdges)) {
    for (const name of Object.keys(nodeEdges)) {
      if (edges[name]) deleteEdgeOps.push(AuthoringOpsService.deleteEdgeOp(name));
    }
  }

  // TODO: not merge safe, a merge could result in a pending set edge order failing
  const edgeOrderOps = flatMap(Object.entries(edgeOrders), ([name, groupOrders]) =>
    Object.entries(groupOrders).map(([group, order]) =>
      AuthoringOpsService.setEdgeOrderOp(name, group as any, order)
    )
  );

  return [
    ...addNodeOps,
    ...updateNodeOps,
    ...deleteEdgeOps,
    ...addEdgeOps,
    ...updateEdgeOps,
    ...edgeOrderOps,
  ];
};

// TODO: autosave should schedule an autosave. a new graph edit w/ pending should save immediately, otherwise
// it can happen 1/3 later so a local undo is possible without a commit... realtime + save/continue modal should
// autosave - for example, make change, tab, click admin or such..
export const autoSaveProjectGraphEdits =
  (andThen: Thunk = () => void 0): Thunk =>
  (dispatch, getState) => {
    const { configuration, graphEdits, story } = getState();
    if (configuration.realTime && graphEdits.dirty && !graphEdits.saving && !story.offline) {
      trackAuthoringEvent(`Narrative Editor - Autosave`);
      dispatch(safeSaveProjectGraphEdits(andThen, { autoSave: true }));
      // TODO if dirty and saving this should retry after the save
    }
  };

// Ignoring server-rendered properties
const equalish = (a: any, b: any): boolean =>
  a == null
    ? b == null
    : typeof a === 'object'
      ? typeof b === 'object' && b != null && equalishObject(a, b)
      : Array.isArray(a)
        ? Array.isArray(b) && equalishArray(a, b)
        : a === b;

const equalishObject = (a: object, b: object): boolean => {
  const aNames = Object.getOwnPropertyNames(a);
  const bNames = Object.getOwnPropertyNames(b);
  return (
    aNames.every(k => k === 'renderedHtml' || equalish(a[k], b[k])) &&
    bNames.every(k => k === 'renderedHtml' || equalish(a[k], b[k]))
  );
};

const equalishArray = (a: any[], b: any[]): boolean =>
  a.length === b.length && a.every((o, i) => equalish(o, b[i]));

// This checks to see if any pending edits will overwrite edits that have been merged into
// the local project graph from upstream after the edits were initially made. If there are
// conflicts it prompts the user.
export const safeSaveProjectGraphEdits =
  (andThen: Thunk = () => void 0, options: { autoSave?: boolean } = {}): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits, configuration } = getState();
    const titles = new Array<string>();
    if (configuration.realTime) {
      for (const [name, edit] of Object.entries(graphEdits.editNodes)) {
        const original = graphEdits.originalNodes[name];
        const current = projectGraph.nodes[name];
        if (Object.keys(edit).some(key => !equalish(original?.data[key], current?.data[key]))) {
          titles.push(original.data.title);
        }
      }
    }
    if (titles.length) {
      const overwriteChangesConfig: OverwriteChangesModalData = {
        titles,
        callback: save => {
          if (save) {
            trackAuthoringEvent(`Narrative Editor - Overwrite`);
            dispatch(saveProjectGraphEdits(andThen, options));
          }
        },
      };
      dispatch(openModal(ModalIds.OverwriteChanges, overwriteChangesConfig));
    } else {
      dispatch(saveProjectGraphEdits(andThen, options));
    }
  };

export const saveProjectGraphEdits =
  (andThen: Thunk, options: { autoSave?: boolean; unsafe?: boolean }): Thunk =>
  async (dispatch, getState) => {
    const { configuration, layout, projectGraph, graphEdits } = getState();
    const { dirty, editNodes } = graphEdits;
    if (!dirty) {
      dispatch(andThen);
      return;
    }
    const rootEdit = editNodes[projectGraph.rootNodeName];
    const projectStatusChanged = rootEdit && has(rootEdit, 'projectStatus');
    const { realTime } = configuration;

    dispatch(setProjectGraphSaving(true));

    return (
      prepareSaveOps(projectGraph, graphEdits)
        .then(ops =>
          AuthoringOpsService.postWriteOps(
            ops,
            options.unsafe || !realTime ? undefined : projectGraph.commit.id
          )
        )
        //.then(r => new Promise<WriteOpsResponse>(resolve => setTimeout(() => resolve(r), 1000))) // simulate latency
        .then(response => {
          dispatch(
            resetProjectGraphEdits(
              options.autoSave,
              !realTime ? undefined : response.commit.id,
              response.squashed,
              response.squashed && isEmpty(response.nodes),
              true,
              response.commit.id // new remote head
            )
          );
          if (response.priorCommit.id === projectGraph.commit.id) {
            EdgeEditorService.sendEdgesToStructurePanel(response);
          } else {
            return loadStructure(projectGraph.branchId, projectGraph.homeNodeName).then(content => {
              dispatch(receiveProjectGraph(content, false));
            });
          }
        })
        .then(() =>
          // Update the Project status to match the project status in the graph.
          // This is the worst but no clean way to sync the two...
          projectStatusChanged && layout.userCanEditSettings
            ? putProjectStatus(
                layout.project.id,
                configuration.projectStatuses[rootEdit.projectStatus]
              ).then(project => {
                dispatch({ type: UPDATE_BRANCH, layout: { project } });
              })
            : null
        )
        .then(() => {
          dispatch(setProjectGraphSaving(false, false));
          if (!options.autoSave) dispatch(openToast('Edits saved.', 'success'));
          dispatch(andThen);
        })
        .catch(e => {
          if (e.status === 409) {
            trackAuthoringEvent(`Narrative Editor - Save`, 'Conflict');
            dispatch(setProjectGraphSaving(false, true));
            dispatch(promptMergeOrOverwrite(andThen, options));
          } else {
            console.log(e);
            dispatch(setProjectGraphSaving(false, true));
            dispatch(openToast('Edits failed to save.', 'danger'));
          }
        })
    );
  };

export const UNDO_GRAPH_EDIT_COMMIT = 'UNDO_GRAPH_EDIT_COMMIT';

const reverseCommit = (branchId: number, commitId: number): Promise<WriteOpsResponse> =>
  gretchen
    .post('/api/v2/authoring/:branchId/commits/:commitId/reverse')
    .params({ branchId, commitId })
    .data({})
    .exec();

export const undoGraphEditCommit = (): Thunk => (dispatch, getState) => {
  const { projectGraph, graphEdits } = getState();

  dispatch(setProjectGraphSaving(true));
  return reverseCommit(projectGraph.branchId, graphEdits.undos[0].commitId)
    .then(response => {
      dispatch({
        type: UNDO_GRAPH_EDIT_COMMIT,
        commitId: response.commit.id,
      });
      return response;
    })
    .then(response => {
      if (response.priorCommit.id === projectGraph.commit.id) {
        EdgeEditorService.sendEdgesToStructurePanel(response);
      } else {
        return loadStructure(projectGraph.branchId, projectGraph.homeNodeName).then(content => {
          dispatch(receiveProjectGraph(content, false));
        });
      }
    })
    .then(() => {
      dispatch(setProjectGraphSaving(false));
    })
    .catch(e => {
      console.log(e);
      dispatch(setProjectGraphSaving(false));
      dispatch(openToast('Undo failed.', 'danger'));
    });
};

// Prompt the user to merge updates from upstream or force save without checking for conflicts.
const promptMergeOrOverwrite =
  (andThen: Thunk, options: { autosave?: boolean; unsafe?: boolean }): Thunk =>
  dispatch => {
    const modalConfig: MergeUpdatesModalData = {
      callback: action => {
        if (action === 'Save') {
          trackAuthoringEvent(`Narrative Editor - Save`, 'Force');
          dispatch(saveProjectGraphEdits(andThen, { ...options, unsafe: true }));
        } else if (action === 'Merge') {
          trackAuthoringEvent(`Narrative Editor - Save`, 'Merge');
          dispatch(fetchStructure());
        }
      },
    };
    dispatch(openModal(ModalIds.MergeUpdates, modalConfig));
  };

const putProjectStatus = (id: number, status?: string): Promise<Project> =>
  gretchen // This is the worst but no clean way to sync the two...
    .put('/api/v2/authoring/projects/:id/status')
    .params({ id })
    .data(status ?? null)
    .exec()
    .then(({ projects: [{ project }] }) => project);

export const SET_PROJECT_GRAPH_REMOTE_HEAD = 'SET_PROJECT_GRAPH_REMOTE_HEAD';

export const setProjectGraphRemote =
  (remoteHead?: number): Thunk =>
  (dispatch, getState) => {
    if (remoteHead !== getState().graphEdits.remoteHead)
      dispatch({
        type: SET_PROJECT_GRAPH_REMOTE_HEAD,
        remoteHead,
      });
  };

export const pollForHeadCommit = (): Thunk => (dispatch, getState) => {
  const { layout, projectStructure, configuration } = getState();
  const branchId = layout.branchId ? parseInt(layout.branchId) : 0;
  if (branchId > 0 && !projectStructure.isFetching && configuration.realTime) {
    // if we become active, recheck the remote head just in case presence didn't feed us
    gretchen
      .get('/api/v2/authoring/branches/:branchId/head')
      .headers({ 'X-No-Session-Extension': true })
      .params({ branchId })
      .silent()
      .exec()
      .then(({ id }) => dispatch(setProjectGraphRemote(id)));
  }
};
