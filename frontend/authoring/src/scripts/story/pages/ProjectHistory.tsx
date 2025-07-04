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

import React, { useCallback, useEffect, useState } from 'react';
import { VscHistory } from 'react-icons/vsc';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import VisibilitySensor from 'react-visibility-sensor';
import { Spinner } from 'reactstrap';

import { trackAuthoringEvent } from '../../analytics';
import { formatFullDate } from '../../dateUtil';
import { reloadAssetEditor } from '../../editor/assetEditorActions';
import { useBranchId, useHomeNodeName } from '../../hooks';
import { ConfirmationTypes } from '../../modals/ConfirmModal';
import { openModal } from '../../modals/modalActions';
import { ModalIds } from '../../modals/modalIds';
import {
  CommitSegment,
  ProjectOffering,
  isAddEdge,
  isAddNode,
  loadBranchOffering,
  loadCommitLog,
  revertBranch,
} from '../../revision/revision';
import { openToast } from '../../toast/actions';
import { NodeName } from '../../types/asset';
import NarrativePresence from '../NarrativeAsset/NarrativePresence';
import { CommitRow } from './ProjectHistory/CommitRow';
import { HistoryMenu } from './ProjectHistory/HistoryMenu';
import { PublishButton } from './ProjectHistory/PublishButton';

const Limit = 50;

export const ProjectHistory: React.FC = () => {
  const branchId = useBranchId();
  const dispatch = useDispatch();
  const history = useHistory();
  const homeNodeName = useHomeNodeName();

  const [more, setMore] = useState(false);
  const [segments, setSegments] = useState(new Array<CommitSegment>());
  const [refresh, setRefresh] = useState(0);

  const [offering, setOffering] = useState<ProjectOffering | null>(null);
  useEffect(() => {
    loadBranchOffering(branchId).then(setOffering);
  }, [branchId, refresh]);

  const [loading, setLoading] = useState(false);
  const [from, setFrom] = useState<number>();
  useEffect(() => {
    setLoading(true);
    loadCommitLog(branchId, Limit, from)
      .then(({ objects }) => {
        for (const segment of objects) {
          // if a segment "Added X to Y" then drop the "Created X" op
          const edged = new Set<NodeName>();
          for (const op of segment.ops) {
            if (isAddEdge(op)) edged.add(op.targetName);
          }
          segment.ops = segment.ops.filter(op => !isAddNode(op) || !edged.has(op.name));
        }
        setSegments([...segments, ...objects]);
        setMore(!!objects.length);
        setLoading(false);
      })
      .catch(e => {
        console.warn(e);
        dispatch(openToast('Failed to load project history.', 'danger'));
      });
  }, [branchId, from, refresh]);

  const doReload = useCallback(() => {
    setSegments([]);
    setFrom(undefined);
    setRefresh(r => 1 + r);
    dispatch(reloadAssetEditor());
  }, []);

  const onRevert = useCallback(
    (commit: CommitSegment) => {
      dispatch(
        openModal(ModalIds.Confirm, {
          confirmationType: ConfirmationTypes.RevertProject,
          color: 'danger',
          words: {
            header: 'Permanently Revert Project?',
            body: `This will revert the project to this revision (${
              commit.createdBy.fullName
            }, ${formatFullDate(
              commit.created
            )}). All more recent changes will be discarded. This operation cannot be undone.`,
            confirm: 'Revert',
          },
          confirmCallback: () => {
            trackAuthoringEvent('Narrative Editor - Revert Project');
            return revertBranch(branchId, commit.first, segments[0].first)
              .then(() => {
                dispatch(openToast('The project was successfully reverted.', 'success'));
                history.replace(`/branch/${branchId}/story/history?contextPath=${homeNodeName}`);
                doReload();
              })
              .catch(e => {
                console.warn(e);
                dispatch(openToast('The revert operation failed.', 'danger'));
              });
          },
        })
      );
    },
    [branchId, homeNodeName, segments]
  );

  return (
    <>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
          <VscHistory size="1.75rem" />
        </div>
        <h2 className="my-4 text-center">Project History</h2>
        <NarrativePresence name="history">
          <HistoryMenu published={!!offering} />
        </NarrativePresence>
      </div>

      <div className="text-center text-muted my-3">
        {!offering
          ? 'Not yet published.'
          : offering.commitId === segments[0]?.first
            ? 'Latest version published.'
            : `Published version ${formatFullDate(offering.commitTime)}.`}
      </div>

      <PublishButton
        offering={offering}
        commit={segments[0]?.first}
        doReload={doReload}
      />

      <div className="mx-5 my-5 content-list">
        {loading && !more && (
          <div className="d-flex justify-content-center">
            <Spinner color="muted" />
          </div>
        )}
        {segments.map((segment, index) => (
          <CommitRow
            key={segment.first}
            commit={segment}
            next={segments[index + 1]}
            first={!index}
            onRevert={onRevert}
            published={segment.first === offering?.commitId}
          />
        ))}
        <div>
          {more && (
            <VisibilitySensor
              onChange={visible => {
                if (visible) setFrom(segments[segments.length - 1].last);
              }}
            >
              <div className="d-flex justify-content-center pt-4">
                <Spinner color="muted" />
              </div>
            </VisibilitySensor>
          )}
        </div>
      </div>
    </>
  );
};
