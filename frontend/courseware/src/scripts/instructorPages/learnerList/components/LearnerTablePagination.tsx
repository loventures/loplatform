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

import { DEFAULT_PAGE_SIZE_OPTIONS } from '../../../components/PaginateWithMax';
import {
  LearnerListComponent,
  setFilters,
} from '../../../instructorPages/learnerList/learnerListActions';
import { map } from 'lodash';
import { useTranslation } from '../../../i18n/translationContext';
import ListPaginate from '../../../listComponents/parts/ListPaginate';
import React from 'react';

type LearnerTablePaginationProps = LearnerListComponent;

const LearnerTablePagination: React.FC<LearnerTablePaginationProps> = ({ state, _dispatch }) => {
  const translate = useTranslation();
  return (
    <div className="flex-row-content flex-wrap flex-col-fluid justify-content-end">
      <div className="lo-select-wrap">
        <select
          className="form-control"
          value={state.filters.perPage}
          onChange={event =>
            _dispatch(setFilters({ page: 1, perPage: parseInt(event.target.value, 10) }))
          }
        >
          {map(DEFAULT_PAGE_SIZE_OPTIONS, size => (
            <option
              key={size}
              value={size}
            >
              {translate('PAGE_SIZE_SELECT', { size })}
            </option>
          ))}
        </select>
      </div>
      {state.studentCount && state.studentCount > state.filters.perPage && (
        <ListPaginate
          page={state.filters.page}
          numPages={Math.ceil(state.studentCount / state.filters.perPage)}
          pageAction={(page: number) => _dispatch(setFilters({ page }))}
        />
      )}
    </div>
  );
};

export default LearnerTablePagination;
