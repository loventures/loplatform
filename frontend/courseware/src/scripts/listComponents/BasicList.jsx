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

import { isEmpty } from 'lodash';
import ListPaginate from './parts/ListPaginate.js';
import ListHeader from './parts/ListHeader.jsx';
import ListSearch from './parts/ListSearch.jsx';
import ListSort from './parts/ListSort.jsx';
import ListMessages from './parts/ListMessages.jsx';

const BasicList = ({
  children,
  listState,

  title,
  icon,
  filteredMessage,
  emptyMessage,
  searchByProps,
  sortByProps,

  Header = ListHeader,
  HeaderButton,

  searchAction,
  sortAction,
  paginateAction,

  hasSort = !isEmpty(sortByProps),
  hasSearch = !isEmpty(searchByProps),
}) => (
  <div className="card-list">
    <Header
      title={title}
      icon={icon}
      listState={listState}
      HeaderButton={HeaderButton}
    />

    {(hasSort || hasSearch) && (
      <div className="card-list-filters">
        <div className="flex-row-content">
          {hasSearch && (
            <ListSearch
              className="flex-col-fluid"
              activeSearchString={listState.activeOptions.searchString}
              searchAction={searchAction}
              searchByProps={searchByProps}
            ></ListSearch>
          )}
          {hasSort && (
            <ListSort
              activeSortKey={listState.activeOptions.sortKey}
              sortAction={sortAction}
              sortByProps={sortByProps}
            ></ListSort>
          )}
        </div>
      </div>
    )}

    {listState.status.loaded && listState.data.count > 0 && <div>{children}</div>}

    <ListMessages
      listState={listState}
      emptyMessage={emptyMessage}
      filteredMessage={filteredMessage}
    />

    {listState.status.loaded && listState.activeOptions.totalPages > 1 && (
      <div className="card-footer">
        <ListPaginate
          page={listState.activeOptions.currentPage}
          numPages={listState.activeOptions.totalPages}
          pageAction={paginateAction}
        ></ListPaginate>
      </div>
    )}
  </div>
);

export default BasicList;
