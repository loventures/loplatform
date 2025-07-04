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

import { replace } from 'connected-react-router';
import qs from 'qs';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { IoSearchOutline } from 'react-icons/io5';
import { VscListTree } from 'react-icons/vsc';
import { useDispatch } from 'react-redux';
import VisibilitySensor from 'react-visibility-sensor';
import { Input, InputGroup, InputGroupText, Spinner } from 'reactstrap';

import edgeRuleConstants from '../editor/EdgeRuleConstants';
import { useFeedbackCounts } from '../feedback/feedbackHooks';
import { useCurrentContextPath, useEditedAsset, useFilteredContentList } from '../graphEdit';
import { useBranchId, useRouterQueryParam } from '../hooks';
import { NodeName } from '../types/asset';
import { IndexRow } from './FullIndex/IndexRow';
import NarrativePresence from './NarrativeAsset/NarrativePresence';
import { toMultiWordRegex } from './questionUtil';
import { useIsStoryEditMode, useRevisionCommit } from './storyHooks';

// This could support a ToC on a non-course node but makes little sense.
export const FullIndex: React.FC<{ name: NodeName; contextPath: string | undefined }> = ({
  name,
}) => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const editMode = useIsStoryEditMode();
  const counts = useFeedbackCounts();
  const asset = useEditedAsset(name);
  const questions = !!edgeRuleConstants[asset.typeId]?.questions;
  const commit = useRevisionCommit();
  const parameters = commit ? { commit } : {};
  const searchParam = useRouterQueryParam('search');
  const contextPath = useCurrentContextPath();
  const [search, setSearch] = useState(searchParam ?? '');

  useEffect(() => {
    const params = { commit, search: search || undefined, contextPath: contextPath || undefined };
    dispatch(replace({ search: qs.stringify(params) }));
  }, [search, commit, contextPath]);

  const regex = useMemo(() => toMultiWordRegex(search), [search]);

  // For React performance reasons don't show everything up front
  const [max, setMax] = useState(100);
  useEffect(() => setMax(100), [regex]);

  const contentList = useFilteredContentList(asset, [], questions, search);

  const searchField = useRef<HTMLInputElement>();

  useEffect(() => {
    const listener = (e: KeyboardEvent) => {
      if (e.key === 'f' && (e.ctrlKey || e.metaKey) && searchField.current) {
        e.preventDefault();
        searchField.current.focus();
      }
    };
    window.addEventListener('keydown', listener);
    return () => {
      window.removeEventListener('keydown', listener);
    };
  }, [searchField]);

  const slimList = useMemo(() => contentList.slice(0, max), [contentList, max]);

  return (
    <>
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <div className="button-spacer d-flex align-items-center justify-content-center actions-icon">
          <VscListTree size="1.75rem" />
        </div>
        <h2 className="my-4 text-center">Table of Contents</h2>
        <NarrativePresence name="index">
          <div className="button-spacer d-flex align-items-center justify-content-center actions-icon"></div>
        </NarrativePresence>
      </div>
      <div className="mx-5 my-5">
        {!questions && (search || !!contentList.length) && (
          <div className="content-list-filter mb-5 d-flex flex-column align-items-center">
            <InputGroup
              id="toc-search"
              className="search-bar"
            >
              <Input
                innerRef={searchField}
                type="search"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Filter by title..."
                size={48}
              />
              <InputGroupText className="search-icon form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1">
                <IoSearchOutline aria-hidden />
              </InputGroupText>
            </InputGroup>
          </div>
        )}
        {!contentList.length && (!editMode || !!search) && (
          <div className="text-muted mt-5 depth-1 text-center">
            {search
              ? 'No content matches your search.'
              : questions
                ? 'No questions.'
                : 'No contents.'}
          </div>
        )}
        <div className="content-list full-index">
          {slimList.map(content => (
            <IndexRow
              key={content.edge?.name ?? '_root_'}
              content={content}
              branchId={branchId}
              regex={regex}
              questions={questions}
              counts={counts}
              parameters={parameters}
            />
          ))}
          {slimList.length < contentList.length && (
            <VisibilitySensor
              onChange={visible => {
                if (visible) setMax(max * 2);
              }}
            >
              <div className="d-flex justify-content-center pt-4">
                <Spinner color="muted" />
              </div>
            </VisibilitySensor>
          )}
        </div>
      </div>
    </>
  );
};
