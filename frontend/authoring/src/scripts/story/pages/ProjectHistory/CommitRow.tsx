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

import classNames from 'classnames';
import React from 'react';
import { AiOutlineUndo } from 'react-icons/ai';
import { MdHistory, MdHistoryToggleOff } from 'react-icons/md';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Badge, Button } from 'reactstrap';

import { formatFullDate } from '../../../dateUtil';
import {
  confirmSaveProjectGraphEditsLink,
  useEditedCurrentAsset,
  useGraphEditSelector,
} from '../../../graphEdit';
import { useBranchId, usePolyglot } from '../../../hooks';
import { CommitSegment } from '../../../revision/revision';
import { editorUrl } from '../../story';
import { useRevisionCommit } from '../../storyHooks';
import { Op } from './Op';

export const CommitRow: React.FC<{
  commit: CommitSegment;
  next?: CommitSegment;
  first: boolean;
  onRevert: (commit: CommitSegment) => void;
  published: boolean;
}> = ({ commit, next, first, onRevert, published }) => {
  const revisionCommit = useRevisionCommit();
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const home = useEditedCurrentAsset();
  const dirty = useGraphEditSelector(s => s.dirty);

  const active = first ? !revisionCommit : revisionCommit === commit.first;
  return (
    <>
      <div
        className={classNames('add-content edit-mode', { active })}
        id={commit.first.toString()}
      >
        <div className="rule" />
        <Link
          className="btn btn-primary plus"
          to={editorUrl('story', branchId, home, [], { commit: first ? undefined : commit.first })}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
          style={active ? undefined : { padding: '.1rem .12rem .1rem .08rem' }}
        >
          {active ? <MdHistoryToggleOff size="1rem" /> : <MdHistory size="1rem" />}
        </Link>
        <div className="rule" />
      </div>
      <div className="card py-2 project-history-entry">
        <div className="ps-3 pe-2 d-flex align-items-center">
          <span className="fw-bold me-1">{commit.createdBy.fullName}</span>–
          <span className={classNames(published ? 'text-dark' : 'text-muted', 'ms-1')}>
            {formatFullDate(commit.created)}
            {commit.first !== commit.last ? ' *' : ''}
          </span>
          {published && <Badge className="ms-2">Published</Badge>}
          <div className="flex-grow-1" />
          <div className="flex-shrink-0 flex-grow-0 controls ms-2 d-flex">
            {!first && !published && (
              <Button
                size="sm"
                color="danger"
                outline
                className="p-2 d-flex align-items-center border-0"
                onClick={() => onRevert(commit)}
                title="Revert Project"
              >
                <AiOutlineUndo />
              </Button>
            )}
          </div>
        </div>
        <ul className="mb-1">
          {commit.ops.map((op, index) => (
            <li key={index}>
              <Op
                op={op}
                polyglot={polyglot}
                commit={commit.first}
                nextCommit={next?.first}
              />
            </li>
          ))}
          {commit.truncated && (
            <li>
              <em className="text-muted">And more...</em>
            </li>
          )}
        </ul>
      </div>
    </>
  );
};
