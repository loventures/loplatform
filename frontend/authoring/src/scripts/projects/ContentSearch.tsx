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
import { replace } from 'connected-react-router';
import gretchen from '../grfetchen/';
import qs from 'qs';
import React, { useEffect, useMemo, useState } from 'react';
import { GiAllSeeingEye } from 'react-icons/gi';
import { IoChevronBack, IoChevronForward, IoSearchOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Button, Input, InputGroup, Spinner } from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import { useRouterQueryParam } from '../hooks';
import { SearchWebHit } from '../modals/narrative/types';
import { parseTypeIds } from '../story/NarrativeSearch';
import { ClippyModal } from '../story/NarrativeSearch/ClippyModal';
import { AllTypes, InitialTypes, SearchFilter } from '../story/NarrativeSearch/SearchFilter';
import { SearchInstructions } from '../story/NarrativeSearch/SearchInstructions';
import { SearchRow } from '../story/NarrativeSearch/SearchRow';
import { plural } from '../story/story';
import { ContentSearchMenu } from './ContentSearch/ContentSearchMenu';

const maxResults = 10;

export const ContentSearch: React.FC = () => {
  const dispatch = useDispatch();
  const searchParam = useRouterQueryParam('search');
  const pageParam = useRouterQueryParam('page');
  const typesParam = useRouterQueryParam('types');
  const unusedParam = useRouterQueryParam('unused') === 'true';
  const [search, setSearch] = useState(searchParam ?? '');
  const [types, setTypes] = useState(() => parseTypeIds(typesParam));
  const [unused, setUnused] = useState(unusedParam);
  const [searching, setSearching] = useState(false);
  const [hits, setHits] = useState<Array<SearchWebHit>>();
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState(false);
  const [searched, setSearched] = useState('');
  const page = pageParam ? parseInt(pageParam) : 0;
  const offset = page * maxResults;
  const typesStr = useMemo(
    () =>
      types === InitialTypes ? undefined : types === AllTypes ? 'all' : Array.from(types).join(','),
    [types]
  );
  const [clippyOpen, setClippyOpen] = useState<boolean>();

  const onSearch = () => {
    if (clippyOpen == null && search.match(/\b(?:AND|OR)\b/)) {
      setClippyOpen(true);
      return;
    }
    const params = {
      search: search || undefined,
      types: typesStr,
      unused: unused || undefined,
      page: undefined,
    };
    dispatch(replace({ search: qs.stringify(params) }));
  };

  useEffect(() => {
    if (!searchParam && !typesParam) {
      setHits(undefined);
      setSearched('');
      return;
    }
    const webQuery = JSON.stringify({
      query: searchParam,
      typeIds: Array.from(parseTypeIds(typesParam)),
      unusedAssets: unusedParam,
      offset: offset,
      limit: maxResults,
    });
    setSearching(true);
    setError(false);
    trackAuthoringEvent(`Narrative Editor - Content Search`);
    gretchen
      .get(`/api/v2/authoring/search`)
      .params({
        query: webQuery,
      })
      .exec()
      .then(({ objects, totalCount }: { totalCount: number; objects: SearchWebHit[] }) => {
        setTotalCount(totalCount);
        setHits(objects);
        setSearched(searchParam);
      })
      .catch(e => {
        console.log(e);
        setError(true);
      })
      .finally(() => setSearching(false));
  }, [searchParam, typesParam, unusedParam, offset]);

  useEffect(() => {
    if (searchParam && !search) onSearch();
  }, [search, searchParam]);

  const isDisabled =
    searchParam === (search || undefined) && typesParam === typesStr && unused === unusedParam;

  return (
    <div className="narrative-editor narrative-mode py-0 py-sm-4">
      <div className="container narrative-container">
        <div className="story-element">
          <div className="project-search">
            <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
              <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
                <GiAllSeeingEye size="2rem" />
              </div>
              <h2 className="my-4 text-center">Content Search</h2>
              <ContentSearchMenu totalCount={totalCount} />
            </div>
            <div>
              <div
                className={classNames(
                  'content-list-filter d-flex justify-content-center search-margin',
                  !searchParam && 'no-search'
                )}
              >
                <InputGroup className="search-bar">
                  <Input
                    id="search-input"
                    type="search"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder="Search for content..."
                    size={48}
                    onKeyDown={e => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        onSearch();
                      }
                    }}
                    autoComplete="off"
                  />
                  <SearchFilter
                    types={types}
                    setTypes={setTypes}
                    unused={unused}
                    setUnused={setUnused}
                  />
                  <Button
                    id="search-button"
                    className="form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1 search-icon"
                    color="primary"
                    onClick={onSearch}
                    disabled={isDisabled}
                  >
                    <IoSearchOutline aria-hidden />
                  </Button>
                </InputGroup>
              </div>
              {error ? (
                <div className="text-center text-danger pb-5 search-error">
                  A search error occurred.
                </div>
              ) : searching ? (
                <div className="d-flex justify-content-center pb-5">
                  <Spinner className="text-muted" />
                </div>
              ) : hits && !hits.length ? (
                <div className="text-center text-muted pb-5 no-results">
                  No content matched your search.
                </div>
              ) : hits ? (
                <div
                  className="mb-5 full-search content-list full-index"
                  data-query={searched}
                >
                  {hits.map(hit => (
                    <SearchRow
                      key={hit.path.map(i => i.name).join('/')}
                      hit={hit}
                      global
                    />
                  ))}
                  <div className="mb-3 mt-5 text-muted d-flex align-items-center justify-content-center search-pager">
                    <Link
                      id="search-prev"
                      className={classNames(
                        'me-2 p-2 d-flex btn btn-transparent br-50 border-0',
                        !offset && 'disabled'
                      )}
                      to={{
                        search: qs.stringify({
                          search: searchParam || undefined,
                          types: typesParam || undefined,
                          page: page > 1 ? page - 1 : undefined,
                        }),
                      }}
                      title="Previous Page"
                    >
                      <IoChevronBack />
                    </Link>
                    <span id="search-counts">
                      Showing {offset + 1}–{offset + hits.length} of {plural(totalCount, 'result')}.
                    </span>
                    <Link
                      id="search-next"
                      className={classNames(
                        'ms-2 p-2 d-flex  btn btn-transparent br-50 border-0',
                        totalCount <= offset + maxResults && 'disabled'
                      )}
                      to={{
                        search: qs.stringify({
                          search: searchParam || undefined,
                          types: typesParam || undefined,
                          page: 1 + page,
                        }),
                      }}
                      title="Next Page"
                    >
                      <IoChevronForward />
                    </Link>
                  </div>
                </div>
              ) : (
                <SearchInstructions />
              )}
            </div>
          </div>
        </div>
      </div>
      <ClippyModal
        isOpen={!!clippyOpen}
        toggle={() => setClippyOpen(false)}
      />
    </div>
  );
};
