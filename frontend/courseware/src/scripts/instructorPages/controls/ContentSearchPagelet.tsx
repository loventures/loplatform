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

import { useSearchStuff } from '../../components/search/search';
import { SearchBar } from '../../components/search/SearchBar';
import { SearchDisplay } from '../../components/search/SearchDisplay';
import { SearchPagination } from '../../components/search/SearchPagination';
import React from 'react';
import { FiDownload } from 'react-icons/fi';

export const ContentSearchPagelet: React.FC<{ search: string }> = ({ search }) => {
  const { divRef, error, value, setValue, onSearch, setOffset, results, searching, downloadUrl } =
    useSearchStuff(search);

  return (
    <div className="search-pagelet mt-3">
      <p>
        Content Search is intended to help you identify and locate outdated terms throughout your
        course. This tool searches all the course content for the terms or phrases you specify and
        allows you to generate a report of all the instances.
      </p>

      <div
        className="py-4 d-flex flex-column align-items-stretch"
        ref={divRef}
      >
        <SearchBar
          value={value}
          setValue={setValue}
          onSearch={onSearch}
          disabled={searching}
          autoFocus
        />
      </div>

      {!!results?.totalCount && (
        <div className="pt-1 mb-3 text-center">
          <a
            href={downloadUrl}
            target="_blank"
            className="d-inline-flex align-items-center search-download"
          >
            Download Search Results CSV <FiDownload className="ms-2" />
          </a>
        </div>
      )}

      <SearchDisplay
        error={error}
        results={results}
      />

      <SearchPagination
        results={results}
        setOffset={setOffset}
      />
    </div>
  );
};
