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
import { uniqueId } from 'lodash';

import {
  AddEdgeWriteOp,
  AddNodeWriteOp,
  DeleteEdgeWriteOp,
  EdgePosition,
  SetEdgeDataWriteOp,
  SetEdgeOrderWriteOp,
  SetNodeDataWriteOp,
  WriteOp,
  WriteOpsResponse,
} from '../types/api';
import { AssetNode } from '../types/asset';
import { EdgeGroup, NewEdge } from '../types/edge';
import { generateCompleteAsset } from './AssetGenerators';
import { getBranchId } from '../router/ReactRouterService';

class AuthoringOpsService {
  public postWriteOps(ops: WriteOp[], commit?: number): Promise<WriteOpsResponse> {
    return gretchen
      .post('/api/v2/authoring/:branchId/write')
      .params({ branchId: getBranchId(), commit })
      .data(ops)
      .exec();
  }

  public updateNodeOp(asset): SetNodeDataWriteOp {
    return {
      op: 'setNodeData',
      name: asset.name,
      data: asset.data,
    };
  }

  public addNodeOp(asset): AddNodeWriteOp {
    return {
      op: 'addNode',
      typeId: asset.typeId,
      name: asset.name ? asset.name : uniqueId(),
      data: generateCompleteAsset(asset.data, asset.typeId),
    };
  }

  public addEdgeOp(
    sourceName: string,
    targetName: string,
    group: EdgeGroup,
    traverse = true,
    data = {},
    name = undefined,
    edgeId = undefined,
    position?: EdgePosition
  ): AddEdgeWriteOp {
    return {
      op: 'addEdge',
      name: name ?? uniqueId(),
      sourceName,
      targetName,
      group,
      traverse,
      data,
      edgeId,
      position,
    };
  }

  public addNewEdgeOp(edge: NewEdge): AddEdgeWriteOp {
    return this.addEdgeOp(
      edge.sourceName,
      edge.targetName,
      edge.group,
      edge.traverse,
      edge.data,
      edge.name,
      edge.edgeId,
      edge.newPosition
    );
  }

  public deleteEdgeOp(
    edgeName: string,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    notARealThingAllowRootification = false
  ): DeleteEdgeWriteOp {
    return {
      op: 'deleteEdge',
      name: edgeName,
    };
  }

  public setEdgeOrderOp(
    sourceName: string,
    group: EdgeGroup,
    ordering: string[]
  ): SetEdgeOrderWriteOp {
    return {
      op: 'setEdgeOrder',
      sourceName,
      group,
      ordering,
    };
  }

  public setEdgeDataOp(edgeName, edgeData): SetEdgeDataWriteOp {
    return {
      op: 'setEdgeData',
      name: edgeName,
      data: edgeData,
    };
  }

  public updateSingleAsset(asset: AssetNode) {
    return this.postWriteOps([this.updateNodeOp(asset)]);
  }
}

export const isDeleteEdgeWriteOp = (op: WriteOp): op is DeleteEdgeWriteOp => op.op === 'deleteEdge';

export default new AuthoringOpsService();
