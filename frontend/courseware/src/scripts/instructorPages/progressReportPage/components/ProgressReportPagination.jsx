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

import { connect } from 'react-redux';
import { withTranslation } from '../../../i18n/translationContext';
import { map } from 'lodash';

import { selectProgressReportListStateComponent } from '../selectors';

import { paginateActionCreator, setPageSizeActionCreator } from '../actions/listActions';

import ListPaginate from '../../../listComponents/parts/ListPaginate';
import { DEFAULT_PAGE_SIZE_OPTIONS } from '../../../components/PaginateWithMax';

const ProgressReportPagination = ({ translate, listState, setPageSize, paginateAction }) => (
  <div className="flex-row-content flex-wrap flex-col-fluid justify-content-end">
    <div className="lo-select-wrap">
      <select
        className="form-control"
        value={listState.activeOptions.pageSize}
        onChange={event => setPageSize(event.target.value)}
      >
        {map(DEFAULT_PAGE_SIZE_OPTIONS, size => (
          <option
            value={size}
            key={size}
          >
            {translate('PAGE_SIZE_SELECT', { size })}
          </option>
        ))}
      </select>
    </div>
    {listState.activeOptions.totalPages > 1 && (
      <ListPaginate
        page={listState.activeOptions.currentPage}
        numPages={listState.activeOptions.totalPages}
        pageAction={paginateAction}
      ></ListPaginate>
    )}
  </div>
);

export default connect(selectProgressReportListStateComponent, {
  setPageSize: setPageSizeActionCreator,
  paginateAction: paginateActionCreator,
})(withTranslation(ProgressReportPagination));
