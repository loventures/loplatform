/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { useTranslation } from '../../i18n/translationContext';
import { SrsStoreController } from './useSrsStore';
import React, { useState } from 'react';
import { Dropdown, DropdownItem, DropdownMenu, DropdownToggle } from 'reactstrap';

/**
 * React port of the `srsSort` directive. DOM preserved: `.dropdown-wrap` wrapping
 * a `.dropdown` (reactstrap) with a `.btn.btn-primary` toggle and `.dropdown-menu`
 * of `.dropdown-item` buttons — the selectors the Angular srs screens use
 * (`.card-list-filters .dropdown > button` / `.dropdown-menu > button`). Each item
 * drives `store.sort(true, false, config)`.
 */

interface SrsSortProps {
  controller: SrsStoreController;
  props?: Record<string, any>;
}

export const SrsSort: React.FC<SrsSortProps> = ({ controller, props }) => {
  const { store, run } = controller;
  const translate = useTranslation();
  const [open, setOpen] = useState(false);
  const sortProps = props ?? store.sortByProps ?? {};

  const sortBy = (config: any) => run(store.sort!(true, false, config));

  return (
    <div className="dropdown-wrap">
      <Dropdown
        isOpen={open}
        toggle={() => setOpen(o => !o)}
      >
        <DropdownToggle className="btn btn-primary">
          <span>{translate('Sort By')}</span>
          <span className="lo-icon icon-chevron-down" />
        </DropdownToggle>
        <DropdownMenu>
          {Object.keys(sortProps).map(key => (
            <DropdownItem
              key={key}
              onClick={() => sortBy(sortProps[key])}
            >
              {translate('SORT_' + key)}
            </DropdownItem>
          ))}
        </DropdownMenu>
      </Dropdown>
    </div>
  );
};
