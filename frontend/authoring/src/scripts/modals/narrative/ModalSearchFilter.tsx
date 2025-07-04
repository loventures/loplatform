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
import { sortBy } from 'lodash';
import React, { useMemo } from 'react';
import { BsFilter } from 'react-icons/bs';
import { IoCheckmarkOutline } from 'react-icons/io5';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { setToggle } from '../../gradebook/set';
import { usePolyglot } from '../../hooks';
import { storyTypeName } from '../../story/story';
import { TypeId } from '../../types/asset';

export const ModalSearchFilter: React.FC<{
  typeIds?: TypeId[];
  types: Set<TypeId>;
  setTypes: (types: Set<TypeId>) => void;
  unused: boolean;
  setUnused: (unused: boolean) => void;
}> = ({ typeIds, types, setTypes, unused, setUnused }) => {
  const polyglot = usePolyglot();
  const sortedTypes = useMemo(
    () => sortBy(typeIds, typeId => storyTypeName(polyglot, typeId)),
    [typeIds]
  );

  return (
    <UncontrolledDropdown
      id="content-type-menu"
      className="input-group-append"
    >
      <DropdownToggle
        color="primary"
        outline
        className={classNames(
          'form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 search-filter',
          (types.size !== typeIds.length || unused) && 'filtered'
        )}
        title="Filter Content Types"
      >
        <BsFilter size="1.5rem" />
      </DropdownToggle>
      <DropdownMenu
        right
        strategy="fixed"
        modifiers={[{ name: 'preventOverflow', options: { mainAxis: false } } as any]}
      >
        <DropdownItem
          onClick={() => {
            setTypes(new Set());
            setUnused(false);
          }}
          disabled={!types.size && !unused}
          toggle={false}
        >
          <div className="check-spacer" />
          Clear Filters
        </DropdownItem>
        <DropdownItem divider />
        {sortedTypes.map(typeId => (
          <DropdownItem
            key={typeId}
            onClick={() => setTypes(setToggle(types, typeId))}
            toggle={false}
            className={types.has(typeId) ? 'checked' : undefined}
          >
            <div className="check-spacer">{types.has(typeId) && <IoCheckmarkOutline />}</div>
            {storyTypeName(polyglot, typeId)}
          </DropdownItem>
        ))}
        <DropdownItem divider />
        <DropdownItem
          onClick={() => setUnused(!unused)}
          toggle={false}
          className={unused ? 'checked' : undefined}
        >
          <div className="check-spacer">{unused && <IoCheckmarkOutline />}</div>
          Include Deleted
        </DropdownItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
