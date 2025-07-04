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

import { searchByProps } from '../../../instructorPages/activityOverview/config';
import {
  LearnerListComponent,
  setFilters,
} from '../../../instructorPages/learnerList/learnerListActions';
import { InactiveFilter } from '../../../listComponents/parts/InactiveFilter';
import ListSearch from '../../../listComponents/parts/ListSearch';
import React from 'react';

type LearnerTableSearchProps = LearnerListComponent & {
  ariaControls: string;
};

const LearnerTableSearch: React.FC<LearnerTableSearchProps> = ({
  state,
  _dispatch,
  ariaControls,
}) => (
  <div className="card-list-filters d-flex">
    <ListSearch
      className="flex-col-fluid learner-table-search flex-grow-1"
      activeSearchString={state.filters.search}
      searchAction={(search: string) => _dispatch(setFilters({ search, page: 1 }))}
      searchByProps={searchByProps}
      ariaControls={ariaControls}
    />
    <InactiveFilter
      inactive={state.filters.inactive}
      toggle={() => _dispatch(setFilters({ inactive: !state.filters.inactive }))}
    />
  </div>
);

export default LearnerTableSearch;
