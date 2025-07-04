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
import { useDispatch } from 'react-redux';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { trackOpenImportModalButton } from '../analytics/AnalyticsEvents';
import {
  RootAsset,
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  confirmSaveProjectGraphEdits,
  useGraphEditSelector,
} from '../graphEdit';
import { usePolyglot } from '../hooks';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import { Bullseye } from '../projectNav/NavIcons';
import { SubmenuItem } from '../story/ActionMenu/SubmenuItem';
import { useProjectAccess } from '../story/hooks';
import { useIsStoryEditMode, useRevisionCommit } from '../story/storyHooks';
import { useProjectGraphSelector } from '../structurePanel/projectGraphHooks';
import { NodeName } from '../types/asset';
import { NewEdge } from '../types/edge';
import * as T from '../types/typeIds';
import { useLevel1LinkedBranches, useRelatedCompetencies } from './competencyEditorHooks';

export const CompetencySetActionsMenu: React.FC<{
  competencyTree: RootAsset | undefined;
  getCompetencySet: () => NodeName;
  resetCompetencyTree: () => void;
  resetAlignments: () => void;
}> = ({ competencyTree, getCompetencySet, resetCompetencyTree, resetAlignments }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  // Get the L1s from multiversally-linked projects and which I'm already using
  const relatedCompetencies = useRelatedCompetencies();
  const linkedBranches = useLevel1LinkedBranches(competencyTree);
  const branchProjects = useProjectGraphSelector(state => state.branchProjects);
  const dirty = useGraphEditSelector(state => state.dirty);
  const projectAccess = useProjectAccess();
  const editMode = useIsStoryEditMode() && projectAccess.EditObjectives;
  const commit = useRevisionCommit();

  const linkCompetencySet = (competencies: NodeName[]) => {
    const csName = getCompetencySet();
    dispatch(beginProjectGraphEdit('Link learning objectives'));
    competencies.forEach(name => {
      const edge: NewEdge = {
        name: crypto.randomUUID(),
        sourceName: csName,
        targetName: name,
        group: 'level1Competencies',
        traverse: true,
        data: {},
        newPosition: 'end',
      };
      dispatch(addProjectGraphEdge(edge));
    });
    dispatch(autoSaveProjectGraphEdits());
    resetCompetencyTree();
    resetAlignments();
  };

  return commit ? (
    <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
      <Bullseye size="1.75rem" />
    </div>
  ) : (
    <UncontrolledDropdown>
      <DropdownToggle
        id="competency-set-import-toggle"
        color="primary"
        outline
        caret
        className={classNames(
          'border-0 asset-settings',
          dirty ? 'dirty' : 'unhover-muted hover-white'
        )}
      >
        <Bullseye size="1.75rem" />
      </DropdownToggle>
      <DropdownMenu className="with-submenu">
        <SubmenuItem
          label="Multiverse"
          className="multiverse-submenu"
          disabled={!relatedCompetencies.length || !projectAccess.ViewMultiverse}
        >
          {relatedCompetencies.map(({ remote, level1Competencies }) => (
            <DropdownItem
              key={remote}
              onClick={() => linkCompetencySet(level1Competencies)}
              className="text-truncate"
              style={{ maxWidth: '33vw' }}
              disabled={!level1Competencies.length || linkedBranches.has(remote)}
            >
              {`Link LOs from ${branchProjects[remote]?.branchName ?? 'Unknown'}.`}
            </DropdownItem>
          ))}
        </SubmenuItem>
        <DropdownItem divider />
        <DropdownItem
          id="import-cset-button"
          onClick={() => {
            dispatch(
              confirmSaveProjectGraphEdits(() => {
                dispatch(
                  openModal(ModalIds.CompetencySetImport, {
                    resetCompetencyTree,
                  })
                );
                trackOpenImportModalButton(T.CompetencySet);
              })
            );
          }}
          disabled={!editMode}
        >
          {polyglot.t('IMPORT_COMPETENCY_SET')}
        </DropdownItem>
        <DropdownItem
          id="import-alignment-button"
          onClick={() => {
            dispatch(
              confirmSaveProjectGraphEdits(() => {
                dispatch(openModal(ModalIds.ContentAlignmentImport, { resetAlignments }));
                trackOpenImportModalButton(T.CompetencySet);
              })
            );
          }}
          disabled={!editMode || !competencyTree}
        >
          {polyglot.t('IMPORT_CONTENT_ALIGNMENT')}
        </DropdownItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
