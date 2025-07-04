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
import { GiDinosaurBones } from 'react-icons/gi';
import { IoMenuOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { confirmSaveProjectGraphEditsLink, useGraphEditSelector } from '../graphEdit';
import { useBranchId, usePolyglot } from '../hooks';
import { DropdownAItem } from '../story/components/DropdownAItem';
import { useProjectAccess } from '../story/hooks';
import { useRevisionCommit } from '../story/storyHooks';
import { useEditedCompetencySetEdges } from './useFlatCompetencies';

const competencySetExportUrl = (
  branchId: number,
  name: string,
  commit: number | undefined
): string =>
  commit
    ? `/api/v2/assets/${branchId}/commits/${commit}/competencySet.1/${name}/structure.csv`
    : `/api/v2/assets/${branchId}/competencySet.1/${name}/structure.csv`;

const competencySetAlignmentUrl = (
  branchId: number,
  name: string,
  commit: number | undefined
): string =>
  commit
    ? `/api/v2/assets/${branchId}/commits/${commit}/competencySet.1/${name}/alignment.csv`
    : `/api/v2/assets/${branchId}/competencySet.1/${name}/alignment.csv`;

const competencySetAlignmentPlusUrl = (
  branchId: number,
  name: string,
  commit: number | undefined
): string =>
  commit
    ? `/api/v2/assets/${branchId}/commits/${commit}/competencySet.1/${name}/alignmentPlus.csv`
    : `/api/v2/assets/${branchId}/competencySet.1/${name}/alignmentPlus.csv`;

const CompetencySetMenu: React.FC = () => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const branchId = useBranchId();
  const competencySetEdge = useEditedCompetencySetEdges()[0];
  const competencySetName = competencySetEdge?.targetName;
  const dirty = useGraphEditSelector(state => state.dirty);
  const commit = useRevisionCommit();
  const projectAccess = useProjectAccess();

  return (
    <UncontrolledDropdown className="d-inline-block">
      <DropdownToggle
        id="competency-set-toggle"
        color="primary"
        outline
        caret
        className="border-0 asset-settings unhover-muted hover-white"
      >
        {commit ? <GiDinosaurBones size="2rem" /> : <IoMenuOutline size="1.75rem" />}
      </DropdownToggle>
      <DropdownMenu
        right
        id="competency-set-menu"
      >
        <DropdownAItem
          id="export-cset-button"
          target="_blank"
          href={competencySetExportUrl(branchId, competencySetName, commit)}
          disabled={!competencySetName || !projectAccess.EditObjectives}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
        >
          {polyglot.t('EXPORT_COMPETENCY_SET')}
        </DropdownAItem>
        <DropdownAItem
          id="export-alignment-button"
          target="_blank"
          href={competencySetAlignmentUrl(branchId, competencySetName, commit)}
          disabled={!competencySetName || !projectAccess.EditObjectives}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
        >
          {polyglot.t('EXPORT_CONTENT_ALIGNMENT')}
        </DropdownAItem>
        <DropdownAItem
          id="export-alignment-plus-button"
          target="_blank"
          href={competencySetAlignmentPlusUrl(branchId, competencySetName, commit)}
          disabled={!competencySetName || !projectAccess.EditObjectives}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
        >
          {polyglot.t('EXPORT_CONTENT_PLUS_ALIGNMENT')}
        </DropdownAItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};

export default CompetencySetMenu;
