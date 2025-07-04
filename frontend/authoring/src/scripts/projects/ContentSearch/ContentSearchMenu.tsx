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
import { IoMenuOutline } from 'react-icons/io5';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { useRouterQueryParam } from '../../hooks';
import { DropdownAItem } from '../../story/components/DropdownAItem';
import { AllTypes, InitialTypes } from '../../story/NarrativeSearch/SearchFilter';

const MaxCsvRows = 5000;

export const ContentSearchMenu: React.FC<{ totalCount: number }> = ({ totalCount }) => {
  const searchParam = useRouterQueryParam('search');
  const typesParam = useRouterQueryParam('types');
  const webQuery = JSON.stringify({
    query: searchParam,
    typeIds:
      typesParam === 'all'
        ? Array.from(AllTypes)
        : !typesParam
          ? Array.from(InitialTypes)
          : typesParam.split(','),
  });
  const encodedQuery = encodeURIComponent(webQuery);

  const downloadUrl = `/api/v2/authoring/search/results.csv?query=${encodedQuery}`;

  // While one would expect to use <a download /> links, Chrome randomly fails
  // with "Network error" that never actually hits the server. Perceived wisdom
  // is to use target="_blank" instead.
  return (
    <UncontrolledDropdown
      id="search-menu"
      className="d-inline-block"
    >
      <DropdownToggle
        color="primary"
        outline
        caret
        className="border-0 asset-settings unhover-muted hover-white"
      >
        <IoMenuOutline size="1.75rem" />
      </DropdownToggle>
      <DropdownMenu end>
        <DropdownAItem
          id="results-download-button"
          target="_blank"
          href={downloadUrl}
          disabled={!searchParam && !typesParam}
        >
          Download Search Results
        </DropdownAItem>
        {totalCount > MaxCsvRows && (
          <DropdownItem
            disabled
            className="small text-muted pt-0 text-center"
          >
            Limit: {MaxCsvRows} rows
          </DropdownItem>
        )}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
