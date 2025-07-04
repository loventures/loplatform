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

import React from 'react';

import { useBranchId } from '../../../hooks';
import {
  CommitAssetInfo,
  CommitWriteOp,
  isAddDependency,
  isUpdateDependency,
  updateDependencySummary,
} from '../../../revision/revision';
import { EdgeGroup } from '../../../types/edge';
import { Polyglot } from '../../../types/polyglot';
import { storyTypeName } from '../../story';
import { OpLink } from './OpLink';

export const structuralEdges = new Set<EdgeGroup>([
  'elements',
  'questions',
  'level1Competencies',
  'level2Competencies',
  'level3Competencies',
  'survey',
  'cblRubric',
  'gradebookCategories',
]);

export const Op: React.FC<{
  op: CommitWriteOp;
  polyglot: Polyglot;
  commit: number;
  nextCommit?: number;
}> = ({ op, polyglot, commit, nextCommit }) => {
  const branchId = useBranchId();
  const lcTypeName = (asset?: CommitAssetInfo) =>
    storyTypeName(polyglot, asset?.typeId).toLowerCase();
  if (isAddDependency(op)) {
    return <div>Linked project {op.projectCode ?? op.projectTitle}.</div>;
  } else if (isUpdateDependency(op)) {
    return (
      <div>
        Synchronized project {op.projectCode ?? op.projectTitle}; {updateDependencySummary(op)} (
        <a href={`/api/v2/authoring/sync-reports/${op.reportId}`}>report</a>).
      </div>
    );
  } else if (op.op === 'addNode') {
    return (
      <div>
        Created {lcTypeName(op.asset)}{' '}
        <OpLink
          asset={op.asset}
          branch={branchId}
          commit={commit}
        />
        .
      </div>
    );
  } else if (op.op === 'setNodeData') {
    return (
      <div>
        Edited {lcTypeName(op.asset)}{' '}
        <OpLink
          asset={op.asset}
          branch={branchId}
          commit={commit}
          diff={op.asset?.typeId === 'html.1' ? nextCommit : undefined}
        />
        .
      </div>
    );
  } else if (op.op === 'addEdge') {
    if (structuralEdges.has(op.group)) {
      return (
        <div>
          Added {lcTypeName(op.target)}{' '}
          <OpLink
            asset={op.target}
            branch={branchId}
            commit={commit}
          />
          {' to '}
          <OpLink
            asset={op.source}
            branch={branchId}
            commit={commit}
          />
          .
        </div>
      );
    } else if (
      op.group === 'teaches' ||
      op.group === 'assesses' ||
      op.group === 'gradebookCategory'
    ) {
      const verb = op.group === 'gradebookCategory' ? 'Categorized' : 'Aligned';
      return (
        <div>
          {verb + ' ' + lcTypeName(op.source) + ' '}
          <OpLink
            asset={op.source}
            branch={branchId}
            commit={commit}
          />
          {' as '}
          {op.target?.title ?? 'Unknown'}.
        </div>
      );
    } else if (
      op.group === 'gates' ||
      op.group === 'testsOut' ||
      op.group === 'courses' ||
      op.group === 'hyperlinks'
    ) {
      const verb =
        op.group === 'gates'
          ? 'Gated'
          : op.group === 'testsOut'
            ? 'Tested out'
            : op.group === 'courses'
              ? 'Linked'
              : 'Hyperlinked';
      const adverb = op.group === 'gates' || op.verb === 'testsOut' ? 'by' : 'from';
      return (
        <div>
          {verb + ' ' + lcTypeName(op.target) + ' '}
          <OpLink
            asset={op.target}
            branch={branchId}
            commit={commit}
          />
          {` ${adverb} `}
          <OpLink
            asset={op.source}
            branch={branchId}
            commit={commit}
          />
          .
        </div>
      );
    } else {
      console.log(op);
      return <div>Unknown change.</div>;
    }
  } else if (op.op === 'deleteEdge') {
    if (structuralEdges.has(op.group)) {
      // It's not in this commit anymore so I have to let people view it in the next commit
      return (
        <div>
          Removed {lcTypeName(op.target)}{' '}
          <OpLink
            asset={op.target}
            branch={branchId}
            commit={nextCommit}
          />
          {' from '}
          <OpLink
            asset={op.source}
            branch={branchId}
            commit={commit}
          />
          .
        </div>
      );
    } else if (
      op.group === 'teaches' ||
      op.group === 'assesses' ||
      op.group === 'gradebookCategory'
    ) {
      const verb = op.group === 'gradebookCategory' ? 'Uncategorized' : 'Unaligned';
      return (
        <div>
          {verb + ' ' + lcTypeName(op.source) + ' '}
          <OpLink
            asset={op.source}
            branch={branchId}
            commit={commit}
          />
          {' as '}
          {op.target?.title ?? 'Unknown'}.
        </div>
      );
    } else if (op.group === 'gates' || op.group === 'testsOut') {
      const verb = op.group === 'gates' ? 'Ungated' : 'Untested out';
      return (
        <div>
          {verb + ' ' + lcTypeName(op.target) + ' '}
          <OpLink
            asset={op.target}
            branch={branchId}
            commit={commit}
          />
          {' by '}
          <OpLink
            asset={op.source}
            branch={branchId}
            commit={commit}
          />
          .
        </div>
      );
    } else {
      console.log(op);
      return <div>Unknown change.</div>;
    }
  } else if (op.op === 'setEdgeOrder') {
    if (op.group === 'elements' || op.group === 'questions') {
      return (
        <div>
          Reordered {lcTypeName(op.source)}{' '}
          <OpLink
            asset={op.source}
            branch={branchId}
            commit={commit}
          />
          .
        </div>
      );
    } else {
      console.log(op);
      return <div>Unknown change.</div>;
    }
  } else {
    console.log(op);
    return <div>Unknown change.</div>;
  }
};
