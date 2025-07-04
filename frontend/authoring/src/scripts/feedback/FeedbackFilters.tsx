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

import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import {
  getAllEditedOutEdges,
  getEditedAsset,
  useEditedAssetTitle,
  useGraphEdits,
} from '../graphEdit';
import { useBranchId, useDcmSelector } from '../hooks';
import AssetDropdownItem from '../story/components/AssetDropdownItem';
import { compareProjects, ProjectResponse } from '../story/NarrativeMultiverse';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { NewAsset, NodeName } from '../types/asset';
import { setFeedbackFilters } from './feedbackActions';
import {
  FeedbackProfileDto,
  FeedbackStatusFilters,
  loadFeedbackAssignees,
  loadUpstreamFeedbackProjects,
} from './FeedbackApi';
import { useFeedbackFilters } from './feedbackHooks';

const truncate = (s: string, n: number) => (s.length < n ? s : s.substring(0, n - 1) + '…');

export const EverythingRights = new Set([
  'loi.authoring.security.right$ViewAllProjectsRight',
  'loi.authoring.security.right$EditContentAnyProjectRight',
  'loi.authoring.security.right$AllAuthoringActionsRight',
]);

export const FeedbackFilters: React.FC<{ right?: boolean }> = ({ right: might }) => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const { profile, rights } = useDcmSelector(state => state.user);
  const yourself = profile?.id;
  const { branch, status, assignee, unit, module } = useFeedbackFilters();
  const [assignees, setAssignees] = useState(new Array<FeedbackProfileDto>());
  const feedbackBranch = branch ?? branchId;
  const [consumers, setConsumers] = useState<ProjectResponse[]>([]);

  useEffect(() => {
    loadFeedbackAssignees(feedbackBranch, {}).then(res => setAssignees(res.objects.sort()));
  }, [feedbackBranch]);

  useEffect(() => {
    if (rights?.some(right => EverythingRights.has(right)))
      loadUpstreamFeedbackProjects(branchId).then(projects =>
        setConsumers(projects.sort(compareProjects))
      );
  }, [branchId]);

  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const locations = useMemo(() => {
    const result = new Array<NewAsset<any> & { depth: number }>();
    const loop = (name: NodeName, depth: number) => {
      const asset = getEditedAsset(name, projectGraph, graphEdits);
      result.push({ ...asset, depth });
      if (asset.typeId === 'unit.1') {
        for (const x of getAllEditedOutEdges(asset.name, projectGraph, graphEdits)) {
          loop(x.targetName, 1);
        }
      }
    };
    for (const x of getAllEditedOutEdges(projectGraph.homeNodeName, projectGraph, graphEdits)) {
      loop(x.targetName, 0);
    }
    return result;
  }, [projectGraph, graphEdits]);
  const location = module ?? unit;
  const locationTitle = useEditedAssetTitle(location).replace(/:.*/, '');

  const projectName = useMemo(() => {
    if (!branch) return undefined;
    const project = consumers.find(project => project.branchId === branch);
    if (!project) return `Project ${branch}`;
    const ten = project.branchName.substring(0, 10);
    return ten.includes(':') ? ten.replace(/:.*/, '') : ten.replace(/[^a-zA-Z0-9]*$/, '') + '…';
  }, [consumers, branch]);

  return (
    <>
      {!!consumers.length && (
        <UncontrolledDropdown id="branch-selector">
          <DropdownToggle
            color="light"
            size="sm"
            caret
          >
            {projectName ?? <span className="text-muted">Project</span>}
          </DropdownToggle>
          <DropdownMenu
            style={{ maxHeight: '75vh', overflow: 'auto' }}
            right={might}
          >
            <DropdownItem
              onClick={() => dispatch(setFeedbackFilters({ branch: undefined }))}
              disabled={!branch}
            >
              This Project
            </DropdownItem>
            {consumers.map(project => (
              <DropdownItem
                key={project.branchId}
                onClick={() => dispatch(setFeedbackFilters({ branch: project.branchId }))}
                disabled={branch === project.branchId}
              >
                {truncate(project.branchName, 32)}
              </DropdownItem>
            ))}
          </DropdownMenu>
        </UncontrolledDropdown>
      )}
      <UncontrolledDropdown id="module-selector">
        <DropdownToggle
          color="light"
          size="sm"
          caret
        >
          {location ? locationTitle : <span className="text-muted">Location</span>}
        </DropdownToggle>
        <DropdownMenu
          style={{ maxHeight: '75vh', overflow: 'auto' }}
          right={might}
        >
          <DropdownItem
            onClick={() => dispatch(setFeedbackFilters({ unit: undefined, module: undefined }))}
            disabled={!module && !unit}
          >
            Anywhere
          </DropdownItem>
          {locations.map(asset => (
            <AssetDropdownItem
              key={asset.name}
              onClick={() =>
                dispatch(
                  setFeedbackFilters({
                    unit: asset.typeId === 'unit.1' ? asset.name : undefined,
                    module: asset.typeId === 'module.1' ? asset.name : undefined,
                  })
                )
              }
              disabled={location === asset.name}
              name={asset.name}
              className={asset.depth ? 'ps-5' : undefined}
            />
          ))}
        </DropdownMenu>
      </UncontrolledDropdown>
      <UncontrolledDropdown id="status-selector">
        <DropdownToggle
          color="light"
          size="sm"
          caret
        >
          {status == 'Open' ? <span className="text-muted">Status</span> : status}
        </DropdownToggle>
        <DropdownMenu end={might}>
          {FeedbackStatusFilters.map(s => (
            <DropdownItem
              key={s}
              onClick={() => dispatch(setFeedbackFilters({ status: s }))}
              disabled={s === status}
            >
              {s}
            </DropdownItem>
          ))}
        </DropdownMenu>
      </UncontrolledDropdown>
      <UncontrolledDropdown id="assignee-selector">
        <DropdownToggle
          color="light"
          size="sm"
          caret
        >
          {assignee === undefined ? (
            <span className="text-muted">Assignee</span>
          ) : assignee === null ? (
            'Unassigned'
          ) : assignee === yourself ? (
            'Assigned to you'
          ) : (
            assignees.find(a => a.id === assignee)?.fullName
          )}
        </DropdownToggle>
        <DropdownMenu end={might}>
          <DropdownItem
            onClick={() => dispatch(setFeedbackFilters({ assignee: undefined }))}
            disabled={assignee === undefined}
          >
            Anyone
          </DropdownItem>
          <DropdownItem
            onClick={() => dispatch(setFeedbackFilters({ assignee: null }))}
            disabled={assignee === null}
          >
            Unassigned
          </DropdownItem>
          <DropdownItem
            onClick={() => dispatch(setFeedbackFilters({ assignee: yourself }))}
            disabled={assignee === yourself}
          >
            Assigned to you
          </DropdownItem>
          {assignees
            .filter(a => a.id !== yourself)
            .map(a => (
              <DropdownItem
                key={a.id}
                onClick={() => dispatch(setFeedbackFilters({ assignee: a.id }))}
                disabled={a.id === assignee}
              >
                {a.fullName}
              </DropdownItem>
            ))}
        </DropdownMenu>
      </UncontrolledDropdown>
    </>
  );
};

export default FeedbackFilters;
