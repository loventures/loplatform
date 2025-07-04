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
import React, { HTMLAttributes, useContext } from 'react';

import PaginateWithMax from '../../PaginateWithMax';
import LoListContext from './LoListContext';

const LoListPaginate: React.FC<HTMLAttributes<any>> = ({
  className = 'card-footer',
  ...attributes
}) => {
  const listState = ensureNotNull(useContext(LoListContext));
  return listState.filteredCount === 0 ? null : (
    <div
      className={className}
      {...attributes}
    >
      <PaginateWithMax
        pageIndex={listState.pageIndex}
        numPages={Math.ceil(listState.filteredCount / listState.pageSize)}
        pageAction={listState.setPageIndex}
      />
    </div>
  );
};

export default LoListPaginate;
