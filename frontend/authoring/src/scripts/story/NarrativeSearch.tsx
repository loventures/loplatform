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
import { BsBodyText } from 'react-icons/bs';
import {
  IoChevronBack,
  IoChevronForward,
  IoCodeSlashOutline,
  IoSearchOutline,
} from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import {
  Button,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Input,
  InputGroup,
  Spinner,
  UncontrolledDropdown,
} from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import { useCurrentContextPath, useGraphEditSelector } from '../graphEdit';
import { useBranchId, useDcmSelector, useRouterQueryParam } from '../hooks';
import { SearchWebHit } from '../modals/narrative/types';
import { TypeId } from '../types/asset';
import { useProjectAccess } from './hooks';
import NarrativePresence from './NarrativeAsset/NarrativePresence';
import { ClippyModal } from './NarrativeSearch/ClippyModal';
import { AllTypes, InitialTypes, SearchFilter } from './NarrativeSearch/SearchFilter';
import { SearchInstructions } from './NarrativeSearch/SearchInstructions';
import { SearchMenu } from './NarrativeSearch/SearchMenu';
import { SearchRow } from './NarrativeSearch/SearchRow';
import { plural } from './story';
import { useRevisionCommit } from './storyHooks';

const maxResults = 10;

export const parseTypeIds = (typesParam: string | undefined): Set<TypeId> =>
  !typesParam
    ? InitialTypes
    : typesParam === 'all'
      ? AllTypes
      : new Set(typesParam.split(',') as TypeId[]);

export const NarrativeSearch: React.FC = () => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const commit = useRevisionCommit();
  const contextPath = useCurrentContextPath();
  const searchParam = useRouterQueryParam('search');
  const htmlParam = useRouterQueryParam('html') === 'true';
  const pageParam = useRouterQueryParam('page');
  const typesParam = useRouterQueryParam('types');
  const unusedParam = useRouterQueryParam('unused') === 'true';
  const [search, setSearch] = useState(searchParam ?? '');
  const [html, setHtml] = useState(htmlParam);
  const [types, setTypes] = useState<Set<TypeId>>(() => parseTypeIds(typesParam));
  const [unused, setUnused] = useState(unusedParam);
  const [searching, setSearching] = useState(false);
  const [hits, setHits] = useState<Array<SearchWebHit>>();
  const [totalCount, setTotalCount] = useState(0);
  const [error, setError] = useState(false);
  const [searched, setSearched] = useState('');
  const role = useDcmSelector(state => state.layout.role);
  const projectAccess = useProjectAccess();
  const accessRights = useGraphEditSelector(state => state.contentTree.accessRights);
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
      html: html || undefined,
      types: typesStr,
      unused: unused || undefined,
      page: undefined,
      contextPath,
    };
    dispatch(replace({ search: qs.stringify(params) }));
  };

  useEffect(() => {
    if (!searchParam && !typesParam) {
      setHits(undefined);
      setSearched('');
      return;
    }
    const html = htmlParam;
    const webQuery = JSON.stringify({
      query: searchParam,
      typeIds: html ? undefined : Array.from(parseTypeIds(typesParam)),
      unusedAssets: unusedParam,
      offset: offset,
      limit: maxResults,
    });
    setSearching(true);
    setError(false);
    trackAuthoringEvent(`Narrative Editor - ${html ? 'HTML' : 'Text'} Search`);
    gretchen
      .get(
        html
          ? `/api/v2/authoring/search/${branchId}/html`
          : `/api/v2/authoring/search/branch/${branchId}`
      )
      .params({
        query: webQuery,
      })
      .exec()
      .then(({ objects, totalCount }: { totalCount: number; objects: SearchWebHit[] }) => {
        setTotalCount(totalCount);
        setHits(objects.filter(hit => !role || accessRights[hit.path[0].name]?.ViewContent));
        setSearched(searchParam);
      })
      .catch(e => {
        console.log(e);
        setError(true);
      })
      .finally(() => setSearching(false));
  }, [searchParam, htmlParam, typesParam, unusedParam, offset]);

  useEffect(() => {
    if (searchParam && !search) onSearch();
  }, [search, searchParam]);

  const isDisabled =
    searchParam === (search || undefined) &&
    typesParam === typesStr &&
    html === htmlParam &&
    unused === unusedParam;

  return (
    <>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
          <IoSearchOutline size="1.75rem" />
        </div>
        <h2 className="my-4 text-center">Search</h2>
        <NarrativePresence name="search">
          <SearchMenu />
        </NarrativePresence>
      </div>
      <div>
        <div
          className={classNames(
            'content-list-filter d-flex justify-content-center search-margin',
            !searchParam && 'no-search'
          )}
        >
          <InputGroup className="search-bar">
            {projectAccess.ContentRepo && (
              <UncontrolledDropdown
                id="search-type-menu"
                className="d-flex"
              >
                <DropdownToggle
                  className="form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 ps-1"
                  color="secondary"
                  title={html ? 'HTML Search' : 'Text Search'}
                >
                  {html ? <IoCodeSlashOutline aria-hidden /> : <BsBodyText aria-hidden />}
                </DropdownToggle>
                <DropdownMenu>
                  <DropdownItem onClick={() => setHtml(false)}>Text Search</DropdownItem>
                  <DropdownItem onClick={() => setHtml(true)}>HTML Search</DropdownItem>
                </DropdownMenu>
              </UncontrolledDropdown>
            )}
            <Input
              id="search-input"
              type="search"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder={html ? 'Search for HTML...' : 'Search for content...'}
              size={48}
              autoFocus
              onKeyDown={e => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  onSearch();
                }
              }}
              disabled={!!commit}
            />
            <SearchFilter
              types={types}
              setTypes={setTypes}
              unused={unused}
              setUnused={setUnused}
            />
            <Button
              id="search-submit"
              className="form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1 search-icon"
              color="primary"
              disabled={isDisabled}
              onClick={onSearch}
            >
              <IoSearchOutline aria-hidden />
            </Button>
          </InputGroup>
        </div>
        {commit ? (
          <div className="text-center text-danger pb-5">
            Search of historic content is unsupported.
          </div>
        ) : error ? (
          <div className="text-center text-danger pb-5 search-error">A search error occurred.</div>
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
              />
            ))}
            <div className="mb-3 mt-5 text-muted d-flex align-items-center justify-content-center search-pager">
              <Link
                id="search-prev"
                className={classNames(
                  'me-3 d-flex  btn btn-transparent br-50 border-0',
                  !offset && 'disabled'
                )}
                to={{
                  search: qs.stringify({
                    search: search,
                    html: html || undefined,
                    types: typesStr,
                    page: page > 1 ? page - 1 : undefined,
                    contextPath,
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
                  'ms-3 d-flex  btn btn-transparent br-50 border-0',
                  totalCount <= offset + maxResults && 'disabled'
                )}
                to={{
                  search: qs.stringify({
                    search: search,
                    html: html || undefined,
                    types: typesStr,
                    page: 1 + page,
                    contextPath,
                  }),
                }}
                title="Next Page"
              >
                <IoChevronForward />
              </Link>
            </div>
          </div>
        ) : !html ? (
          <SearchInstructions />
        ) : null}
      </div>
      <ClippyModal
        isOpen={!!clippyOpen}
        toggle={() => setClippyOpen(false)}
      />
    </>
  );
};
