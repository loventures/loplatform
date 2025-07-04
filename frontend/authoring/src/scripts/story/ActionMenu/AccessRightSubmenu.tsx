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

import { accessRightable, accessRights } from '../../components/AccessRightEditor';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useEditedAssetDatum,
} from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { NodeName, TypeId } from '../../types/asset';
import { useContentAccess } from '../hooks/useContentAccess';
import { SubmenuItem } from './SubmenuItem';

export const AccessRightSubmenu: React.FC<{ name: NodeName; typeId: TypeId }> = ({
  name,
  typeId,
}) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const accessRight = useEditedAssetDatum(name, data => data.accessRight);
  const contentAccess = useContentAccess(name);

  const isCurrentRight = (key: string): boolean => (key ? accessRight === key : !accessRight);

  const setAccessRight = useCallback(
    (event: React.MouseEvent) => {
      const accessRight = event.currentTarget.getAttribute('data-access-right') || null;
      if (isCurrentRight(accessRight)) return;
      dispatch(beginProjectGraphEdit('Edit access restrictions'));
      dispatch(
        editProjectGraphNodeData(name, {
          accessRight,
        })
      );
      dispatch(autoSaveProjectGraphEdits());
    },
    [name]
  );
  const righteous = accessRightable.has(typeId);

  return (
    <SubmenuItem
      label="Set Restriction"
      disabled={!righteous || !contentAccess.EditSettings}
    >
      {accessRights.map(([key, label]) => (
        <DropdownItem
          key={key}
          value={key}
          data-access-right={key}
          onClick={setAccessRight}
        >
          <div className="check-spacer">{isCurrentRight(key) && <IoCheckmarkOutline />}</div>
          {polyglot.t(`ACCESS_RIGHT_${label}`)}
        </DropdownItem>
      ))}
    </SubmenuItem>
  );
};
