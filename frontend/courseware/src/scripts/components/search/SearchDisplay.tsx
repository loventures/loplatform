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

import { SearchResults, useSearchContents } from '../../components/search/search';
import { SearchInstructions } from '../../components/search/SearchInstructions';
import { SearchResult } from '../../components/search/SearchResult';
import { useCourseSelector } from '../../loRedux';
import { useTranslation } from '../../i18n/translationContext';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React from 'react';

export const SearchDisplay: React.FC<{
  error: boolean;
  results: SearchResults | undefined;
}> = ({ error, results }) => {
  const translate = useTranslation();
  const viewingAs = useCourseSelector(selectCurrentUser);
  const contents = useSearchContents();

  return (
    <>
      {!results && !error && <SearchInstructions />}

      <ul className="competency-list-nested px-md-4 pt-3">
        {error ? (
          <li className="text-muted text-center">{translate('SEARCH_ERROR')}</li>
        ) : !results ? (
          <li />
        ) : !results.totalCount ? (
          <li className="text-muted text-center">{translate('SEARCH_NO_RESULTS')}</li>
        ) : (
          results.hits.map(hit => (
            <SearchResult
              key={hit.edgePath}
              content={contents[hit.edgePath]}
              highlights={hit.highlights}
              viewingAs={viewingAs}
            />
          ))
        )}
      </ul>
    </>
  );
};
