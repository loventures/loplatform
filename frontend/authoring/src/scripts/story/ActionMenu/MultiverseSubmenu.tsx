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
import { MdOutlineOpenInNew } from 'react-icons/md';
import { useDispatch } from 'react-redux';
import { DropdownItem } from 'reactstrap';

import { autoSaveProjectGraphEdits, beginProjectGraphEdit } from '../../graphEdit';
import { useRemoteAssetBranch } from '../../structurePanel/projectGraphActions';
import { NodeName } from '../../types/asset';
import { useProjectAccess } from '../hooks';
import { NarrativeMode } from '../story';
import { useIsEditable } from '../storyHooks';
import { SubmenuItem } from './SubmenuItem';

export const MultiverseSubmenu: React.FC<{ name: NodeName; mode: NarrativeMode }> = ({
  name,
  mode,
}) => {
  const dispatch = useDispatch();
  const remote = useRemoteAssetBranch(name);
  const projectAccess = useProjectAccess();
  const canRestore = useIsEditable(name, 'EditContent');

  const restoreOriginal = () => {
    dispatch(beginProjectGraphEdit('Restore original'));
    // TODO: SUPPORT LAIRD
    dispatch(autoSaveProjectGraphEdits());
  };

  return (
    <SubmenuItem
      className="multiverse-submenu"
      disabled={!remote || !projectAccess.ViewMultiverse}
      label="Multiverse"
    >
      <DropdownItem
        tag="a"
        target="_blank"
        className="d-flex align-items-center"
        href={`/Authoring/branch/${remote}/launch/${name}`}
      >
        View Source <MdOutlineOpenInNew className="ms-1" />
      </DropdownItem>
      {mode !== 'feedback' && (
        <>
          <DropdownItem
            disabled={!canRestore}
            onClick={() => restoreOriginal()}
          >
            Restore Original
          </DropdownItem>
        </>
      )}
    </SubmenuItem>
  );
};
