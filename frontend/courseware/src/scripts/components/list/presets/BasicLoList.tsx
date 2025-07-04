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

import React from 'react';

import { ListState, SearchConfig, SortConfig } from '../listTypes';
import LoListContainer from '../parts/LoListContainer';
import LoListControlsContainer from '../parts/LoListControlsContainer';
import LoListHeader from '../parts/LoListHeader';
import LoListLoader from '../parts/LoListLoader';
import LoListPaginate from '../parts/LoListPaginate';
import LoListSearch from '../parts/LoListSearch';
import LoListSort from '../parts/LoListSort';

const BasicLoList: React.FC<
  {
    listId: string;
    listState: ListState<any>;
    title: string;
    searchConfig?: SearchConfig;
    sortConfigs?: SortConfig[];
    emptyMessage?: string;
    filteredMessage?: string;
    renderHeaderButton?: () => JSX.Element;
  } & React.ConsumerProps<any>
> = ({
  listId,
  listState,
  title,
  searchConfig,
  sortConfigs,
  emptyMessage,
  filteredMessage,
  renderHeaderButton,
  children,
}) => {
  return (
    <LoListContainer listState={listState}>
      <LoListHeader title={title}>{renderHeaderButton && renderHeaderButton()}</LoListHeader>

      {(searchConfig || sortConfigs) && (
        <LoListControlsContainer>
          {searchConfig && (
            <LoListSearch
              ariaControls={listId}
              searchConfig={searchConfig}
            />
          )}
          {sortConfigs && <LoListSort sortConfigs={sortConfigs} />}
        </LoListControlsContainer>
      )}
      <LoListLoader
        id={listId}
        emptyMessage={emptyMessage}
        filteredMessage={filteredMessage}
        children={children}
      />
      <LoListPaginate />
    </LoListContainer>
  );
};

export default BasicLoList;
