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

import React from 'react';
import DocumentTytle from 'react-document-title';

import { contrast } from '../services';

const FakeCrumb = ({ title, color, docTitle }) => {
  const bg = color || '#566B10',
    fg = contrast(bg);
  return (
    <DocumentTytle title={docTitle || title}>
      <div
        className="breadcrumb"
        style={{ backgroundColor: bg, borderRadius: '0' }}
      >
        <nav className="breadcrumbs">
          <span className="breadcrumbs__section">
            <a
              className="breadcrumbs__crumb breadcrumbs__crumb--active"
              aria-current="true"
              style={{ color: fg }}
            >
              {title}
            </a>
          </span>
        </nav>
      </div>
    </DocumentTytle>
  );
};

export default FakeCrumb;
