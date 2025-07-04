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

import { SearchLimit, SearchResults } from '../../components/search/search';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { IoArrowBackOutline, IoArrowForwardOutline } from 'react-icons/io5';
import { Button } from 'reactstrap';

export const SearchPagination: React.FC<{
  results: SearchResults | undefined;
  setOffset: (offset: number) => void;
}> = ({ results, setOffset }) => {
  const translate = useTranslation();

  return results && results.totalCount > SearchLimit ? (
    <div className="d-flex justify-content-between align-items-center px-md-3 py-md-3">
      <Button
        color="primary"
        outline
        className="border-0"
        disabled={!results.offset}
        onClick={() => setOffset(Math.max(0, results.offset - SearchLimit))}
        title={translate('SEARCH_PREVIOUS')}
        aria-label={translate('SEARCH_PREVIOUS')}
      >
        <IoArrowBackOutline
          size="1.5rem"
          aria-hidden
        />
      </Button>
      <span className="text-muted">
        {1 + Math.floor(results.offset / SearchLimit)}
        {' / '}
        {Math.floor((results.totalCount + SearchLimit - 1) / SearchLimit)}
      </span>
      <Button
        color="primary"
        outline
        className="border-0"
        disabled={results.offset + SearchLimit >= results.totalCount}
        onClick={() => setOffset(results.offset + SearchLimit)}
        title={translate('SEARCH_NEXT')}
        aria-label={translate('SEARCH_NEXT')}
      >
        <IoArrowForwardOutline
          size="1.5rem"
          aria-hidden
        />
      </Button>
    </div>
  ) : null;
};
