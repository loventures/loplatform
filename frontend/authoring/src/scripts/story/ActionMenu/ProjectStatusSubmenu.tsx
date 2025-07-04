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

import React, { useCallback } from 'react';
import { IoCheckmarkOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { DropdownItem } from 'reactstrap';

import { useEditedAssetDatum } from '../../graphEdit';
import { useDcmSelector, useRootNodeName } from '../../hooks';
import { useProjectAccess } from '../hooks';
import { setProjectStatusAction } from './actions';
import { SubmenuItem } from './SubmenuItem';

export const ProjectStatusSubmenu: React.FC = () => {
  const dispatch = useDispatch();
  const rootName = useRootNodeName();
  const projectStatus = useEditedAssetDatum(rootName, data => data.projectStatus);
  const projectStatuses = useDcmSelector(state => state.configuration.projectStatuses ?? {});
  const projectStatusEntries = Object.entries(projectStatuses);
  const projectAccess = useProjectAccess();
  // TODO: who has access?
  const hasAccess = projectAccess.ContentStatus && projectStatusEntries.length > 0;

  const setProjectStatus = useCallback((event: React.MouseEvent) => {
    const projectStatus = event.currentTarget.getAttribute('data-project-status') || null;
    dispatch(setProjectStatusAction(projectStatus));
  }, []);

  return hasAccess ? (
    <SubmenuItem
      className="project-status-submenu"
      label="Project Status"
    >
      <DropdownItem
        value=""
        data-project-status=""
        onClick={setProjectStatus}
      >
        <div className="check-spacer">{!projectStatus && <IoCheckmarkOutline />}</div>
        <span className="gray-600">Unset</span>
      </DropdownItem>
      {projectStatusEntries.map(([key, label]) => (
        <DropdownItem
          key={key}
          value={key}
          data-project-status={key}
          onClick={setProjectStatus}
        >
          <div className="check-spacer">{key === projectStatus && <IoCheckmarkOutline />}</div>
          {label}
        </DropdownItem>
      ))}
    </SubmenuItem>
  ) : null;
};
