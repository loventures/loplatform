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

import axios from 'axios';
import Course from '../../bootstrap/course';
import {
  ContentWithAncestors,
  useLearningPathResource,
} from '../../resources/LearningPathResource';
import qs from 'qs';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useHistory } from 'react-router';

export type SearchHighlights = Record<string, string[]>;

export type SearchWebHit = {
  edgePath: string;
  highlights: SearchHighlights;
};

export type SearchResults = {
  query: string;
  offset: number;
  totalCount: number;
  hits: SearchWebHit[];
};

const webQuery = (query: string, offset: number, limit: number) =>
  JSON.stringify({
    query: query || null,
    offset,
    limit,
  });

export const doSearch = (query: string, offset: number, limit: number): Promise<SearchResults> => {
  const encodedQuery = encodeURIComponent(webQuery(query, offset, limit));
  return axios
    .get(`/api/v2/lwc/${Course.id}/search?query=${encodedQuery}`)
    .then(({ data: { objects: hits, totalCount } }) => {
      return {
        query,
        offset,
        totalCount,
        hits,
      };
    });
};

export const searchDownloadUrl = (query?: string) =>
  `/api/v2/lwc/${Course.id}/search/results.csv?query=${encodeURIComponent(
    JSON.stringify({ query: query ?? null })
  )}`;

export type EdgePath = string;

export const useSearchContents = (): Record<EdgePath, ContentWithAncestors> => {
  const { modules } = useLearningPathResource();
  return useMemo(() => {
    const map: Record<EdgePath, ContentWithAncestors> = {};
    for (const module of modules) {
      map[module.content.id] = module.content;
      for (const element of module.elements) {
        map[element.id] = element;
        if (element.lesson) map[element.lesson.id] = element.lesson;
      }
    }
    return map;
  }, [modules]);
};

export const SearchLimit = 10;

export const useSearchStuff = (search: string) => {
  const history = useHistory();
  const params = qs.parse(search.substring(1));
  const query = params.q as string | undefined;
  const page = params.p as string | undefined;
  const offset = page == null ? 0 : SearchLimit * parseInt(page, 10);
  const [value, setValue] = useState(query ?? '');
  const [results, setResults] = useState<SearchResults | undefined>(undefined);
  const [error, setError] = useState(false);
  const [searching, setSearching] = useState(false);
  const divRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    // By depending on the search results, if the user makes a new search while
    // we were fetching an old query, we'll delay the new search until the old
    // results come back.
    if (searching || (query === results?.query && offset === results?.offset)) {
      return;
    } else if (!query) {
      setResults(undefined);
      return;
    }
    setError(false);
    setSearching(true);
    doSearch(query, offset, SearchLimit)
      .then(results => {
        setResults(results);
        setError(false);
      })
      .catch(e => {
        console.log(e);
        history.push({ search: '' });
        setError(true);
      })
      .finally(() => setSearching(false));
  }, [query, offset, searching, results]);

  useEffect(() => {
    if (!value.trim()) history.push({ search: '' });
  }, [value]);

  const onSearch = () => {
    history.push({ search: !value.trim() ? '' : '?q=' + encodeURIComponent(value.trim()) });
  };

  const setOffset = (offset: number) => {
    history.push({
      search: !query
        ? ''
        : '?q=' + encodeURIComponent(query) + '&p=' + Math.floor(offset / SearchLimit),
    });
    divRef.current?.scrollIntoView(true);
  };

  const downloadUrl = useMemo(() => searchDownloadUrl(query), [query]);

  return {
    divRef,
    error,
    value,
    setValue,
    onSearch,
    setOffset,
    results,
    searching,
    downloadUrl,
  };
};
