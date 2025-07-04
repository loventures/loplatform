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

import { useTranslation } from '../../i18n/translationContext.tsx';
import React from 'react';
import { IoCheckmark, IoFilterOutline } from 'react-icons/io5';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

export const InactiveFilter: React.FC<{ inactive: boolean; toggle: () => void }> = ({
  inactive,
  toggle,
}) => {
  const translate = useTranslation();
  return (
    <UncontrolledDropdown className="d-flex align-items-stretch">
      <DropdownToggle
        color="dark"
        outline
        className="d-flex align-items-center"
        style={{
          borderLeft: 0,
          borderColor: '#ced4da',
          borderTopLeftRadius: 0,
          borderBottomLeftRadius: 0,
          padding: '.625rem',
        }}
      >
        <IoFilterOutline />
      </DropdownToggle>
      <DropdownMenu end>
        <DropdownItem
          className="d-flex align-items-center gap-1"
          onClick={toggle}
          role="checkbox"
          aria-checked={inactive ? 'true' : 'false'}
        >
          <IoCheckmark className={inactive ? 'visible' : 'invisible'} />
          {translate('SHOW_INACTIVE_LEARNERS')}
        </DropdownItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
