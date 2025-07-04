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

import { ensureNotNull } from '../../../utils/utils';
import { find, map } from 'lodash';
import React, { HTMLAttributes, useContext } from 'react';

import SortDropdown from '../../SortDropdown';
import { SortConfig } from '../listTypes';
import LoListContext from './LoListContext';

const LoListSort: React.FC<
  {
    sortConfigs?: SortConfig[];
  } & HTMLAttributes<unknown>
> = ({ sortConfigs, children, ...otherProps }) => {
  const listState = ensureNotNull(useContext(LoListContext));
  return (
    <SortDropdown
      activeSortKey={listState.activeSort && listState.activeSort.i18nKey}
      sortKeys={map(sortConfigs, c => c.i18nKey)}
      sortAction={key => {
        return listState.setActiveSort(find(sortConfigs, c => c.i18nKey === key)!);
      }}
      children={children}
      {...otherProps}
    />
  );
};

export default LoListSort;
