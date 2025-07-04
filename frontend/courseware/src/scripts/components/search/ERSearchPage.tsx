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

import classNames from 'classnames';
import { SearchBar } from '../../components/search/SearchBar';
import { SearchDisplay } from '../../components/search/SearchDisplay';
import { SearchPagination } from '../../components/search/SearchPagination';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';

import { useSearchStuff } from './search';
import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle.tsx';

const ERSearchPage: React.FC<{ search: string }> = ({ search }) => {
  const translate = useTranslation();
  const { divRef, error, value, setValue, onSearch, setOffset, results, searching } =
    useSearchStuff(search);

  return (
    <ERContentContainer
      title={translate('SEARCH_PAGE_TITLE')}
      className="search-page"
    >
      <div
        className="container p-0"
        ref={divRef}
      >
        <div
          className={classNames(
            'card er-content-wrapper mb-2 m-md-3 m-lg-4',
            !results && 'unsearched'
          )}
        >
          <div className="card-body">
            <ERNonContentTitle label={translate('SEARCH_PAGE_TITLE')} />

            <div className="my-2 my-md-3 my-lg-4 d-flex flex-column align-items-stretch">
              <SearchBar
                value={value}
                setValue={setValue}
                onSearch={onSearch}
                disabled={searching}
                autoFocus
              />
            </div>

            <SearchDisplay
              error={error}
              results={results}
            />

            <SearchPagination
              results={results}
              setOffset={setOffset}
            />
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERSearchPage;
