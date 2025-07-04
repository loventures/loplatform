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

import { AssetNode } from '../types/asset';
import { EdgeGroup, FullEdge } from '../types/edge';
import { setAssetEditorDirty, setCurrentAssetNode, updateIncludes } from './assetEditorActions';
import { dcmStore } from '../dcmStore';

/**
 * This service replaces assetEditState and AssetInEditService from old angular days.
 *
 * We store values such as filesave callbacks and setPristine functions as state in this singleton.
 *
 * Redux is responsible for holding values that are used to render (namely asset and includes).
 *
 * */
class AssetInEditService {
  public updateAsset(asset: AssetNode, dirty = false) {
    dcmStore.dispatch(setCurrentAssetNode(asset));
    dirty && this.triggerDirtyAsset();
  }

  /** Deprecated */
  public replaceEdges(edges: FullEdge[], group: EdgeGroup): FullEdge[] {
    dcmStore.dispatch(updateIncludes(edges, group));
    return edges;
  }

  public triggerDirtyAsset() {
    dcmStore.dispatch(setAssetEditorDirty());
  }
}

export default new AssetInEditService();
