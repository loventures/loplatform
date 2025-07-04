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

import { has, minBy } from 'lodash';

import { INITIALIZE_DCM } from '../dcmStoreConstants';
import { ContextPath } from '../graphEdit/graphEditReducer';
import { Project } from '../layout/dcmLayoutReducer';
import { CommitSegment } from '../revision/revision';
import { NewAsset, NodeName } from '../types/asset';
import { EdgeData, EdgeGroup, NewAssetWithEdge } from '../types/edge';
import {
  SESSION_EDIT_MODE,
  SESSION_FLAG_MODE,
  SESSION_INLINE_VIEW,
  SESSION_KEYWORDS,
  SESSION_OMEGA_EDIT,
  SESSION_SYNCHRONOUS,
  SET_NARRATIVE_ACTIVE,
  SET_NARRATIVE_ASSET_STATE,
  SET_NARRATIVE_STATE,
} from './storyActions';

export type NarrativeAssetState = {
  created?: boolean; // was this asset just created, used to animate in and autofocus
  deleted?: boolean; // was this asset just deleted, used to animate away
  collapsed?: boolean; // was this asset just collapsed, used to animate
  gated?: boolean; // was a gate just adedd, used to scroll to
  expanded?: boolean; // are this asset's children expanded
  invalid?: boolean; // is this asset invalid, e.g. no correct choices
  renderAll?: boolean; // expanding parent so must render all children
  previewing?: boolean; // preview sidebar
  keywording?: boolean; // editing keywords
};

// This is garbage but basically I have to copy in some edge data for how this
// copied asset was reached in order to clone it
export type CopiedAsset = NewAsset<any> & {
  edgeTraverse?: boolean;
  edgeId?: string;
  edgeData?: EdgeData<any>;
  edgeTargets: Partial<Record<EdgeGroup, Array<CopiedAsset>>>;
};

export const isCopiedAsset = (asset: NewAsset<any>): asset is CopiedAsset =>
  (asset as any).edgeTargets != null;

export interface StoryState {
  editMode: boolean; // editing mode
  inlineView: boolean; // inline view
  omegaEdit: boolean; // ninja raptor edit mode
  synchronous: boolean; // synchronous edit mode
  offline: boolean;
  flagMode: boolean; // international flags
  keyWords: boolean; // show keywords
  findCount: number; // increment whenever command F find is pressed
  scrollTo?: ContextPath; // expanding parent/child, scroll to this node aggressively
  clipboard?: CopiedAsset | NewAssetWithEdge<any>; // the last "cut" asset that can be pasted in
  pasteboard?: NodeName; // the last pasted cut asset, needed for project graph fetch
  addingTo?: NodeName; // on which asset should the add asset popup be open
  activeIFrame?: string; // active iframe to handle click to edit / blur to unedit
  activeNode?: NodeName;
  activeContextPath?: ContextPath;
  assetOffsets: Record<ContextPath, number>; // in inline view, distance off asset from center of screen
  assetStates: Record<NodeName, NarrativeAssetState>;
  revisionHistory?: {
    name: NodeName;
    history: CommitSegment[];
  };
}

const initialState: StoryState = {
  editMode: true,
  inlineView: false,
  omegaEdit: false,
  synchronous: false,
  offline: false,
  flagMode: false,
  keyWords: false,
  findCount: 0,
  assetStates: {},
  assetOffsets: {},
};

export default function storyReducer(state: StoryState = initialState, action): StoryState {
  switch (action.type) {
    case INITIALIZE_DCM: {
      // all the ? are for the crap test that does a bad initialization
      const self: number = action.user?.profile?.id;
      const project: Project = action.layout?.project;
      const userCanEdit: boolean = action.layout?.userCanEdit;
      const isMemberOrOwner = project?.ownedBy === self || has(project?.contributedBy, self);
      const editModeDefault = action.user?.preferences?.authoringPreferences?.editModeDefault;
      const ssMode = sessionStorage.getItem(SESSION_EDIT_MODE(project?.id));
      // Default to view-mode if it's not your project or it is live. Session storage overrides.
      const editMode =
        ssMode != null
          ? ssMode === 'true'
          : editModeDefault || (project?.liveVersion !== 'Live' && isMemberOrOwner && userCanEdit);
      const inlineView = sessionStorage.getItem(SESSION_INLINE_VIEW) === 'true';
      const omegaEdit = sessionStorage.getItem(SESSION_OMEGA_EDIT) === 'true';
      const synchronous =
        sessionStorage.getItem(SESSION_SYNCHRONOUS) == null
          ? !action.configuration.semiRealTime
          : sessionStorage.getItem(SESSION_SYNCHRONOUS) === 'true';
      const flagMode = sessionStorage.getItem(SESSION_FLAG_MODE) === 'true';
      const keyWords = sessionStorage.getItem(SESSION_KEYWORDS) === 'true';
      (window as any).flagMode = flagMode; // eesh

      return {
        ...state,
        editMode,
        inlineView,
        omegaEdit,
        synchronous,
        flagMode,
        keyWords,
      };
    }

    case SET_NARRATIVE_STATE: {
      const { state: update } = action;
      return {
        ...state,
        ...update,
      };
    }

    case SET_NARRATIVE_ACTIVE: {
      const { name, contextPath, delta } = action;
      const assetOffsets = { ...state.assetOffsets };
      const fullPath = contextPath ? `${contextPath}.${name}` : name;
      if (delta == null) delete assetOffsets[fullPath];
      else assetOffsets[fullPath] = delta;

      const active = minBy(Object.entries(assetOffsets), 1)?.[0];
      const index = active?.lastIndexOf('.');

      return active
        ? {
            ...state,
            assetOffsets,
            activeNode: active.substring(1 + index),
            activeContextPath: index < 0 ? '' : active.substring(0, index),
          }
        : {
            ...state,
            assetOffsets,
            activeNode: undefined,
            activeContextPath: undefined,
          };
    }

    case SET_NARRATIVE_ASSET_STATE: {
      const { name, state: update } = action;
      const assetStates = { ...state.assetStates };
      assetStates[name] = { ...(assetStates[name] ?? {}), ...update };
      return {
        ...state,
        assetStates,
      };
    }

    default:
      return state;
  }
}
