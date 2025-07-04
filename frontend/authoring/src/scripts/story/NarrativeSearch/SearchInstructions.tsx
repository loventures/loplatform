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

export const SearchInstructions: React.FC = () => (
  <div className="mx-md-5 px-md-4 py-md-4 mb-5 text-muted search-instructions">
    <div className="mx-3 mb-2">Search Instructions</div>
    <dl className="mx-3 small">
      <dt>apple orange</dt>
      <dd>Space to search for documents containing any term (this or that).</dd>
      <dt>&quot;orange apple&quot;</dt>
      <dd>Use quotes to search for exact phrases.</dd>
      <dt>&quot;extra terrestrial&quot; + life</dt>
      <dd>Use plus to search for documents containing all terms (this and that).</dd>
      <dt>flash + -photography</dt>
      <dd>
        Use plus and minus to search for documents containing some terms but not others (this but
        not that).
      </dd>
      <dt>flash + -(photography dance)</dt>
      <dd>Use parentheses to group terms (this but neither that nor the other).</dd>
    </dl>
  </div>
);
