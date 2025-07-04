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

import { searchByProps, sortByProps } from '../config';
import { selectProgressReportListStateComponent } from '../selectors';
import { searchActionCreator, sortActionCreator } from '../actions/listActions';

import ListSearch from '../../../listComponents/parts/ListSearch';
import ListSort from '../../../listComponents/parts/ListSort';
import ProgressReportCompactToggle from './ProgressReportCompactToggle';

const ProgressReportOptions = ({ listState, searchAction, sortAction }) => (
  <div className="progress-filters">
    <div className="flex-row-content flex-wrap">
      <ListSort
        activeSortKey={listState.activeOptions.sortKey}
        sortAction={sortAction}
        sortByProps={sortByProps}
      ></ListSort>

      <ProgressReportCompactToggle />

      <ListSearch
        className="flex-col-fluid"
        activeSearchString={listState.activeOptions.searchString}
        searchAction={searchAction}
        searchByProps={searchByProps}
      ></ListSearch>
    </div>
  </div>
);

export default connect(selectProgressReportListStateComponent, {
  searchAction: searchActionCreator,
  sortAction: sortActionCreator,
})(ProgressReportOptions);
