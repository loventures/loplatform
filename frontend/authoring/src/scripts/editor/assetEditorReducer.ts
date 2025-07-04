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

import { mapValues, sortBy } from 'lodash';

import { AssetNode, NodeName } from '../types/asset';
import { Includes } from '../types/edge';
import {
  ASSET_EDITOR_CLEAN,
  ASSET_EDITOR_DIRTY,
  RELOAD_ASSET_EDITOR,
  SAVE_ASSETS_SUCCESS,
  SAVE_ASSET_ERROR,
  SAVE_ASSET_START,
  SET_CURRENT_ASSET_NODE,
  SET_CURRENT_INCLUDES,
  SET_NEW_ASSET_AND_INCLUDES,
  UPDATE_ASSET_CATEGORY,
  UPDATE_INCLUDES,
} from './assetEditorActions';

export interface AssetEditorState {
  assetNode: AssetNode;
  includes: Includes;
  category: NodeName | null | undefined; // updated gradebook category
  dirty: boolean;
  saving: boolean;
  reload: number;
}

const initialState: AssetEditorState = {
  assetNode: {} as AssetNode,
  includes: {} as Includes,
  category: undefined,
  dirty: false,
  saving: false,
  reload: 0,
};

const sortIncludes = (includes: Includes) => {
  return mapValues(includes, edges => sortBy(edges, edge => edge.position)) as Includes;
};

export default function assetEditor(
  state: AssetEditorState = initialState,
  action
): AssetEditorState {
  switch (action.type) {
    case SET_CURRENT_ASSET_NODE: {
      return {
        ...state,
        assetNode: action.assetNode,
      };
    }
    case SET_CURRENT_INCLUDES: {
      return {
        ...state,
        includes: sortIncludes(action.includes),
      };
    }
    case SET_NEW_ASSET_AND_INCLUDES: {
      return {
        ...state,
        assetNode: action.asset,
        includes: sortIncludes(action.includes),
      };
    }
    case UPDATE_INCLUDES: {
      return {
        ...state,
        includes: {
          ...state.includes,
          [action.group]: action.edges,
        },
      };
    }
    case SAVE_ASSET_START: {
      return {
        ...state,
        saving: true,
      };
    }
    case SAVE_ASSETS_SUCCESS: {
      // in squash case the asset may not be updated and so may not exist
      const assetNode = action.updatedAssetNodes.find(
        asset => asset.name === action.currentAssetName
      );
      return {
        ...state,
        saving: false,
        dirty: false,
        assetNode: assetNode ?? state.assetNode,
      };
    }
    case SAVE_ASSET_ERROR: {
      return {
        ...state,
        saving: false,
        dirty: true,
      };
    }
    case ASSET_EDITOR_DIRTY: {
      return {
        ...state,
        dirty: true,
      };
    }
    case ASSET_EDITOR_CLEAN: {
      return {
        ...state,
        dirty: false,
      };
    }
    case RELOAD_ASSET_EDITOR: {
      return {
        ...state,
        reload: state.reload + 1,
      };
    }
    case UPDATE_ASSET_CATEGORY: {
      return {
        ...state,
        category: action.category,
        dirty: true,
      };
    }
    default: {
      return state;
    }
  }
}
