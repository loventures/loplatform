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

import { Action } from 'redux';

import { INITIALIZE_DCM } from '../dcmStoreConstants';
import { ContentAccessRights, ContentStatus } from '../story/contentStatus';
import { AfterEdge, BeforeEdge, EdgePosition } from '../types/api';
import { AssetNodeData, BlobRef, EdgeName, NewAsset, NodeName } from '../types/asset';
import { EdgeData, EdgeGroup, NewEdge } from '../types/edge';
import {
  ADD_PROJECT_GRAPH_EDGE,
  ADD_PROJECT_GRAPH_NODE,
  BEGIN_PROJECT_GRAPH_EDIT,
  DELETE_PROJECT_GRAPH_EDGE,
  DISCARD_PROJECT_GRAPH_EDIT,
  EDIT_PROJECT_GRAPH_EDGE_DATA,
  EDIT_PROJECT_GRAPH_NODE_DATA,
  PUT_NODE_BLOB_REF,
  RECEIVE_GRAPH_EDIT_CACHE,
  REDO_PROJECT_GRAPH_EDIT,
  RESET_PROJECT_GRAPH_EDITS,
  RESTORE_PROJECT_GRAPH_NODE,
  SET_PROJECT_GRAPH_EDGE_ORDER,
  SET_PROJECT_GRAPH_ORIGINS,
  SET_PROJECT_GRAPH_REMOTE_HEAD,
  SET_PROJECT_GRAPH_SAVING,
  UNDO_GRAPH_EDIT_COMMIT,
  UNDO_PROJECT_GRAPH_EDIT,
} from './graphEditActions';

export const isBeforeEdge = (edge?: EdgePosition): edge is BeforeEdge => (edge as any)?.before;
export const isAfterEdge = (edge?: EdgePosition): edge is AfterEdge => (edge as any)?.after;

export type ContextPath = string; // context id including the asset itself

// Just elements of the content tree (elements, surveys, questions)
export type ContentTree = {
  loaded: boolean;
  page: Record<ContextPath, number>; // page number in parent
  count: Record<ContextPath, number>; // count of children
  playlist: Array<ContextPath>; // content order
  contextPaths: Record<NodeName, ContextPath>; // context path
  accessRights: Record<NodeName, ContentAccessRights>; // access rights by content
  contentStatuses: Record<NodeName, ContentStatus>; // effective content status
  contentReuse: Set<NodeName>; // reused nodes
};

const emptyContentTree: ContentTree = {
  loaded: false,
  page: {},
  count: {},
  playlist: [],
  contextPaths: {},
  accessRights: {},
  contentStatuses: {},
  contentReuse: new Set(),
};

export type FakeEdge = Pick<NewEdge, 'sourceName' | 'group' | 'data' | 'targetName'>;

export type Undo = {
  comment: string; // description of the edit this undo replaces
  commitId: number;
};

type BaseProjectGraphEditState = {
  addNodes: Record<NodeName, NewAsset<any>>; // added nodes
  editNodes: Record<NodeName, AssetNodeData>; // edited node data
  originalNodes: Record<NodeName, NewAsset<any>>; // record of the node at edit time
  addEdges: Record<NodeName, Array<NewEdge>>; // from node to added edges
  editEdges: Record<EdgeName, EdgeData<any>>; // edited edge date
  deleteEdges: Record<NodeName, Record<EdgeName, NewEdge>>; // from node to deleted edges
  edgeOrders: Record<NodeName, Partial<Record<EdgeGroup, EdgeName[]>>>; // replacement edge orders
  restoredNodes: Record<NodeName, NewAsset<any>>; // placeholder for nodes restored from prior commit
  editedAssets: Record<NodeName, NewAsset<any>>;
  editedInEdges: Record<NodeName, NewEdge[]>;
  editedOutEdges: Record<NodeName, NewEdge[]>;
  contentTree: ContentTree;
  unsaved: Action[];
  undo?: ProjectGraphEditStateHistory; // undo state
};

type ProjectGraphEditStateHistory = BaseProjectGraphEditState & {
  comment: string; // description of the edit this undo replaces
  key?: string; // a key that allows multiple edits of one field to coalesce into a single undo
};

// This could be modeled as just a list of write ops, but slower and messier.
export type ProjectGraphEditState = BaseProjectGraphEditState & {
  initial: boolean;
  dirty: boolean;
  saving: boolean;
  problem: boolean;
  // map from UI names to copy-source uuids, allows HTML to render
  origins: Record<NodeName, NodeName>;
  blobRefs: Record<NodeName, BlobRef>;
  // this is for when a major change (e.g. reset or undo) has happened to the edit
  // state and so inputs that have defaultValue instead of bound value need to reset
  generation: number;
  undos: Undo[];
  redos: Undo[];
  redo?: ProjectGraphEditStateHistory; // the next redo is redo.undo(!)
  remoteHead?: number; // remote head commit id
};

const initialState: ProjectGraphEditState = {
  addNodes: {},
  editNodes: {},
  originalNodes: {},
  addEdges: {},
  editEdges: {},
  deleteEdges: {},
  edgeOrders: {},
  restoredNodes: {},
  editedAssets: {},
  editedInEdges: {},
  editedOutEdges: {},
  contentTree: emptyContentTree,
  unsaved: [],
  origins: {},
  blobRefs: {},
  undos: [],
  redos: [],
  initial: true,
  dirty: false,
  saving: false,
  problem: false,
  generation: 0,
};

export const noGraphEdits = initialState;

export default function graphEditReducer(state = initialState, action): ProjectGraphEditState {
  switch (action.type) {
    case INITIALIZE_DCM: {
      return initialState;
    }

    case RESET_PROJECT_GRAPH_EDITS: {
      const { autoSave, squashed, idempotent, commitId, remoteHead } = action;
      // The entire undo history has been written in one commit so the undo message is pedantically all of
      // them  squashed together. Usually this is one edit but if you went offline it could be multiple edits.
      const loop = (undo: ProjectGraphEditStateHistory | undefined): number =>
        undo ? 1 + loop(undo.undo) : 0;
      const count = loop(state.undo);
      const comment = count === 1 ? state.undo!.comment : `${count} edits`;
      // We prefer the original comment when squashed because Added X + Edited X should remain Added X
      const undos = commitId
        ? idempotent
          ? state.undos
          : squashed && state.undos.length
            ? [{ ...state.undos[0], commitId }, ...state.undos.slice(1)]
            : [{ commitId, comment }, ...state.undos]
        : [];
      return {
        ...initialState,
        generation: autoSave ? state.generation : 1 + state.generation,
        initial: state.initial,
        saving: false,
        problem: false,
        remoteHead: remoteHead ?? state.remoteHead,
        undos,
        redos: [], // after a save there's no redo
      };
    }

    // TODO: some of these should not be available while saving
    case UNDO_GRAPH_EDIT_COMMIT: {
      const { commitId } = action;
      const [redo, ...undos] = state.undos;
      const redos = [{ ...redo, commitId }, ...state.redos];
      return {
        ...initialState,
        generation: 1 + state.generation,
        saving: false,
        problem: false,
        remoteHead: state.remoteHead,
        undos,
        redos,
      };
    }

    case SET_PROJECT_GRAPH_REMOTE_HEAD: {
      return { ...state, remoteHead: action.remoteHead };
    }

    case RECEIVE_GRAPH_EDIT_CACHE: {
      const { editedAssets, editedInEdges, editedOutEdges, contentTree } = action;
      return {
        ...state,
        editedAssets: editedAssets ?? state.editedAssets,
        editedInEdges: editedInEdges ?? state.editedInEdges,
        editedOutEdges: editedOutEdges ?? state.editedOutEdges,
        contentTree: contentTree ?? state.contentTree,
      };
    }

    case ADD_PROJECT_GRAPH_NODE: {
      const node = action.node as NewAsset<any>;
      return {
        ...state,
        dirty: true,
        addNodes: {
          ...state.addNodes,
          [node.name]: node,
        },
        unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
      };
    }

    case RESTORE_PROJECT_GRAPH_NODE: {
      const node = action.node as NewAsset<any>;
      return {
        ...state,
        restoredNodes: {
          ...state.restoredNodes,
          [node.name]: node,
        },
        unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
      };
    }

    case ADD_PROJECT_GRAPH_EDGE: {
      const edge = action.edge as NewEdge;
      const { sourceName } = edge;
      const edges = [...(state.addEdges[sourceName] ?? []), edge];
      return {
        ...state,
        dirty: true,
        addEdges: {
          ...state.addEdges,
          [sourceName]: edges,
        },
        unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
      };
    }

    case DELETE_PROJECT_GRAPH_EDGE: {
      const edge = action.edge as NewEdge;
      const edgeOrder = action.edgeOrder as EdgeName[];
      const { sourceName, group, name } = edge;

      // if an edge order includes this edge, remove it
      const setOrder = state.edgeOrders[sourceName]?.[group];
      const edgeOrders = !setOrder
        ? state.edgeOrders
        : {
            ...state.edgeOrders,
            [sourceName]: {
              ...state.edgeOrders[sourceName],
              [group]: setOrder.filter(n => n !== name),
            },
          };

      // if an add edge is relative to this then rewrite it to be relative
      // to the next or previous or ...
      const tweakAddEdges = state.addEdges[sourceName]?.find(
        e =>
          e.name === name ||
          (isBeforeEdge(e.newPosition) && e.newPosition.before === name) ||
          (isAfterEdge(e.newPosition) && e.newPosition.after === name)
      );
      const index = edgeOrder.findIndex(e => e === name);
      const before = edgeOrder[index + 1];
      const after = edgeOrder[index - 1];
      const addEdges = !tweakAddEdges
        ? state.addEdges
        : {
            ...state.addEdges,
            [sourceName]: state.addEdges[sourceName]
              .filter(e => e.name !== name)
              .map(e =>
                isBeforeEdge(e.newPosition) && e.newPosition.before === name
                  ? { ...e, newPosition: before ? { before } : 'start' }
                  : isAfterEdge(e.newPosition) && e.newPosition.after === name
                    ? { ...e, newPosition: after ? { after } : 'end' }
                    : e
              ),
          };

      if (state.addEdges[sourceName]?.some(edge => edge.name === name)) {
        return {
          ...state,
          dirty: true,
          addEdges,
          edgeOrders,
          unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
        };
      } else {
        const edges = {
          ...(state.deleteEdges[sourceName] ?? {}),
          [name]: edge,
        };
        return {
          ...state,
          dirty: true,
          deleteEdges: {
            ...state.deleteEdges,
            [sourceName]: edges,
          },
          addEdges,
          edgeOrders,
          unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
        };
      }
    }

    case SET_PROJECT_GRAPH_EDGE_ORDER: {
      const name = action.name as NodeName;
      const group = action.group as EdgeGroup;
      const order = action.order as EdgeName[];
      const orig = state.edgeOrders[name];
      // If there are unsaved relative add-edges then remove their newPosition in favour of this new total order
      const hasPosition = state.addEdges[name]?.find(e => e.group === group && e.newPosition);
      const addEdges = !hasPosition
        ? state.addEdges
        : {
            ...state.addEdges,
            [name]: state.addEdges[name].map(e =>
              e.group === group && e.newPosition ? { ...e, newPosition: undefined } : e
            ),
          };
      return {
        ...state,
        dirty: true,
        edgeOrders: {
          ...state.edgeOrders,
          [name]: {
            ...(orig ?? {}),
            [group]: order,
          },
        },
        addEdges,
        unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
      };
    }

    case EDIT_PROJECT_GRAPH_NODE_DATA: {
      const { name, data, original } = action;
      const added = state.addNodes[name];
      if (added) {
        return {
          ...state,
          dirty: true,
          addNodes: {
            ...state.addNodes,
            [name]: {
              ...added,
              data: {
                ...added.data,
                ...data,
              },
            },
          },
          unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
        };
      } else {
        const { editNodes, originalNodes } = state;
        const edited = editNodes[name];
        return {
          ...state,
          dirty: true,
          originalNodes: originalNodes[name]
            ? originalNodes
            : { ...originalNodes, [name]: original },
          editNodes: {
            ...state.editNodes,
            [name]: edited ? { ...edited, ...data } : data,
          },
          unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
        };
      }
    }

    case EDIT_PROJECT_GRAPH_EDGE_DATA: {
      const { sourceName, name, data } = action;
      if (state.addEdges[sourceName]?.some(edge => edge.name === name)) {
        return {
          ...state,
          dirty: true,
          addEdges: {
            ...state.addEdges,
            [sourceName]: state.addEdges[sourceName].map(edge =>
              edge.name === name
                ? {
                    ...edge,
                    data: { ...edge.data, ...data },
                  }
                : edge
            ),
          },
          unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
        };
      } else {
        const edited = state.editEdges[name];
        return {
          ...state,
          dirty: true,
          editEdges: {
            ...state.editEdges,
            [name]: edited ? { ...edited, ...data } : data,
          },
          unsaved: state.saving ? state.unsaved.concat(action) : state.unsaved,
        };
      }
    }

    case SET_PROJECT_GRAPH_SAVING: {
      const { saving, problem } = action;
      return {
        ...state,
        saving,
        unsaved: [], // TODO: <- ...
        initial: false,
        problem: problem ?? state.problem,
      };
    }

    case SET_PROJECT_GRAPH_ORIGINS: {
      const { origins: updates } = action;
      const origins = { ...state.origins };
      // handle case of a copy of a copy...
      for (const [key, value] of Object.entries(updates as Record<NodeName, NodeName>)) {
        origins[key] = origins[value] ?? value;
      }
      return {
        ...state,
        origins,
      };
    }

    case PUT_NODE_BLOB_REF: {
      const { name, blobRef } = action;
      return {
        ...state,
        blobRefs: { ...state.blobRefs, [name]: blobRef },
      };
    }

    case BEGIN_PROJECT_GRAPH_EDIT: {
      const { comment, key } = action;
      const {
        undo,
        addNodes,
        editNodes,
        originalNodes,
        addEdges,
        editEdges,
        deleteEdges,
        edgeOrders,
        restoredNodes,
        editedAssets,
        editedInEdges,
        editedOutEdges,
        contentTree,
        unsaved,
      } = state;
      return key && undo?.key === key
        ? state
        : {
            ...state,
            undo: {
              comment,
              key,
              addNodes,
              editNodes,
              originalNodes,
              addEdges,
              editEdges,
              deleteEdges,
              edgeOrders,
              restoredNodes,
              editedAssets,
              editedInEdges,
              editedOutEdges,
              contentTree,
              unsaved,
              undo,
            },
            redo: undefined,
          };
    }

    case DISCARD_PROJECT_GRAPH_EDIT: {
      const {
        undo: { key, ...undo },
        generation,
      } = state;
      return key !== action.key
        ? state
        : {
            ...state,
            ...undo,
            redo: undefined,
            generation: 1 + generation,
            dirty: !!undo.undo,
            problem: !undo.undo ? false : state.problem,
          };
    }

    case UNDO_PROJECT_GRAPH_EDIT: {
      const {
        undo: { comment, key, ...undo },
        redo,
        generation,
        addNodes,
        editNodes,
        originalNodes,
        addEdges,
        editEdges,
        deleteEdges,
        edgeOrders,
        restoredNodes,
        editedAssets,
        editedInEdges,
        editedOutEdges,
        unsaved,
        contentTree,
      } = state;
      return {
        ...state,
        ...undo,
        redo: {
          comment,
          key,
          addNodes,
          editNodes,
          originalNodes,
          addEdges,
          editEdges,
          deleteEdges,
          edgeOrders,
          restoredNodes,
          editedAssets,
          editedInEdges,
          editedOutEdges,
          contentTree,
          unsaved,
          undo: redo,
        },
        generation: 1 + generation,
        dirty: !!undo.undo,
        initial: false,
      };
    }

    case REDO_PROJECT_GRAPH_EDIT: {
      const {
        undo,
        redo: { comment, key, undo: next, ...redo },
        generation,
        addNodes,
        editNodes,
        originalNodes,
        addEdges,
        editEdges,
        deleteEdges,
        edgeOrders,
        restoredNodes,
        editedAssets,
        editedInEdges,
        editedOutEdges,
        unsaved,
        contentTree,
      } = state;
      return {
        ...state,
        ...redo,
        redo: next,
        undo: {
          comment,
          key,
          addNodes,
          editNodes,
          originalNodes,
          addEdges,
          editEdges,
          deleteEdges,
          edgeOrders,
          restoredNodes,
          editedAssets,
          editedInEdges,
          editedOutEdges,
          contentTree,
          unsaved,
          undo,
        },
        generation: 1 + generation,
        dirty: true,
      };
    }

    default:
      return state;
  }
}
