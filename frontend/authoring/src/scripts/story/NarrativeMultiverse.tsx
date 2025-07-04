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
import gretchen from '../grfetchen/';
import React, { useEffect, useMemo, useState } from 'react';
import { AiOutlinePlus } from 'react-icons/ai';
import { BsTornado } from 'react-icons/bs';
import { FiExternalLink } from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import {
  Button,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Nav,
  NavItem,
  NavLink,
  Spinner,
  UncontrolledDropdown,
  UncontrolledTooltip,
} from 'reactstrap';

import {
  QuestionsAndElements,
  TreeAsset,
  getFilteredContentList,
  useContentTree,
  useEditedAsset,
} from '../graphEdit';
import { Project } from '../layout/dcmLayoutReducer';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { User } from '../types/user';
import { useProjectAccess } from './hooks';
import NarrativePresence from './NarrativeAsset/NarrativePresence';
import {
  linkBranchAction,
  synchronizeAllAction,
  synchronizeBranchAction,
  unlinkBranchAction,
} from './NarrativeMultiverse/actions';
import { MultiverseLink } from './NarrativeMultiverse/MultiverseLink';
import { useIsStoryEditMode } from './storyHooks';

export type ProjectResponse = {
  project: Project;
  branchId: number;
  branchName: string;
  headId: number;
  branchCreated?: string;
  branchActive?: boolean;
  headCreated?: string;
  headCreatedBy?: number;
  headCreatedByUser?: User; // patched in after fetch, not actually a User, something else
  // and more
};

export type ProjectsResponse = {
  projects: ProjectResponse[];
  users: Record<number, User>; // not actually a User, something else
};

export const compareProjects = (a: ProjectResponse, b: ProjectResponse): number =>
  a.project.name.localeCompare(b.project.name);

export const addDependencies = (projectId: number, ids: number[]): Promise<void> =>
  gretchen.post(`/api/v2/authoring/projects/${projectId}/dependencies/add`).data({ ids }).exec();

export const loadLinkedProjects = (branchId: number): Promise<ProjectResponse[]> =>
  gretchen
    .get(`/api/v2/authoring/branches/${branchId}/linked`)
    .exec()
    .then((res: ProjectsResponse) => res.projects);

// It is possible that a more useful view would be a Table of Contents View so that if you have a lot of
// deep links - for example, a bunch of divorced chapters - the ordering makes sense.

export const NarrativeMultiverse: React.FC = () => {
  const projectGraph = useProjectGraph();
  const dispatch = useDispatch();
  const [branches, setBranches] = useState<ProjectResponse[]>([]);
  const [consumers, setConsumers] = useState<ProjectResponse[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [loading, setLoading] = useState(0);
  const { branchId, branchCommits, assetBranches, branchProjects } = projectGraph;
  const projectAccess = useProjectAccess();
  const editMode = useIsStoryEditMode() && projectAccess.EditMultiverse;

  const course = useEditedAsset(projectGraph.homeNodeName);
  const tree = useContentTree(course, [], QuestionsAndElements);

  const multiverse = useMemo(() => {
    const branchAssets: Record<number, TreeAsset[]> = {};
    for (const branchId of Object.keys(branchCommits)) {
      branchAssets[branchId] = getFilteredContentList(tree, asset =>
        parseInt(branchId) === assetBranches[asset.name] ? '.' : false
      );
    }
    return branchAssets;
  }, [projectGraph, tree]);

  const setLoading1 = (loading: boolean) => setLoading(l => l + (loading ? 1 : -1));

  useEffect(() => {
    setLoading1(true);
    loadLinkedProjects(branchId)
      .then(projects => setConsumers(projects.sort(compareProjects)))
      .finally(() => setLoading1(false));
  }, [branchId]);

  useEffect(() => {
    if (projectGraph.rootNodeName) {
      setBranches(
        Object.values(branchProjects).sort((a, b) => a.project.name.localeCompare(b.project.name))
      );
      setLoaded(true);
    }
  }, [projectGraph, branchProjects]);

  const canSyncAll = useMemo(
    () => branches.some(branch => branch.headId !== branchCommits[branch.branchId]),
    [branches, branchCommits]
  );

  const doSynchronize = (branch: ProjectResponse) => {
    dispatch(synchronizeBranchAction(branch, setLoading1));
  };

  const doSynchronizeAll = () => {
    dispatch(synchronizeAllAction(branches, setLoading1));
  };

  const doUnlink = (branch: ProjectResponse) => {
    dispatch(unlinkBranchAction(branch));
  };

  const addLink = () => {
    dispatch(linkBranchAction());
  };

  const [xActiveTab, setActiveTab] = useState('');
  const activeTab = xActiveTab || (branches.length || !consumers.length ? 'uses' : 'usedBy');

  return (
    <div className="multiverse-app">
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <UncontrolledDropdown>
          <DropdownToggle
            color="primary"
            outline
            caret
            className="border-0 asset-preview unhover-muted hover-white"
          >
            <BsTornado size="1.5rem" />
          </DropdownToggle>
          <DropdownMenu>
            <DropdownItem
              color="warning"
              disabled={!canSyncAll || !editMode}
              onClick={() => doSynchronizeAll()}
            >
              Synchronize All
            </DropdownItem>
          </DropdownMenu>
        </UncontrolledDropdown>
        <h2 className="my-4 text-center">Multiverse</h2>
        <NarrativePresence name="multiverse">
          <div className="button-spacer d-flex align-items-center justify-content-center actions-icon"></div>
        </NarrativePresence>
      </div>
      <div className="mx-5 mt-3 mb-5">
        <div className="content-list multiverse-index">
          {loading || !loaded ? (
            <div className="text-center">
              <Spinner
                color="muted"
                size="sm"
              />
            </div>
          ) : (
            <>
              {consumers.length ? (
                <Nav
                  tabs
                  className="mb-4"
                >
                  <NavItem>
                    <NavLink
                      active={activeTab === 'uses'}
                      onClick={() => setActiveTab('uses')}
                    >
                      Uses ({branches.length})
                    </NavLink>
                  </NavItem>
                  <NavItem>
                    <NavLink
                      active={activeTab === 'usedBy'}
                      onClick={() => setActiveTab('usedBy')}
                    >
                      Used by ({consumers.length})
                    </NavLink>
                  </NavItem>
                </Nav>
              ) : null}

              {activeTab === 'usedBy' ? (
                consumers.map(branch => (
                  <React.Fragment key={branch.branchId}>
                    <a
                      href={`#/branch/${branch.branchId}/story/multiverse?contextPath=${branch.project.homeNodeName}`}
                      target="_blank"
                      rel="noreferrer"
                      className="story-index-item story-nav-course depth-1 d-flex align-items-center text-decoration-none"
                    >
                      <span className="hover-underline flex-shrink-1 text-truncate">
                        {branch.project.name}
                      </span>
                      <FiExternalLink
                        size="1rem"
                        className="ms-2 flex-shrink-0"
                      />
                    </a>
                  </React.Fragment>
                ))
              ) : (
                <>
                  {!branches.length ? (
                    <div
                      className={classNames(
                        'text-muted text-center',
                        consumers.length && 'pt-2 mt-5'
                      )}
                    >
                      No multiverse projects.
                    </div>
                  ) : (
                    branches.map((branch, index) => (
                      <React.Fragment key={branch.branchId}>
                        <a
                          href={`#/branch/${branch.branchId}/story/${branch.project.homeNodeName}`}
                          target="_blank"
                          rel="noreferrer"
                          className="story-index-item multi story-nav-course depth-1 d-flex align-items-center text-decoration-none"
                        >
                          <span className="hover-underline flex-shrink-1 text-truncate">
                            {branch.project.name}
                          </span>
                          <FiExternalLink
                            size="1rem"
                            className="ms-2 flex-shrink-0"
                          />
                          <div className="flex-grow-1" />
                          {editMode && projectAccess.EditMultiverse && (
                            <div
                              className="flex-shrink-0 flex-grow-0 gap-2 ms-2 d-flex align-items-center"
                              onClick={e => {
                                e.preventDefault();
                                e.stopPropagation();
                              }}
                            >
                              <Button
                                size="sm"
                                color="danger"
                                outline
                                onClick={() => doUnlink(branch)}
                                disabled={true}
                              >
                                Delete
                              </Button>
                              {branch.headId === branchCommits[branch.branchId] ? (
                                <Button
                                  size="sm"
                                  outline
                                  disabled
                                >
                                  Synchronized
                                </Button>
                              ) : (
                                <Button
                                  size="sm"
                                  color="warning"
                                  outline
                                  onClick={() => doSynchronize(branch)}
                                >
                                  Synchronize
                                </Button>
                              )}
                            </div>
                          )}
                        </a>
                        {multiverse[branch.branchId]?.map(asset => (
                          <MultiverseLink
                            key={asset.name}
                            asset={asset}
                          />
                        ))}
                        {index < branches.length - 1 && (
                          <div className="flex-grow-1 multiverse-spacer" />
                        )}
                      </React.Fragment>
                    ))
                  )}
                  {editMode && (
                    <div className="add-content mt-5 edit-mode">
                      <div className="rule" />
                      <Button
                        id="multiverse-add"
                        color="primary"
                        className="plus"
                        onClick={() => addLink()}
                      >
                        <AiOutlinePlus
                          size="1.2rem"
                          stroke="0px"
                        />
                      </Button>
                      <UncontrolledTooltip
                        delay={0}
                        target="multiverse-add"
                        placement="bottom"
                      >
                        Link new multiversal project.
                      </UncontrolledTooltip>
                      <div className="rule" />
                    </div>
                  )}
                </>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
