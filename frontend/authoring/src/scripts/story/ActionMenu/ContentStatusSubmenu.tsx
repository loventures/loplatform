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
import { useDcmSelector } from '../../hooks';
import { NodeName, TypeId } from '../../types/asset';
import { useProjectAccess } from '../hooks';
import { setContentStatusAction } from './actions';
import { SubmenuItem } from './SubmenuItem';

export const ContentStatusSubmenu: React.FC<{
  name: NodeName;
  contextPath: string;
  typeId: TypeId;
}> = ({ name, contextPath }) => {
  const dispatch = useDispatch();
  const contentStatus = useEditedAssetDatum(name, data => data.contentStatus);
  const contentStatuses = useDcmSelector(state => state.configuration.contentStatuses ?? {});
  const contentStatusEntries = Object.entries(contentStatuses);
  const projectAccess = useProjectAccess();
  const hasAccess = projectAccess.ContentStatus && contentStatusEntries.length > 0;

  const setContentStatus = useCallback(
    (event: React.MouseEvent) => {
      const contentStatus = event.currentTarget.getAttribute('data-content-status') || null;
      dispatch(setContentStatusAction(name, contextPath, contentStatus));
    },
    [name, contextPath]
  );

  return hasAccess ? (
    <SubmenuItem
      className="content-status-submenu"
      label="Content Status"
    >
      <DropdownItem
        value=""
        data-content-status=""
        onClick={setContentStatus}
      >
        <div className="check-spacer">{!contentStatus && <IoCheckmarkOutline />}</div>
        <span className="gray-600">Unset</span>
      </DropdownItem>
      {contentStatusEntries.map(([key, label]) => (
        <DropdownItem
          key={key}
          value={key}
          data-content-status={key}
          onClick={setContentStatus}
        >
          <div className="check-spacer">{key === contentStatus && <IoCheckmarkOutline />}</div>
          {label}
        </DropdownItem>
      ))}
    </SubmenuItem>
  ) : null;
};
