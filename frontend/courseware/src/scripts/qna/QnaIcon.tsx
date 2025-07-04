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

const QnaIcon = (props: React.SVGAttributes<SVGElement>) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    fill="none"
    viewBox="0 0 24 24"
    {...props}
  >
    <path
      stroke="currentColor"
      strokeWidth="1.25"
      d="M21 3H3a1 1 0 0 0-1 1v12c0 .6.4 1 1 1h12.7c.2 0 .4 0 .6.2l4.1 3.1a1 1 0 0 0 1.6-.8V4c0-.6-.4-1-1-1Z"
    />
    <path
      stroke="currentColor"
      strokeWidth="1.25"
      d="M12 12v-.7c0-.7 0-1 .7-1.4.6-.4 1.3-.7 1.3-1.8a2 2 0 0 0-2-1.8c-1.3 0-2 .9-2 2.1m2 4.4v1.4"
    />
  </svg>
);

export default QnaIcon;
