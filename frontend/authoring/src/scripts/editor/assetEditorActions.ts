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

import { Thunk } from '../types/dcmState';

export const SET_CURRENT_ASSET_NODE = 'SET_CURRENT_ASSET_NODE';

export function setCurrentAssetNode(assetNode) {
  return {
    type: SET_CURRENT_ASSET_NODE,
    assetNode,
  };
}

export const SET_CURRENT_INCLUDES = 'SET_CURRENT_INCLUDES';

export function setCurrentIncludes(includes) {
  return {
    type: SET_CURRENT_INCLUDES,
    includes,
  };
}

export const SET_NEW_ASSET_AND_INCLUDES = 'SET_NEW_ASSET_AND_INCLUDES';

export function setNewAssetAndIncludes(asset, includes) {
  return {
    type: SET_NEW_ASSET_AND_INCLUDES,
    asset,
    includes,
  };
}

export const UPDATE_INCLUDES = 'UPDATE_INCLUDES';

export function updateIncludes(edges, group) {
  return {
    type: UPDATE_INCLUDES,
    edges,
    group,
  };
}

export const SAVE_ASSET_START = 'SAVE_ASSET_START';

export function saveAssetStart() {
  return {
    type: SAVE_ASSET_START,
  };
}

export const SAVE_ASSETS_SUCCESS = 'SAVE_ASSETS_SUCCESS';

export function saveAssetsSuccess(updatedAssetNodes, currentAssetName) {
  return {
    type: SAVE_ASSETS_SUCCESS,
    updatedAssetNodes,
    currentAssetName,
  };
}

export const SAVE_ASSET_ERROR = 'SAVE_ASSET_ERROR';

export function saveAssetError() {
  return {
    type: SAVE_ASSET_ERROR,
  };
}

export const ASSET_EDITOR_DIRTY = 'ASSET_EDITOR_DIRTY';

export function setAssetEditorDirty(): Thunk {
  return (dispatch, getState) => {
    if (!getState().assetEditor.dirty) {
      dispatch({
        type: ASSET_EDITOR_DIRTY,
      });
    }
  };
}

export const ASSET_EDITOR_CLEAN = 'ASSET_EDITOR_CLEAN';

export function setAssetEditorClean() {
  return {
    type: ASSET_EDITOR_CLEAN,
  };
}

export const RELOAD_ASSET_EDITOR = 'RELOAD_ASSET_EDITOR';

export function reloadAssetEditor() {
  return {
    type: RELOAD_ASSET_EDITOR,
  };
}

export const UPDATE_ASSET_CATEGORY = 'UPDATE_ASSET_CATEGORY';

export function updateAssetCategory(category: string | null | undefined) {
  return {
    type: UPDATE_ASSET_CATEGORY,
    category,
  };
}
