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

import LoadingSpinner from '../../../directives/loadingSpinner';
import { isLoading } from '../../../types/loadable';
import { ensureNotNull } from '../../../utils/utils';
import React, { useContext } from 'react';

import LoListContext from './LoListContext';

const LoListHeader: React.FC<
  {
    title: string;
  } & React.PropsWithChildren
> = ({ title, children }) => {
  const listState = ensureNotNull(useContext(LoListContext));
  return (
    <div className="card-header">
      <div className="flex-row-content">
        <span className="circle-badge badge-primary">
          {isLoading(listState.listLoadable) && <LoadingSpinner />}
          {!isLoading(listState.listLoadable) && <span>{listState.filteredCount}</span>}
        </span>
        <span className="flex-col-fluid">{title}</span>
        {children}
      </div>
    </div>
  );
};

export default LoListHeader;
