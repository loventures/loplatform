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
import { uniq } from 'lodash';

import { FeedbackProfileDto } from '../feedback/FeedbackApi';
import { ApiQueryResults } from '../srs/apiQuery';
import { cap, plural, storyTypeName } from '../story/story';
import {
  AddDependencyWriteOp,
  AddEdgeWriteOp,
  AddNodeWriteOp,
  DeleteEdgeWriteOp,
  SetEdgeOrderWriteOp,
  SetNodeDataWriteOp,
  UpdateDependencyWriteOp,
  WriteOp,
} from '../types/api';
import { EdgeName, NewAsset, NodeName, TypeId } from '../types/asset';
import { EdgeGroup, NewEdge } from '../types/edge';
import { Polyglot } from '../types/polyglot';

export type ProjectOffering = {
  id: number;
  branchId: number;
  commitId: number;
  commitTime: string;
};

export const loadBranchOffering = (branchId: number): Promise<ProjectOffering | null> =>
  gretchen
    .get(`/api/v2/authoring/branches/:branchId/offering`)
    .params({
      branchId,
    })
    .exec()
    .catch(e => {
      console.log(e);
      if (e.status === 404) return null;
      throw e;
    });

export type CommitAssetInfo = { name: NodeName; typeId: TypeId; title?: string };

// technically Omit<WriteOp, "data"> but that makes life hard
export type CommitWriteOp = WriteOp & {
  target?: CommitAssetInfo;
  source?: CommitAssetInfo;
  asset?: CommitAssetInfo;
};

export type CommitSegment = {
  first: number;
  last: number;
  created: string;
  createdBy: FeedbackProfileDto;
  ops: Array<CommitWriteOp>;
  truncated: boolean;
  hidden: boolean;
};

export const loadNodeCommitHistory = (
  branchId: number,
  name: NodeName,
  detail?: boolean
): Promise<ApiQueryResults<CommitSegment>> =>
  gretchen
    .get(`/api/v2/authoring/branches/:branchId/nodes/:name/commitHistory`)
    .params({ branchId, name, detail })
    .exec();

export const loadCommitLog = (
  branchId: number,
  limit: number,
  after?: number
): Promise<ApiQueryResults<CommitSegment>> =>
  gretchen
    .get(`/api/v2/authoring/branches/:branchId/commits/log`)
    .params({
      branchId,
      limit,
      ...(after ? { after } : {}),
    })
    .exec();

export const revertBranch = (branchId: number, commitId: number, head: number): Promise<void> =>
  gretchen
    .post(`/api/v2/authoring/branches/:branchId/commits/:commitId/revert`)
    .params({
      branchId,
      commitId,
      head,
    })
    .exec();

export const loadSlimAsset = (
  branchId: number,
  name: NodeName,
  includeGroup?: EdgeGroup[]
): Promise<{ asset: NewAsset<any>; includes: Record<EdgeGroup, NewEdge[]> }> =>
  gretchen
    .get('/api/v2/authoring/:branchId/nodes/:name/slim')
    .params({ branchId, name, includeGroup })
    .exec();

export const isAddNode = (op: WriteOp): op is AddNodeWriteOp => op.op === 'addNode';
const isSetNodeData = (op: WriteOp): op is SetNodeDataWriteOp => op.op === 'setNodeData';
export const isAddEdge = (op: WriteOp): op is AddEdgeWriteOp => op.op === 'addEdge';
const isDeleteEdge = (op: WriteOp): op is DeleteEdgeWriteOp => op.op === 'deleteEdge';
const isSetEdgeOrder = (op: WriteOp): op is SetEdgeOrderWriteOp => op.op === 'setEdgeOrder';
export const isAddDependency = (op: WriteOp): op is AddDependencyWriteOp =>
  op.op === 'addDependency';
export const isUpdateDependency = (op: WriteOp): op is UpdateDependencyWriteOp =>
  op.op === 'updateDependency';

const countWhere = <A>(as: A[], pred: (a: A) => any) => {
  let count = 0;
  for (const a of as) if (pred(a)) ++count;
  return count;
};

const pluralIfNonZero = (n: number, prefix: string, suffix: string) =>
  n ? [`${prefix} ${plural(n, suffix)}`] : [];

export const updateDependencySummary = (op: UpdateDependencyWriteOp) => {
  const updated = pluralIfNonZero(op.narrativelyUpdatedNodeNames.length, 'updated', 'node');
  const added = pluralIfNonZero(op.narrativelyAddedNodeNames.length, 'added', 'node');
  const removed = pluralIfNonZero(op.narrativelyRemovedNodeNames.length, 'removed', 'node');

  return updated.concat(added).concat(removed).join(', ');
};

export const summarizeCommit = (
  polyglot: Polyglot,
  asset: NewAsset<any>,
  commit: CommitSegment,
  edgeTargets: Record<EdgeName, CommitAssetInfo>
): string => {
  // Content titles will always be their current titles, not their titles at
  // the time.
  const typeName = storyTypeName(polyglot, asset.typeId).toLowerCase();
  const changes = new Array<string>();
  for (const op of commit.ops) {
    if (isUpdateDependency(op)) {
      changes.push(
        `synchronized this ${typeName} with project ${op.projectCode ?? op.projectTitle}`
      );
    } else if (isAddNode(op) && op.name === asset.name) {
      changes.push(`created this ${typeName}`);
    } else if (isSetNodeData(op) && op.name === asset.name) {
      changes.push(`edited this ${typeName}`);
    } else if (isAddEdge(op) && op.sourceName === asset.name) {
      const targetName = op.target?.title ? `"${op.target?.title}"` : 'unknown';
      if (op.group === 'teaches' || op.group === 'assesses') {
        changes.push(`aligned this ${typeName} as ${targetName}`);
      } else if (op.group === 'gradebookCategory') {
        changes.push(`categorized this ${typeName} as ${targetName}`);
      } else if (op.group === 'elements') {
        changes.push(`added ${targetName} to this ${typeName}`);
      } else if (op.group === 'questions') {
        const questionType = storyTypeName(polyglot, op.target?.typeId).toLowerCase();
        changes.push(`added ${questionType} to this ${typeName}`);
      } else if (op.group === 'gates') {
        changes.push(`gated ${targetName} by this ${typeName}`);
      } else if (op.group === 'testsOut') {
        changes.push(`tested ${targetName} out by this ${typeName}`);
      } else if (op.group === 'hyperlinks') {
        // ignore
      }
    } else if (isDeleteEdge(op) && op.sourceName === asset.name) {
      const targetName = op.target?.title ? `"${op.target?.title}"` : 'unknown';
      if (op.group === 'teaches' || op.group === 'assesses') {
        changes.push(`unaligned this ${typeName} as ${targetName}`);
      } else if (op.group === 'gradebookCategory') {
        changes.push(`uncategorized this ${typeName} as ${targetName}`);
      } else if (op.group === 'elements') {
        changes.push(`removed ${targetName} from this ${typeName}`);
      } else if (op.group === 'questions') {
        const questionType = storyTypeName(polyglot, op.target?.typeId).toLowerCase();
        changes.push(`removed ${questionType} from this ${typeName}`);
      } else if (op.group === 'gates') {
        changes.push(`ungated ${targetName} by this ${typeName}`);
      } else if (op.group === 'testsOut') {
        changes.push(`untested ${targetName} out by this ${typeName}`);
      } else if (op.group === 'hyperlinks') {
        // ignore
      }
    } else if (isSetEdgeOrder(op) && op.sourceName === asset.name) {
      const addn = countWhere(commit.ops, op2 => isAddEdge(op2) && op2.group === op.group);
      const dels = commit.ops.filter(isDeleteEdge); // old deletes could be from anywhere in the graph for anything
      const unknownDeletes = countWhere(dels, del => !del.sourceName);
      if (addn && !dels.length) {
        // nothing because likely just adding the thing
      } else if (unknownDeletes && !addn) {
        // For historic deletes where we don't get the targets, add some information
        // inferred from the set edge order.
        if (op.group === 'teaches' || op.group === 'assesses') {
          changes.push(`unaligned this ${typeName}`);
        } else if (op.group === 'gradebookCategory') {
          changes.push(`uncategorized this ${typeName}`);
        } else if (op.group === 'elements') {
          const knowns = dels.filter(op => edgeTargets[op.name]);
          for (const known of knowns) {
            const target = edgeTargets[known.name];
            const title = target.title;
            const contentName = title ? `"${title}"` : 'content';
            changes.push(`removed ${contentName} from this ${typeName}`);
          }
          if (!knowns.length) changes.push(`removed content from this ${typeName}`);
        } else if (op.group === 'questions') {
          const knowns = dels.filter(op => edgeTargets[op.name]);
          for (const known of knowns) {
            const target = edgeTargets[known.name];
            const questionType = storyTypeName(polyglot, target.typeId).toLowerCase();
            changes.push(`removed ${questionType} from this ${typeName}`);
          }
          if (!knowns.length) changes.push(`removed content from this ${typeName}`);
        } else if (op.group === 'gates') {
          changes.push(`ungated content by this ${typeName}`);
        } else if (op.group === 'hyperlinks') {
          // ignore
        }
      } else if (!dels.length && !addn && op.group === 'elements') {
        changes.push(`reordered content in this ${typeName}`);
      } else if (!dels.length && !addn && op.group === 'questions') {
        changes.push(`reordered questions in this ${typeName}`);
      }
    }
    // TODO: anything else?
  }
  return !changes.length ? 'Unknown changes.' : cap(uniq(changes).join(', ')) + '.';
};
