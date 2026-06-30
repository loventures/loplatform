/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import React, { useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';

import { formatDayjs } from '../../filters/pure/formatDayjs.ts';
import { useTranslation } from '../../i18n/translationContext';
import { useCourseSelector } from '../../loRedux';
import * as searchActions from '../actions/DiscussionSearchActions.ts';
import { createSearchSelector } from '../selectors.js';

interface DiscussionBoardSearchProps {
  discussionId: string;
  setInView: (post: any, info: { viewType: string; flashType: string }) => void;
}

/**
 * React port of the `discussionBoardSearch` component (B2, discussion subsystem — leaf 2): the
 * discussion-board search box + results. Previously an Angular component bridged inside the Angular
 * `discussionBoard.html`; now native React, bridged back via react2angular (with the redux + i18n
 * providers, since it reads the search slice via `useCourseSelector` and dispatches the search action
 * via the Angular `DiscussionSearchActions`). The generic `<list-search-triggered>` search box (still
 * Angular, used by the threads/single-thread views) is replicated inline here as a small React form.
 * DOM preserved for `DiscussionPage`'s SearchArea page object: `.discussion-board-search`,
 * `input[type=text]`, `button .icon-search`, `.discussion-board-search-results li`.
 */
export const DiscussionBoardSearch: React.FC<DiscussionBoardSearchProps> = ({ discussionId, setInView }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const selector = useMemo(() => createSearchSelector(discussionId), [discussionId]);
  const { searchString: activeSearchString, loaded, searchResultTotal, searchResultPosts } = useCourseSelector<any>(
    selector
  ) || {};

  const searchActionCreator = useMemo(
    () => searchActions.makeSearchActionCreator(discussionId),
    [discussionId]
  );
  const runSearch = (str: string | null) => dispatch(searchActionCreator(str));

  const [searchString, setSearchString] = useState(activeSearchString || '');

  // Mirror the old `$onChanges`: sync the local box when the active (redux) search string is set.
  useEffect(() => {
    if (activeSearchString) setSearchString(activeSearchString);
  }, [activeSearchString]);

  const search = () => runSearch(searchString);
  const clearSearch = () => {
    setSearchString('');
    runSearch(null);
  };
  const viewPost = (post: any) => setInView(post, { viewType: 'search', flashType: 'search' });

  return (
    <div className="discussion-board-search my-3 d-print-none">
      <div className="input-group">
        <input
          className="form-control hang-next hang-icon-btn"
          type="text"
          role="search"
          aria-label={translate('DISCUSSION_BOARD_SEARCH_PLACEHOLDER')}
          placeholder={translate('DISCUSSION_BOARD_SEARCH_PLACEHOLDER')}
          value={searchString}
          onChange={e => setSearchString(e.target.value)}
          onKeyDown={e => {
            if (e.key === 'Enter') search();
          }}
        />

        <div className="input-hang-end">
          {!!searchString && (
            <button
              className="icon-btn icon-btn-danger"
              type="button"
              onClick={clearSearch}
            >
              <span className="sr-only">{translate('SRS_SEARCH_CLEAR')}</span>
              <span
                className="icon icon-cancel-circle"
                aria-hidden="true"
              />
            </button>
          )}
        </div>

        <button
          className="btn btn-primary d-flex align-items-center"
          type="button"
          disabled={!searchString}
          onClick={search}
        >
          <span className="sr-only">{translate('SRS_SEARCH_ACTION')}</span>
          <span
            className="icon icon-search"
            aria-hidden="true"
          />
        </button>
      </div>

      {loaded && (
        <div className="discussion-search-results m-2">
          <header className="discussion-board-search-results-header">
            <h6 className="m-0">
              <span>{translate('DISCUSSION_BOARD_SEARCH_RESULTS')}</span>{' '}
              <span>{translate('DISCUSSION_BOARD_SEARCH_COUNTS', { count: searchResultTotal })}</span>
            </h6>
          </header>

          <ul className="discussion-board-search-results list-group">
            {(searchResultPosts || []).map((item: any, index: number) => (
              // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions, jsx-a11y/click-events-have-key-events
              <li
                key={item.id}
                className="search-result"
                title={translate('SEARCH_VIEW_RESULTS')}
                onClick={() => viewPost(item)}
              >
                <span className="result-column index">{index + 1}.</span>
                <span className="result-column user-name">{item.user.fullName}</span>
                <span className="result-column create-time"> ({formatDayjs(item.createTime, 'lll')}) </span>
                <span className="result-column content-column">{item.contentPreview}</span>
                <span
                  className="result-column action"
                  role="presentation"
                >
                  <span>{translate('SEARCH_VIEW_RESULTS')}</span>
                  <span className="icon icon-chevron-right" />
                </span>
              </li>
            ))}
          </ul>

          {searchResultTotal > (searchResultPosts || []).length && (
            <span>{translate('DISCUSSION_BOARD_FOUND_TOO_MANY')}</span>
          )}
        </div>
      )}
    </div>
  );
};

