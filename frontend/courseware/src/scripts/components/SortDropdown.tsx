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

import { map } from 'lodash';
import { TranslationContext } from '../i18n/translationContext';
import React, { useContext } from 'react';
import {
  UncontrolledDropdown as Dropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
} from 'reactstrap';

const SortDropdown: React.FC<
  {
    sortToggleLabel?: string;
    activeSortKey: string | null;
    sortKeys: string[];
    sortAction: (s: string) => void;
  } & React.PropsWithChildren
> = ({ sortToggleLabel, activeSortKey, sortKeys, sortAction, children, ...otherProps }) => {
  const translate = useContext(TranslationContext);
  return (
    <Dropdown {...otherProps}>
      <DropdownToggle
        color="primary"
        caret={true}
      >
        <span>{sortToggleLabel ? sortToggleLabel : translate('Sort By')}</span>
      </DropdownToggle>

      <DropdownMenu flip={false}>
        {React.Children.count(children) > 0
          ? children
          : map(sortKeys, key => (
              <DropdownItem
                key={key}
                active={key === activeSortKey}
                onClick={() => sortAction(key)}
              >
                {translate(key)}
              </DropdownItem>
            ))}
      </DropdownMenu>
    </Dropdown>
  );
};

export default SortDropdown;
