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

import LoadingSpinner from '../../directives/loadingSpinner/index';
import { useTranslation } from '../../i18n/translationContext';
import { SrsPaginate } from './SrsPaginate';
import { SrsSearch } from './SrsSearch';
import { SrsSort } from './SrsSort';
import { SrsStore, useSrsStore } from './useSrsStore';
import { isEmpty } from 'lodash';
import React from 'react';

/**
 * React port of the `srsList` directive. The Angular version was transclusion-
 * based (consumers supplied the `<li>` template); React uses a `renderItem`
 * render-prop instead. DOM preserved from srsList.html: `.card-list`, optional
 * `.card-header`, `.card-list-filters` (search/sort), `ul.card-list-striped-body`
 * (the Selenide row hook), the filtered/empty/error alerts, and a `.card-footer`
 * paginate. Drives the shared Angular `ResourceStore`/`LocalResourceStore` through
 * `useSrsStore` (see the reactivity note there).
 *
 * NOTE: this is a parallel React implementation for the migrated picker modals;
 * the Angular `srs-list` directive stays for the assignment/grader screens until
 * those migrate.
 */

interface HeaderButton {
  label: string;
  onClick: () => void;
}

interface SrsListProps {
  store: SrsStore;
  renderItem: (item: any, index: number) => React.ReactNode;
  /** React key per item (default `item.id ?? index`); e.g. users keyed by handle. */
  getItemKey?: (item: any, index: number) => React.Key;
  /** extra class(es) on the `.card-list` (the Angular host's class, e.g. `list-group`). */
  className?: string;
  headerText?: string;
  iconCls?: string;
  emptyMsg?: string;
  filteredMsg?: string;
  emptyIsGood?: boolean;
  headerButton?: HeaderButton;
  /** drive an initial load on mount (default true) */
  autoload?: boolean;
}

export const SrsList: React.FC<SrsListProps> = ({
  store,
  renderItem,
  getItemKey,
  className,
  headerText,
  iconCls,
  emptyMsg,
  filteredMsg,
  emptyIsGood,
  headerButton,
  autoload = true,
}) => {
  const translate = useTranslation();
  const controller = useSrsStore(store, { autoload });

  const title = headerText || store.title;
  const icon = iconCls || store.iconCls;
  const hasSort = !isEmpty(store.sortByProps);
  const hasSearch = !isEmpty(store.searchByProps);
  const showHeader = !!(title || icon || headerButton);
  const modalClose = headerButton?.label === 'MODAL_CLOSE';

  return (
    <div className={className ? `card-list ${className}` : 'card-list'}>
      {showHeader && (
        <header>
          <div className="card-header">
            <div className="flex-row-content">
              <span className="circle-badge badge-primary ms-1">
                {!!icon && !store.loading && <span className={`icon ${icon}`} />}
                {!icon && !store.loading && <span>{store.filterCount}</span>}
              </span>
              <span className="flex-col-fluid">{title ? translate(title) : null}</span>
              {!!headerButton && (
                <button
                  className={modalClose ? 'btn btn-close' : 'btn btn-sm btn-primary'}
                  onClick={() => headerButton.onClick()}
                  aria-label={modalClose ? translate(headerButton.label) : undefined}
                >
                  {modalClose ? null : translate(headerButton.label)}
                </button>
              )}
            </div>
          </div>
        </header>
      )}

      {store.totalCount !== 0 && (
        <div>
          {(hasSearch || hasSort) && (
            <div className="card-list-filters">
              <div className="flex-row-content">
                {hasSearch && (
                  <SrsSearch
                    controller={controller}
                    // .flex-col-fluid wrapper matches the Angular layout
                  />
                )}
                {hasSort && <SrsSort controller={controller} />}
              </div>
            </div>
          )}

          {store.loading && (
            <div className="card-body">
              <LoadingSpinner />
            </div>
          )}

          {!store.loading && (
            <ul className="card-list-striped-body">
              {store.data.map((item, index) => (
                <React.Fragment key={getItemKey ? getItemKey(item, index) : (item.id ?? index)}>
                  {renderItem(item, index)}
                </React.Fragment>
              ))}
            </ul>
          )}

          {!store.loading && store.totalCount !== 0 && store.filterCount === 0 && (
            <div className="card-body">
              <div className={`alert mb-0 ${emptyIsGood ? 'alert-success' : 'alert-warning'}`}>
                {translate(filteredMsg || 'SRS_STORE_FILTERED')}
              </div>
            </div>
          )}

          {(store.filterCount || 0) > (store.pageSize || 0) && (
            <div className="card-footer">
              <SrsPaginate controller={controller} />
            </div>
          )}
        </div>
      )}

      {!store.loading && store.totalCount === 0 && (
        <div className="card-body">
          <div className={`alert mb-0 ${emptyIsGood ? 'alert-success' : 'alert-danger'}`}>
            {translate(emptyMsg || 'SRS_STORE_EMPTY')}
          </div>
        </div>
      )}

      {!store.loading && !!store.loadErrorMessage && (
        <div className="card-body">
          <div className="alert alert-danger mb-0">{store.loadErrorMessage}</div>
        </div>
      )}
    </div>
  );
};
