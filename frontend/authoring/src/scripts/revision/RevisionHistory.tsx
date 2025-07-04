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
import * as React from 'react';
import { MutableRefObject, useEffect, useMemo, useRef } from 'react';
import { IoGitCompareOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';

import { trackNarrativeNavHandler } from '../analytics/AnalyticsEvents';
import { formatFullDate } from '../dateUtil';
import FeedbackProfile from '../feedback/FeedbackProfile';
import { useCurrentContextPath } from '../graphEdit';
import { useBranchId, usePolyglot } from '../hooks';
import { editorUrl } from '../story/story';
import { setNarrativeState } from '../story/storyActions';
import {
  NoRevisionHistory,
  useDiffCommit,
  useRevisionCommit,
  useRevisionHistory,
} from '../story/storyHooks';
import { EdgeName, NewAsset } from '../types/asset';
import {
  CommitAssetInfo,
  CommitSegment,
  isAddEdge,
  loadNodeCommitHistory,
  summarizeCommit,
} from './revision';

export const RevisionHistory: React.FC<{
  asset: NewAsset<any>;
  scrollRef: MutableRefObject<HTMLDivElement>;
  detail: boolean;
}> = ({ asset, scrollRef, detail }) => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const history = useRevisionHistory(asset.name);

  useEffect(() => {
    dispatch(setNarrativeState({ revisionHistory: undefined }));
    loadNodeCommitHistory(branchId, asset.name, detail).then(({ objects }) =>
      dispatch(setNarrativeState({ revisionHistory: { name: asset.name, history: objects } }))
    );
  }, [branchId, asset.name, detail]);

  // This records the targets of all add edges in all commits. This allows us to infer the target of
  // a delete edge so we can display names in the UI.
  // This will only record edges that are added manually, probably not any that are bulk imported?
  const edgeTargets = useMemo(() => {
    const result: Record<EdgeName, CommitAssetInfo> = {};
    for (const commit of history) {
      for (const op of commit.ops) {
        if (isAddEdge(op)) result[op.name] = op.target;
      }
    }
    return result;
  }, [history]);

  return (
    <div className="feedback-section">
      {history === NoRevisionHistory ? (
        <div className="p-4 text-muted text-center">Loading...</div>
      ) : !history.length ? (
        <div className="p-4 text-muted text-center">No history</div>
      ) : null}
      {history.map((commit, index) => (
        <CommitRevision
          key={commit.first}
          asset={asset}
          commit={commit}
          nextCommit={history[index + 1]}
          first={!index}
          revision={history.length - index}
          edgeTargets={edgeTargets}
          scrollRef={scrollRef}
        />
      ))}
    </div>
  );
};

const ScrollPadding = 96; // enough to get past the sticky header

const CommitRevision: React.FC<{
  asset: NewAsset<any>;
  commit: CommitSegment;
  nextCommit?: CommitSegment;
  first: boolean;
  revision: number;
  edgeTargets: Record<EdgeName, CommitAssetInfo>;
  scrollRef: MutableRefObject<HTMLDivElement>;
}> = ({ asset, commit, nextCommit, first, edgeTargets, revision, scrollRef }) => {
  const branchId = useBranchId();
  const polyglot = usePolyglot();
  const contextPath = useCurrentContextPath();
  const currentCommit = useRevisionCommit();
  const summary = useMemo(
    () => summarizeCommit(polyglot, asset, commit, edgeTargets),
    [commit, polyglot, edgeTargets]
  );
  const commitActive = (first && !currentCommit) || currentCommit === commit.first;
  const diff = useDiffCommit();
  const active = commitActive && !diff;
  const ref = useRef<HTMLDivElement>();
  useEffect(() => {
    const el = ref.current;
    const scroller = scrollRef.current;
    if (commitActive && el && scroller) {
      const { bottom } = el.getBoundingClientRect();
      if (bottom > scroller.clientHeight) {
        scroller.scrollTo({
          top: scroller.scrollTop + bottom - scroller.clientHeight,
          behavior: 'smooth',
        });
      } else if (el.offsetTop - ScrollPadding < scroller.scrollTop) {
        scroller.scrollTo({
          top: Math.max(0, el.offsetTop - ScrollPadding),
          behavior: 'smooth',
        });
      }
    }
  }, [ref, scrollRef, commitActive]);

  return (
    <div ref={ref}>
      <div className={classNames('revision-button d-flex justify-content-center', { first })}>
        <Link
          className={classNames('btn btn-sm', active ? 'btn-primary' : 'btn-outline-primary', {
            active,
          })}
          style={{ borderRadius: '1rem', width: '50%' }}
          to={editorUrl('revision', branchId, asset, contextPath, {
            commit: commit.first,
          })}
          onClick={trackNarrativeNavHandler('Revision')}
        >
          {first ? 'Latest Revision' : `Revision #${revision}`}
        </Link>
      </div>
      {!commit.hidden && (
        <div className="d-flex flex-column pb-3 feedback-item commit-item">
          <div className="d-flex align-items-center px-3 mt-3 history-item-header">
            <FeedbackProfile profile={commit.createdBy} />
            <div className="d-flex flex-column flex-grow-1 ms-3 header-cluster">
              <div className="text-truncate fw-bold">{commit.createdBy.fullName}</div>
              <div className="text-muted text-truncate">
                {formatFullDate(commit.created) + (commit.first !== commit.last ? ' *' : '')}
              </div>
            </div>
            {asset.typeId === 'html.1' && nextCommit && (
              <Link
                className={classNames(
                  'btn d-flex align-items-center p-1 align-self-start',
                  commitActive && diff ? 'btn-primary active' : 'btn-transparent text-primary'
                )}
                to={editorUrl('revision', branchId, asset, contextPath, {
                  commit: commit.first,
                  diff: nextCommit.first,
                })}
                title="Diff"
                onClick={trackNarrativeNavHandler('Diff')}
              >
                <IoGitCompareOutline size="1rem" />
              </Link>
            )}
          </div>
          <div className="px-3 mt-3">{summary}</div>
        </div>
      )}
    </div>
  );
};
