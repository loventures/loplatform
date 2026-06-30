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

import { SrsStoreController } from './useSrsStore';
import React from 'react';

const DEFAULT_PAGE_SIZE = 10;

/**
 * React port of the `srsPaginate` directive (was uib-pagination). DOM:
 * `.list-pagination > ul.pagination` with prev/numbered/next `.page-item`s.
 * Block-windowed (`rotate=false`) like the original; drives `store.gotoPage`.
 */
export const SrsPaginate: React.FC<{ controller: SrsStoreController }> = ({ controller }) => {
  const { store, run } = controller;
  const pageSize = store.pageSize || DEFAULT_PAGE_SIZE;
  const maxSize = store.maxSize || 5;
  const current = store.currentPage || 1;
  const totalPages = Math.max(1, Math.ceil((store.filterCount || 0) / pageSize));

  const blockStart = Math.floor((current - 1) / maxSize) * maxSize + 1;
  const blockEnd = Math.min(blockStart + maxSize - 1, totalPages);
  const pages: number[] = [];
  for (let p = blockStart; p <= blockEnd; p++) pages.push(p);

  const goto = (page: number) => {
    if (page >= 1 && page <= totalPages && page !== current) run(store.gotoPage(page));
  };

  const item = (key: React.Key, label: React.ReactNode, page: number, disabled: boolean, active = false) => (
    <li
      key={key}
      className={`page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`}
    >
      <button
        className="page-link"
        disabled={disabled}
        onClick={() => goto(page)}
      >
        {label}
      </button>
    </li>
  );

  return (
    <div className="list-pagination">
      <ul className="pagination">
        {item('prev', '«', current - 1, current <= 1)}
        {pages.map(p => item(p, p, p, false, p === current))}
        {item('next', '»', current + 1, current >= totalPages)}
      </ul>
    </div>
  );
};
